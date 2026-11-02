package dev.customgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest bei geöffnetem Marktstand die wartenden Kunden aus und merkt sich pro Produkt, wie viel noch
 * fehlt, wie viel schon abgegeben wurde und wie viel insgesamt verlangt wird. Gelesen werden die
 * Lore-Zeilen "Produkt: ..." und "Beliefert: x/y". Außerdem wird versucht, Besitzer und Inselname
 * des Marktstands zu finden (aus dem Titel oder aus Zeilen wie "Besitzer: ...", "Insel: ...").
 */
public final class MarktstandTracker {

    public record Entry(String product, long missing, long delivered, long total, ItemStack icon) {
    }

    private static final Pattern PROGRESS = Pattern.compile("([\\d.]+)\\s*/\\s*([\\d.]+)");
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern OWNER_LINE = Pattern.compile(
            "^(?:Besitzer(?:in)?|Inhaber(?:in)?|Eigent\u00FCmer(?:in)?|Verk\u00E4ufer(?:in)?|Owner|Von)\\s*[:\u00BB\\-]\\s*(.+)$", FLAGS);
    private static final Pattern ISLAND_LINE = Pattern.compile(
            "^(?:Insel(?:name)?|Island)\\s*[:\u00BB\\-]\\s*(.+)$", FLAGS);
    private static final Pattern STRIP_EDGES = Pattern.compile("^[^\\p{L}\\p{N}_]+|[^\\p{L}\\p{N}_]+$");

    private static List<Entry> entries = List.of();
    private static String owner = "";
    private static String island = "";
    /** Gefundene Icons bleiben erhalten, auch wenn das Item später nicht mehr im Inventar liegt. */
    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private MarktstandTracker() {
    }

    public static List<Entry> entries() {
        return entries;
    }

    /** Besitzer des zuletzt geöffneten Marktstands oder "" wenn unbekannt. */
    public static String owner() {
        return owner;
    }

    /** Inselname des zuletzt geöffneten Marktstands oder "" wenn unbekannt. */
    public static String island() {
        return island;
    }

    public static void clear() {
        entries = List.of();
        owner = "";
        island = "";
    }

    /** Wird jeden Client-Tick aufgerufen, tut aber nur etwas, wenn der Marktstand offen ist. */
    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.toLowerCase(Locale.ROOT).contains("marktstand")) return;

        Map<String, long[]> sum = new LinkedHashMap<>(); // Produkt -> {fehlt, abgegeben, gesamt}
        boolean sawCustomer = false;
        String foundOwner = "";
        String foundIsland = "";
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            List<String> lines = new ArrayList<>();
            lines.add(stack.getHoverName().getString().trim());
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) lines.add(line.getString().trim());
            }

            String product = null;
            long delivered = -1;
            long total = -1;
            for (String s : lines) {
                int p = s.indexOf("Produkt:");
                if (p >= 0) {
                    product = s.substring(p + "Produkt:".length()).trim();
                }
                if (s.contains("Beliefert:")) {
                    Matcher m = PROGRESS.matcher(s);
                    if (m.find()) {
                        delivered = num(m.group(1));
                        total = num(m.group(2));
                    }
                }
                if (foundOwner.isEmpty()) {
                    Matcher m = OWNER_LINE.matcher(s);
                    if (m.matches()) foundOwner = clean(m.group(1));
                }
                if (foundIsland.isEmpty()) {
                    Matcher m = ISLAND_LINE.matcher(s);
                    if (m.matches()) foundIsland = clean(m.group(1));
                }
            }
            if (product != null && !product.isEmpty() && total >= 0) {
                sawCustomer = true;
            }
            if (product != null && !product.isEmpty() && delivered >= 0 && total > delivered) {
                long[] v = sum.computeIfAbsent(product, k -> new long[3]);
                v[0] += total - delivered;
                v[1] += delivered;
                v[2] += total;
            }
        }

        // Menü lädt noch (Kunden-Slots leer): alte Liste behalten statt kurz leeren
        if (!sawCustomer) return;

        if (foundOwner.isEmpty()) foundOwner = ownerFromTitle(title);
        owner = foundOwner;
        island = foundIsland;

        List<Entry> list = new ArrayList<>();
        sum.forEach((k, v) -> {
            ItemStack icon = ICONS.get(k);
            if (icon == null) {
                icon = ItemIcons.find(mc, k);
                if (icon != null) ICONS.put(k, icon);
            }
            list.add(new Entry(k, v[0], v[1], v[2], icon));
        });
        entries = list;
    }

    /** "Marktstand von Steve", "Marktstand - Steve", "Steves Marktstand" ... -> Rest nach dem Wort Marktstand. */
    private static String ownerFromTitle(String title) {
        String t = title.replaceAll("(?i)marktstand", " ").trim();
        t = t.replaceFirst("(?i)^(von|of)\\s+", "");
        return clean(t);
    }

    private static String clean(String s) {
        String t = STRIP_EDGES.matcher(s.trim()).replaceAll("").trim();
        return t.length() > 40 ? t.substring(0, 40) : t;
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s.replace(".", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
