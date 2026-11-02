package dev.customgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merkt sich, was in deinem Itemlager liegt. Sobald du das Lager öffnest (Titel enthält das Stichwort
 * aus der Config, Standard "lager"), werden die Items gelesen und in config/customgui_lager.json gespeichert.
 * Steht in der Lore eine Menge (z. B. "Anzahl: 12.345"), wird diese genommen, sonst die Stack-Größe.
 */
public final class ItemStorage {

    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)(?:anzahl|menge|lagernd|gelagert|bestand|vorrat|amount|stored)\\s*[:»\\-]?\\s*([\\d.,]+)");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {
    }.getType();

    private static Map<String, Long> amounts;
    private static int ticks = 0;
    private static boolean dirty = false;

    private ItemStorage() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("customgui_lager.json");
    }

    private static void ensureLoaded() {
        if (amounts != null) return;
        amounts = new HashMap<>();
        try {
            Path p = path();
            if (Files.exists(p)) {
                Map<String, Long> m = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), MAP_TYPE);
                if (m != null) amounts.putAll(m);
            }
        } catch (Exception e) {
            System.err.println("[CustomGui] Lager konnte nicht gelesen werden: " + e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(amounts, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[CustomGui] Lager konnte nicht gespeichert werden: " + e);
        }
        dirty = false;
    }

    /** Gemerkte Menge im Lager oder -1, wenn das Lager noch nie geöffnet wurde / das Item dort nicht liegt. */
    public static long get(String product) {
        ensureLoaded();
        for (Map.Entry<String, Long> e : amounts.entrySet()) {
            if (ItemIcons.nameMatches(e.getKey(), product)) return e.getValue();
        }
        return -1;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            if (dirty) save();
            return;
        }
        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        String keyword = CustomGuiConfig.get().storageKeyword.toLowerCase(Locale.ROOT);
        if (title.contains("marktstand") || !title.contains(keyword)) return;
        if (++ticks % 10 != 0) return;
        ensureLoaded();

        List<Slot> slots = screen.getMenu().slots;
        int limit = Math.max(0, slots.size() - 36); // die letzten 36 Slots sind dein Inventar
        Map<String, Long> page = new HashMap<>();
        for (int i = 0; i < limit; i++) {
            ItemStack st = slots.get(i).getItem();
            if (st.isEmpty()) continue;
            String name = st.getHoverName().getString().trim();
            if (name.isEmpty()) continue;
            long fromLore = loreAmount(st);
            if (fromLore >= 0) {
                page.merge(name, fromLore, Math::max);
            } else {
                page.merge(name, (long) st.getCount(), Long::sum);
            }
        }
        for (Map.Entry<String, Long> e : page.entrySet()) {
            if (!e.getValue().equals(amounts.get(e.getKey()))) {
                amounts.put(e.getKey(), e.getValue());
                dirty = true;
            }
        }
    }

    private static long loreAmount(ItemStack st) {
        ItemLore lore = st.get(DataComponents.LORE);
        if (lore == null) return -1;
        for (Component line : lore.lines()) {
            Matcher m = AMOUNT.matcher(line.getString());
            if (m.find()) {
                try {
                    return Long.parseLong(m.group(1).replace(".", "").replace(",", ""));
                } catch (NumberFormatException ignored) {
                    // nächste Zeile probieren
                }
            }
        }
        return -1;
    }
}
