package dev.customgui;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Übersicht der Rübenpreise: Kennzahlen, Verlauf (Woche/Monat/Jahr) und Prognose. */
public class TurnipScreen extends Screen {

    private static final int BG = 0x9A0A0F1A;
    private static final int BORDER = 0x44FFFFFF;
    private static final int HEADER = 0x33000000;
    private static final int TEXT = 0xFFE6EAF2;
    private static final int MUTED = 0xFF8A94AB;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int GOLD = 0xFFFFC107;

    private static final int[] RANGES = {7, 30, 365};
    private static final String[] RANGE_KEYS = {"tr.week", "tr.month", "tr.year"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.");

    private int range = 7;
    private int px, py, pw, ph;

    // Diagramm-Geometrie (wird beim Zeichnen gesetzt)
    private long startDay;
    private double span;
    private double lo, hi;
    private int cx, cy, cw, ch;

    public TurnipScreen() {
        super(Component.literal("Rübenpreis"));
    }

    @Override
    protected void init() {
        pw = Math.min(480, this.width - 16);
        ph = Math.min(300, this.height - 16);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
    }

    private static String fmt(double v) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(Math.round(v));
    }

    private static String pct(double now, double base) {
        if (base == 0) return "";
        return String.format(Locale.GERMANY, "%+.1f %%", (now - base) / base * 100.0);
    }

    private static int deltaColor(double now, double base) {
        return now >= base ? GREEN : RED;
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gfx, mouseX, mouseY, delta);

        int accent = 0xFF000000 | CustomGuiConfig.ACCENTS[CustomGuiConfig.get().accent];
        List<TurnipPrices.Point> pts = TurnipPrices.points();

        gfx.fill(px, py, px + pw, py + ph, BG);
        frame(gfx, px, py, px + pw, py + ph, BORDER);
        gfx.fill(px + 1, py + 1, px + pw - 1, py + 28, HEADER);
        gfx.fill(px + 1, py + 28, px + pw - 1, py + 29, (accent & 0x00FFFFFF) | 0xCC000000);
        gfx.text(this.font, Component.literal(Lang.t("tr.title")).withStyle(ChatFormatting.BOLD), px + 14, py + 10, TEXT);

        // Zeitraum-Knöpfe
        for (int i = 0; i < RANGES.length; i++) {
            int bx = rangeX(i);
            boolean active = RANGES[i] == range;
            boolean hover = mouseX >= bx && mouseX < bx + 54 && mouseY >= py + 5 && mouseY < py + 23;
            gfx.fill(bx, py + 5, bx + 54, py + 23, active ? ((accent & 0x00FFFFFF) | 0x55000000) : (hover ? 0x33FFFFFF : 0x22FFFFFF));
            frame(gfx, bx, py + 5, bx + 54, py + 23, active ? accent : 0x3CFFFFFF);
            Component c = Component.literal(Lang.t(RANGE_KEYS[i]));
            gfx.text(this.font, c, bx + 27 - this.font.width(c) / 2, py + 10, active ? TEXT : MUTED);
        }

        if (pts.isEmpty()) {
            Component c = Component.literal(Lang.t("tr.nodata"));
            gfx.text(this.font, c, this.width / 2 - this.font.width(c) / 2, py + ph / 2, MUTED);
            return;
        }

        TurnipPrices.Forecast f = TurnipPrices.forecast(pts);
        int n = pts.size();
        double now = pts.get(n - 1).price();

        // ---- Kennzahlen
        int x = px + 14;
        int y1 = py + 36;
        x = chip(gfx, x, y1, Lang.t("tr.today"), fmt(now), TEXT);
        if (n >= 2) {
            double prev = pts.get(n - 2).price();
            x = chip(gfx, x, y1, Lang.t("tr.yesterday"), fmt(prev) + " (" + pct(now, prev) + ")", deltaColor(now, prev));
        }
        double avg7 = 0;
        int from7 = Math.max(0, n - 7);
        for (int i = from7; i < n; i++) avg7 += pts.get(i).price();
        avg7 /= (n - from7);
        chip(gfx, x, y1, Lang.t("tr.avg7"), fmt(avg7), TEXT);

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (TurnipPrices.Point p : pts) {
            min = Math.min(min, p.price());
            max = Math.max(max, p.price());
        }
        int y2 = py + 49;
        x = px + 14;
        x = chip(gfx, x, y2, Lang.t("tr.min"), fmt(min), TEXT);
        x = chip(gfx, x, y2, Lang.t("tr.max"), fmt(max), TEXT);
        chip(gfx, x, y2, Lang.t("tr.tracked"), String.valueOf(n), n < 5 ? GOLD : TEXT);
        if (n < 5) {
            gfx.text(this.font, Component.literal(Lang.t("tr.few")), px + 14, py + 62, GOLD);
        }

