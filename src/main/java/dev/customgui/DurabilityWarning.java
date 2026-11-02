package dev.customgui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Warnt, wenn ein Werkzeug, eine Waffe oder ein Rüstungsteil (alles im Inventar, Rüstung und Zweithand
 * eingeschlossen) nur noch wenig Haltbarkeit hat. Schwelle einstellbar (Standard 10 %).
 */
public final class DurabilityWarning {

    private record Warn(ItemStack stack, int percent) {
    }

    private static List<Warn> warnings = List.of();
    private static Set<String> known = new HashSet<>();
    private static int ticks = 0;

    private DurabilityWarning() {
    }

    public static void tick(Minecraft mc) {
        CustomGuiConfig cfg = CustomGuiConfig.get();
        if (mc.player == null || !cfg.durabilityWarn) {
            warnings = List.of();
            return;
        }
        if (++ticks % 10 != 0) return;

        List<Warn> list = new ArrayList<>();
        Set<String> now = new HashSet<>();
        boolean fresh = false;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty() || !st.isDamageableItem()) continue;
            int max = st.getMaxDamage();
            if (max <= 0) continue;
            int pct = Math.round((max - st.getDamageValue()) * 100f / max);
            if (pct <= cfg.durabilityPercent) {
                list.add(new Warn(st.copy(), pct));
                String id = i + ":" + st.getHoverName().getString();
                now.add(id);
                if (!known.contains(id)) fresh = true;
            }
        }
        list.sort((a, b) -> Integer.compare(a.percent(), b.percent()));
        warnings = list;
        known = now;
        if (fresh) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5f));
        }
    }

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || warnings.isEmpty() || !CustomGuiConfig.get().durabilityWarn) return;
        if (mc.gui.screen() != null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        int shown = Math.min(warnings.size(), 4);
        int y = 24;
        Component head = Component.literal("! " + Lang.t("dur.title") + " !")
                .withStyle(ChatFormatting.BOLD, blink ? ChatFormatting.RED : ChatFormatting.GOLD);
        gfx.text(mc.font, head, sw / 2 - mc.font.width(head) / 2, y, 0xFFFF5555);
        y += 12;
        for (int i = 0; i < shown; i++) {
            Warn w = warnings.get(i);
            Component line = Component.literal(w.stack().getHoverName().getString() + ": " + w.percent() + "%")
                    .withStyle(w.percent() <= 5 ? ChatFormatting.RED : ChatFormatting.GOLD);
            int lw = mc.font.width(line) + 20;
            int x = sw / 2 - lw / 2;
            gfx.item(w.stack(), x, y - 4);
            gfx.text(mc.font, line, x + 20, y, 0xFFFFFFFF);
            y += 18;
        }
        if (warnings.size() > shown) {
            Component more = Component.literal(Lang.t("hud.more", warnings.size() - shown));
            gfx.text(mc.font, more, sw / 2 - mc.font.width(more) / 2, y, 0xFF8A94AB);
        }
    }
}
