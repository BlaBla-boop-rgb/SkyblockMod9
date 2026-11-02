package dev.customgui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

/**
 * Kleines Fenster auf dem Bildschirm mit den Items, die die Marktstand-Kunden noch brauchen:
 * Besitzer/Insel, pro Produkt "fehlt", "im Inventar", "abgegeben" und (wenn bekannt) "im Lager".
 * Die Größe ist in der Config einstellbar (hudScale in Prozent).
 */
public final class MarktstandHud {

    private static final int MAX_ROWS = 6;
    private static final int BG = 0x9A0A0F1A;
    private static final int BORDER = 0x44FFFFFF;
    private static final int ROW = 26;
    private static final int TEXT = 0xFFE6EAF2;
    private static final int MUTED = 0xFF8A94AB;

    private MarktstandHud() {
    }

    public static float scale() {
        return CustomGuiConfig.get().hudScale / 100f;
    }

    /** preview = true zeigt Beispieldaten, wenn noch nichts gelesen wurde (zum Verschieben). */
    public static List<MarktstandTracker.Entry> shown(boolean preview) {
        List<MarktstandTracker.Entry> list = MarktstandTracker.entries();
        if (list.isEmpty() && preview) {
            Minecraft mc = Minecraft.getInstance();
            return List.of(new MarktstandTracker.Entry("Magmacreme", 62, 18, 80, ItemIcons.find(mc, "Magmacreme")),
                    new MarktstandTracker.Entry("Weizen", 128, 0, 128, ItemIcons.find(mc, "Weizen")));
        }
        return list;
    }

    private static String ownerLine(boolean preview) {
        String owner = MarktstandTracker.owner();
        String island = MarktstandTracker.island();
        if (owner.isEmpty() && island.isEmpty() && preview) {
            owner = "Beispiel";
        }
        StringBuilder sb = new StringBuilder();
        if (!owner.isEmpty()) sb.append(Lang.t("hud.owner")).append(' ').append(owner);
        if (!island.isEmpty()) {
            if (sb.length() > 0) sb.append("  \u00B7  ");
            sb.append(Lang.t("hud.island")).append(' ').append(island);
        }
        return sb.toString();
    }

    private static String fmt(long n) {
        return String.format(Locale.GERMANY, "%,d", n);
    }

