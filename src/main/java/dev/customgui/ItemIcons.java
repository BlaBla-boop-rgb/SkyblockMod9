package dev.customgui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Findet zu einem Produktnamen (z. B. "Magmacreme") ein Item zum Anzeigen. Zuerst wird das
 * Inventar durchsucht (findet auch Server-Items mit eigenem Namen), danach alle Vanilla-Items.
 */
public final class ItemIcons {

    private static Map<String, ItemStack> index;

    private ItemIcons() {
    }

    public static ItemStack find(Minecraft mc, String product) {
        String key = norm(product);

        if (mc.player != null) {
            Inventory inv = mc.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (!st.isEmpty() && norm(st.getHoverName().getString()).equals(key)) {
                    return st.copyWithCount(1);
                }
            }
        }

        if (index == null) {
            index = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack st = item.getDefaultInstance();
                if (st.isEmpty()) continue;
                index.putIfAbsent(norm(st.getHoverName().getString()), st);
            }
        }

        ItemStack hit = index.get(key);
        if (hit == null && key.endsWith("n")) hit = index.get(key.substring(0, key.length() - 1)); // Kerzen -> Kerze
        if (hit == null && key.endsWith("en")) hit = index.get(key.substring(0, key.length() - 2));
        if (hit == null && key.endsWith("s")) hit = index.get(key.substring(0, key.length() - 1));
        return hit;
    }

    /** Passt der Item-Name zum Produktnamen? Gleiche Plural-Toleranz wie bei der Icon-Suche. */
    public static boolean nameMatches(String itemName, String product) {
        String name = norm(itemName);
        String key = norm(product);
        if (name.equals(key)) return true;
        if (key.endsWith("n") && name.equals(key.substring(0, key.length() - 1))) return true;
        if (key.endsWith("en") && name.equals(key.substring(0, key.length() - 2))) return true;
        return key.endsWith("s") && name.equals(key.substring(0, key.length() - 1));
    }

    /** Zählt, wie viele Items mit diesem Namen in deinem Inventar liegen. */
    public static long countInInventory(Minecraft mc, String product) {
        if (mc.player == null) return 0;
        long sum = 0;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && nameMatches(st.getHoverName().getString(), product)) {
                sum += st.getCount();
            }
        }
        return sum;
    }

    public static String norm(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
