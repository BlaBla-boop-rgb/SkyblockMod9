package dev.customgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merkt sich den täglichen Rübenpreis. Sobald du das Rübenpreis-Menü öffnest, liest die Mod
 * "Rübenpreis: ..." und die Zeile "alt -> neu" (Vortag -> heute) und speichert pro Tag einen Wert
 * in config/customgui_ruebenpreis.json. Daraus entstehen Verlauf und Prognose.
 */
public final class TurnipPrices {

    public record Point(long day, double price) {
    }

    private static final class Store {
        TreeMap<String, Double> prices = new TreeMap<>();
    }

    private static final Pattern NUM = Pattern.compile("\\d[\\d.]*(?:,\\d+)?");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TreeMap<String, Double> prices;
    private static int ticks = 0;

    private TurnipPrices() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("customgui_ruebenpreis.json");
    }

    private static void ensureLoaded() {
        if (prices != null) return;
        prices = new TreeMap<>();
        Path p = path();
        try {
            if (Files.exists(p)) {
                Store s = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Store.class);
                if (s != null && s.prices != null) prices.putAll(s.prices);
            }
        } catch (Exception e) {
            System.err.println("[CustomGui] Rübenpreise konnten nicht gelesen werden: " + e);
        }
        if (prices.isEmpty()) {
            // Startwerte aus dem Rübenpreis-Menü (Vortag und 01.11.2026)
            prices.put("2026-10-31", 61548.0);
            prices.put("2026-11-01", 70425.0);
            save();
        }
    }

    private static void save() {
        try {
            Store s = new Store();
            s.prices = prices;
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(s), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[CustomGui] Rübenpreise konnten nicht gespeichert werden: " + e);
        }
    }

    public static List<Point> points() {
        ensureLoaded();
        List<Point> list = new ArrayList<>();
        prices.forEach((k, v) -> {
            try {
                list.add(new Point(LocalDate.parse(k).toEpochDay(), v));
            } catch (Exception ignored) {
                // kaputter Eintrag
            }
        });
        return list;
    }

    // ------------------------------------------------------------------ Preis aus dem Menü lesen

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        if (++ticks % 5 != 0) return;

        List<Slot> slots = screen.getMenu().slots;
        int limit = Math.max(0, slots.size() - 36); // die letzten 36 Slots sind dein Inventar

        double today = -1;
        double yesterday = -1;
        double pairToday = -1;

        for (int i = 0; i < limit; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            List<String> lines = new ArrayList<>();
            lines.add(stack.getHoverName().getString());
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component l : lore.lines()) lines.add(l.getString());
            }
            boolean hasVortag = lines.stream().anyMatch(l -> l.contains("Vortag"));

            for (String line : lines) {
                int idx = line.indexOf("Rübenpreis");
                if (idx >= 0) {
                    Matcher m = NUM.matcher(line);
                    m.region(idx, line.length());
                    if (m.find()) today = parse(m.group());
                }
                if (hasVortag) {
                    Matcher m = NUM.matcher(line);
                    List<Double> nums = new ArrayList<>();
                    while (m.find()) nums.add(parse(m.group()));
                    if (nums.size() == 2) { // "alt -> neu"
                        yesterday = nums.get(0);
                        pairToday = nums.get(1);
                    }
                }
            }
        }

        if (today <= 0) today = pairToday;
        if (today <= 0) return; // das ist nicht das Rübenpreis-Menü

        ensureLoaded();
        LocalDate now = LocalDate.now();
        boolean changed = false;
        String key = now.toString();
        Double old = prices.get(key);
        if (old == null || Math.abs(old - today) > 0.004) {
            prices.put(key, today);
            changed = true;
        }
        String yKey = now.minusDays(1).toString();
        if (yesterday > 0 && !prices.containsKey(yKey)) {
            prices.put(yKey, yesterday);
            changed = true;
        }
        if (changed) save();
    }

    /** "70.425" -> 70425, "61.548,00" -> 61548.0 (Punkt = Tausender, Komma = Dezimal). */
    private static double parse(String s) {
        try {
            return Double.parseDouble(s.replace(".", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ Prognose

    /**
     * Einfaches Rückkehr-zum-Mittelwert-Modell: Der Preis wandert von seinem letzten Wert zum
     * Durchschnitt der letzten bis zu 30 Tage. Die Unsicherheit wächst mit dem Abstand.
     * Das ist eine grobe Schätzung, keine Garantie.
     */
    public static final class Forecast {
        public final double last;
        public final double mean;
        public final double phi;
        public final double sd;
        public final int n;

        Forecast(double last, double mean, double phi, double sd, int n) {
            this.last = last;
            this.mean = mean;
            this.phi = phi;
            this.sd = sd;
            this.n = n;
        }

        /** Erwarteter Preis in h Tagen nach dem letzten Messwert. */
        public double value(double h) {
            return mean + (last - mean) * Math.pow(phi, h);
        }

        /** Ungefähre Standardabweichung der Schätzung in h Tagen. */
        public double sd(double h) {
            return sd * Math.sqrt(Math.max(0, 1 - Math.pow(phi, 2 * h)));
        }
    }

    public static Forecast forecast(List<Point> pts) {
        int n = pts.size();
        double last = pts.get(n - 1).price();
        int from = Math.max(0, n - 30);
        List<Double> w = new ArrayList<>();
        for (int i = from; i < n; i++) w.add(pts.get(i).price());

        double mean = 0;
        for (double v : w) mean += v;
        mean /= w.size();

        double sd;
        if (w.size() < 3) {
            sd = 0.12 * last; // zu wenig Daten: grobe Annahme
        } else {
            double s = 0;
            for (double v : w) s += (v - mean) * (v - mean);
            sd = Math.sqrt(s / w.size());
        }

        double phi = 0.5;
        if (w.size() >= 6) {
            double num = 0;
            double den = 0;
            for (int i = 0; i < w.size() - 1; i++) {
                num += (w.get(i) - mean) * (w.get(i + 1) - mean);
            }
            for (double v : w) den += (v - mean) * (v - mean);
            if (den > 0) phi = num / den;
        }
        phi = Math.max(0, Math.min(0.9, phi));
        return new Forecast(last, mean, phi, sd, n);
    }
}