    private static Component detail(Minecraft mc, MarktstandTracker.Entry e, boolean showStorage) {
        long inv = ItemIcons.countInInventory(mc, e.product());
        MutableComponent c = Component.literal(Lang.t("hud.inv") + " " + fmt(inv))
                .withStyle(inv >= e.missing() ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        c.append(Component.literal("  \u00B7  " + Lang.t("hud.done") + " " + fmt(e.delivered()) + "/" + fmt(e.total()))
                .withStyle(ChatFormatting.GRAY));
        if (showStorage) {
            long st = ItemStorage.get(e.product());
            if (st >= 0) {
                c.append(Component.literal("  \u00B7  " + Lang.t("hud.storage") + " " + fmt(st))
                        .withStyle(st + inv >= e.missing() ? ChatFormatting.AQUA : ChatFormatting.RED));
            }
        }
        return c;
    }

    /** Größe des Fensters bei 100 % (ohne Skalierung). */
    public static int[] size(Font font, List<MarktstandTracker.Entry> list, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        boolean storage = CustomGuiConfig.get().showStorage;
        int shownRows = Math.min(list.size(), MAX_ROWS);
        int rows = shownRows + (list.size() > MAX_ROWS ? 1 : 0);
        int w = font.width(Lang.t("hud.title")) + 8;
        String ol = ownerLine(preview);
        int head = 24;
        if (!ol.isEmpty()) {
            w = Math.max(w, font.width(ol));
            head += 11;
        }
        for (int i = 0; i < shownRows; i++) {
            MarktstandTracker.Entry e = list.get(i);
            int line1 = 22 + font.width(e.product()) + 14 + font.width(Lang.t("hud.missing") + " " + fmt(e.missing()));
            int line2 = 22 + font.width(detail(mc, e, storage));
            w = Math.max(w, Math.max(line1, line2));
        }
        return new int[]{w + 16, head + rows * ROW + 4};
    }

    /** Position aus der Config; hudX = -1 heißt: rechts oben. w/h sind die Maße auf dem Bildschirm (skaliert). */
    public static int[] position(Minecraft mc, int w, int h) {
        CustomGuiConfig c = CustomGuiConfig.get();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = c.hudX < 0 ? sw - w - 6 : c.hudX;
        int y = c.hudY;
        x = Math.max(0, Math.min(Math.max(0, sw - w), x));
        y = Math.max(0, Math.min(Math.max(0, sh - h), y));
        return new int[]{x, y};
    }

    /** Zeichnet das Fenster. w/h sind die unskalierten Maße aus size(); x/y die Bildschirmposition. */
    public static void draw(GuiGraphicsExtractor gfx, Font font, int x, int y, List<MarktstandTracker.Entry> list,
                            int w, int h, int accentRgb, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        boolean storage = CustomGuiConfig.get().showStorage;
        float s = scale();
        int accent = 0xFF000000 | accentRgb;

        gfx.pose().pushMatrix();
        gfx.pose().translate((float) x, (float) y);
        gfx.pose().scale(s, s);

        gfx.fill(0, 0, w, h, BG);
        gfx.fill(0, 0, w, 1, BORDER);
        gfx.fill(0, h - 1, w, h, BORDER);
        gfx.fill(0, 1, 1, h - 1, BORDER);
        gfx.fill(w - 1, 1, w, h - 1, BORDER);
        gfx.fill(1, 18, w - 1, 19, (accent & 0x00FFFFFF) | 0xAA000000);
        gfx.text(font, Component.literal(Lang.t("hud.title")).withStyle(ChatFormatting.BOLD), 8, 5, TEXT);

        int ry = 24;
        String ol = ownerLine(preview);
        if (!ol.isEmpty()) {
            gfx.text(font, Component.literal(ol), 8, ry - 1, MUTED);
            ry += 11;
        }

        int shownRows = Math.min(list.size(), MAX_ROWS);
        for (int i = 0; i < shownRows; i++) {
            MarktstandTracker.Entry e = list.get(i);
            if (e.icon() != null) {
                gfx.item(e.icon(), 5, ry + 4);
            }
            gfx.text(font, Component.literal(e.product()), 26, ry + 2, TEXT);
            Component missing = Component.literal(Lang.t("hud.missing") + " " + fmt(e.missing()));
            gfx.text(font, missing, w - 8 - font.width(missing), ry + 2, accent);
            gfx.text(font, detail(mc, e, storage), 26, ry + 14, MUTED);
            ry += ROW;
        }
        if (list.size() > MAX_ROWS) {
            gfx.text(font, Component.literal(Lang.t("hud.more", list.size() - MAX_ROWS)), 8, ry + 4, MUTED);
        }

        gfx.pose().popMatrix();
    }

    /** Wird von Fabric jeden Frame aufgerufen. */
    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        CustomGuiConfig cfg = CustomGuiConfig.get();
        if (!cfg.hudEnabled || mc.player == null) return;
        if (mc.gui.screen() instanceof HudMoveScreen) return; // dort wird die Vorschau gezeichnet

        List<MarktstandTracker.Entry> list = shown(false);
        if (list.isEmpty()) return;
        int[] s = size(mc.font, list, false);
        float sc = scale();
        int[] p = position(mc, Math.round(s[0] * sc), Math.round(s[1] * sc));
        draw(gfx, mc.font, p[0], p[1], list, s[0], s[1], CustomGuiConfig.ACCENTS[cfg.accent], false);
    }
}