        // ---- Diagramm
        cx = px + 64;
        cy = py + 78;
        cw = pw - 64 - 16;
        ch = ph - 78 - 62;
        drawChart(gfx, pts, f, accent, mouseX, mouseY);

        // ---- Prognose-Kacheln
        int[] horizons = {1, 7, 30, 365};
        String[] keys = {"tr.tomorrow", "tr.week1", "tr.month1", "tr.year1"};
        int gap = 8;
        int bw = (pw - 28 - gap * 3) / 4;
        int by = py + ph - 52;
        for (int i = 0; i < 4; i++) {
            int bx = px + 14 + i * (bw + gap);
            gfx.fill(bx, by, bx + bw, by + 34, 0x22FFFFFF);
            frame(gfx, bx, by, bx + bw, by + 34, 0x3CFFFFFF);
            double v = f.value(horizons[i]);
            double sd = f.sd(horizons[i]);
            gfx.text(this.font, Component.literal(Lang.t(keys[i])), bx + 6, by + 4, MUTED);
            gfx.text(this.font, Component.literal("\u2248 " + fmt(v)), bx + 6, by + 14, TEXT);
            Component d = Component.literal(pct(v, now) + "  \u00B1" + fmt(sd));
            gfx.text(this.font, d, bx + 6, by + 24, deltaColor(v, now));
        }
        Component disc = Component.literal(Lang.t("tr.disclaimer"));
        gfx.text(this.font, disc, px + pw / 2 - this.font.width(disc) / 2, py + ph - 12, 0xFF566079);
    }

    private int rangeX(int i) {
        return px + pw - 14 - (RANGES.length - i) * 58 + 4;
    }

    /** Beschriftung + Wert; liefert die nächste freie x-Position. */
    private int chip(GuiGraphicsExtractor gfx, int x, int y, String label, String value, int valueColor) {
        Component l = Component.literal(label + " ");
        gfx.text(this.font, l, x, y, MUTED);
        x += this.font.width(l);
        Component v = Component.literal(value);
        gfx.text(this.font, v, x, y, valueColor);
        return x + this.font.width(v) + 16;
    }

    private int toX(double day) {
        return cx + (int) Math.round((day - startDay) / span * cw);
    }

    private int toY(double v) {
        return cy + ch - (int) Math.round((v - lo) / (hi - lo) * ch);
    }

    private void drawChart(GuiGraphicsExtractor gfx, List<TurnipPrices.Point> pts, TurnipPrices.Forecast f,
                           int accent, int mouseX, int mouseY) {
        int n = pts.size();
        long lastDay = pts.get(n - 1).day();
        startDay = Math.max(pts.get(0).day(), lastDay - range + 1);
        if (lastDay - startDay < 2) startDay = lastDay - 2;
        long endDay = lastDay + range;
        span = endDay - startDay;

        // Wertebereich aus Verlauf und Prognose (inkl. Unsicherheitsband)
        lo = Double.MAX_VALUE;
        hi = -Double.MAX_VALUE;
        for (TurnipPrices.Point p : pts) {
            if (p.day() >= startDay) {
                lo = Math.min(lo, p.price());
                hi = Math.max(hi, p.price());
            }
        }
        int step = Math.max(1, range / 60);
        for (int h = 0; h <= range; h += step) {
            lo = Math.min(lo, f.value(h) - f.sd(h));
            hi = Math.max(hi, f.value(h) + f.sd(h));
        }
        if (hi - lo < 1) {
            hi += 1;
            lo -= 1;
        }
        double pad = (hi - lo) * 0.08;
        lo = Math.max(0, lo - pad);
        hi += pad;

        gfx.fill(cx, cy, cx + cw, cy + ch, 0x22000000);
        frame(gfx, cx, cy, cx + cw, cy + ch, 0x33FFFFFF);

        // Gitter und Beschriftung links
        for (int i = 0; i <= 4; i++) {
            double v = lo + (hi - lo) * i / 4.0;
            int gy = toY(v);
            gfx.fill(cx + 1, Math.min(cy + ch - 1, Math.max(cy, gy)), cx + cw - 1,
                    Math.min(cy + ch, Math.max(cy + 1, gy + 1)), 0x1AFFFFFF);
            Component c = Component.literal(fmt(v));
            gfx.text(this.font, c, cx - 5 - this.font.width(c), gy - 4, MUTED);
        }

        // Prognose: Band und gestrichelte Linie
        int x0 = toX(lastDay);
        int x1 = toX(endDay);
        for (int x = x0; x <= x1; x++) {
            double h = (x - x0) / (double) Math.max(1, x1 - x0) * range;
            double v = f.value(h);
            double s = f.sd(h);
            int yTop = Math.max(cy + 1, toY(v + s));
            int yBot = Math.min(cy + ch - 1, toY(v - s));
            if (yBot >= yTop) {
                gfx.fill(x, yTop, x + 1, yBot + 1, (accent & 0x00FFFFFF) | 0x28000000);
            }
            if (((x - x0) / 4) % 2 == 0) {
                int y = Math.max(cy + 1, Math.min(cy + ch - 2, toY(v)));
                gfx.fill(x, y, x + 1, y + 2, GOLD);
            }
        }

        // "Heute"-Linie
        gfx.fill(x0, cy + 1, x0 + 1, cy + ch - 1, 0x44FFFFFF);

        // Verlauf: Linie und Punkte
        TurnipPrices.Point prev = null;
        for (TurnipPrices.Point p : pts) {
            if (p.day() < startDay) continue;
            if (prev != null) {
                drawLine(gfx, toX(prev.day()), toY(prev.price()), toX(p.day()), toY(p.price()), accent);
            }
            prev = p;
        }
        for (TurnipPrices.Point p : pts) {
            if (p.day() < startDay) continue;
            int px2 = toX(p.day());
            int py2 = toY(p.price());
            gfx.fill(px2 - 2, py2 - 2, px2 + 3, py2 + 3, accent);
        }

        // Datum unten
        gfx.text(this.font, Component.literal(LocalDate.ofEpochDay(startDay).format(DATE)), cx, cy + ch + 4, MUTED);
        Component mid = Component.literal(Lang.t("tr.now") + " " + LocalDate.ofEpochDay(lastDay).format(DATE));
        gfx.text(this.font, mid, Math.max(cx, Math.min(cx + cw - this.font.width(mid), x0 - this.font.width(mid) / 2)),
                cy + ch + 4, TEXT);
        Component end = Component.literal(LocalDate.ofEpochDay(endDay).format(DATE));
        gfx.text(this.font, end, cx + cw - this.font.width(end), cy + ch + 14, GOLD);

        // Hover: Wert unter der Maus
        if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch) {
            long day = startDay + Math.round((mouseX - cx) / (double) cw * span);
            String text;
            if (day <= lastDay) {
                TurnipPrices.Point best = null;
                for (TurnipPrices.Point p : pts) {
                    if (best == null || Math.abs(p.day() - day) < Math.abs(best.day() - day)) best = p;
                }
                if (best == null || Math.abs(best.day() - day) > 1) return;
                text = LocalDate.ofEpochDay(best.day()).format(DATE) + ": " + fmt(best.price());
            } else {
                double h = day - lastDay;
                text = LocalDate.ofEpochDay(day).format(DATE) + ": \u2248 " + fmt(f.value(h)) + " \u00B1" + fmt(f.sd(h));
            }
            gfx.fill(mouseX, cy + 1, mouseX + 1, cy + ch - 1, 0x55FFFFFF);
            int tw = this.font.width(text) + 10;
            int tx = mouseX + 8 + tw > cx + cw ? mouseX - tw - 4 : mouseX + 8;
            gfx.fill(tx, cy + 4, tx + tw, cy + 18, 0xE00A0F1A);
            frame(gfx, tx, cy + 4, tx + tw, cy + 18, accent);
            gfx.text(this.font, Component.literal(text), tx + 5, cy + 8, TEXT);
        }
    }

    /** Verbindet zwei Punkte mit einer 2 Pixel dicken Linie. */
    private void drawLine(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) steps = 1;
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            gfx.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static void frame(GuiGraphicsExtractor gfx, int x1, int y1, int x2, int y2, int c) {
        gfx.fill(x1, y1, x2, y1 + 1, c);
        gfx.fill(x1, y2 - 1, x2, y2, c);
        gfx.fill(x1, y1 + 1, x1 + 1, y2 - 1, c);
        gfx.fill(x2 - 1, y1 + 1, x2, y2 - 1, c);
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < RANGES.length; i++) {
                int bx = rangeX(i);
                if (event.x() >= bx && event.x() < bx + 54 && event.y() >= py + 5 && event.y() < py + 23) {
                    range = RANGES[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(new CustomGuiScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
