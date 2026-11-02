package dev.customgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomGuiScreen extends Screen {

    private enum Tab {
        HOME("tab.home", "tip.tab.home"),
        ISLANDS("tab.islands", "tip.tab.islands"),
        MARKT("tab.markt", "tip.tab.markt"),
        TOOLS("tab.tools", "tip.tab.tools"),
        SETTINGS("tab.settings", "tip.tab.settings");

        final String key;
        final String tipKey;

        Tab(String key, String tipKey) {
            this.key = key;
            this.tipKey = tipKey;
        }
    }

    private static final int MAX_TABS = 5;
    private static final int ROWS = 4;
    private static final int ROW_PITCH = 36;

    // Farben (ARGB)
    private static final int BG = 0x9A0A0F1A;
    private static final int BORDER = 0x44FFFFFF;
    private static final int HEADER = 0x33000000;
    private static final int BTN = 0x22FFFFFF;
    private static final int BTN_BORDER = 0x3CFFFFFF;
    private static final int TEXT = 0xFFE6EAF2;
    private static final int MUTED = 0xFF8A94AB;
    private static final int DIM = 0xFF566079;
    private static final int RED = 0xFF5555;
    private static final int GREEN = 0x55FF55;

    /** Flacher Button. hover läuft weich zwischen 0 und 1. */
    private static final class Btn {
        final String label;
        final String tip;
        final Runnable action;
        String sub = "";        // zweite Zeile (Inselzeile, Kachel)
        String value = "";      // Wert rechts, z. B. "An"
        int valueMode = 0;      // 0 neutral, 1 an (Akzentfarbe), 2 aus (gedimmt)
        int fixed = 0;          // 0 = Akzentfarbe, sonst feste RGB-Farbe (z. B. Rot)
        boolean left = false;
        boolean bold = false;
        boolean dim = false;    // ausgeblendetes Element: Text gedimmt
        boolean active = false; // dauerhaft hervorgehoben
        int x, y, w, h;
        float hover = 0f;
        boolean wasHovered = false;

        Btn(String label, String tip, Runnable action) {
            this.label = label;
            this.tip = tip;
            this.action = action;
        }

        Btn at(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        Btn sub(String s) {
            this.sub = s;
            return this;
        }

        Btn value(String v, int mode) {
            this.value = v;
            this.valueMode = mode;
            return this;
        }

        Btn fixed(int rgb) {
            this.fixed = rgb;
            return this;
        }

        Btn left() {
            this.left = true;
            return this;
        }

        Btn bold() {
            this.bold = true;
            return this;
        }

        Btn dim() {
            this.dim = true;
            return this;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Btn> buttons = new ArrayList<>();
    private final float[] tabHover = new float[MAX_TABS];
    private final boolean[] tabWasHovered = new boolean[MAX_TABS];
    private final int[] tabX = new int[MAX_TABS];
    private final int[] tabW = new int[MAX_TABS];
    private List<Tab> tabs = new ArrayList<>();

    private Tab tab = Tab.HOME;
    private boolean formMode = false;
    private boolean editMode = false;    // Inseln: Ausblenden/Löschen-Buttons sichtbar
    private boolean listening = false;   // wartet auf neue Taste/Maustaste für "Menü öffnen"
    private boolean shiftDown = false;
    private boolean homeEmpty = false;
    private String formName = "";
    private String formCmd = "";
    private String formError = "";
    private int page = 0;

    private EditBox nameBox;
    private EditBox cmdBox;

    // Layout
    private static final int PW = 330;
    private int ph, rowW;
    private int px, py;

    private float indicatorX = -1f;
    private float indicatorW = -1f;
    private long openedAt = System.nanoTime();
    private long lastFrame = System.nanoTime();

    public CustomGuiScreen() {
        super(Component.literal("Custom GUI"));
    }

    // ------------------------------------------------------------------ Aufbau

    private List<Tab> visibleTabs() {
        List<Tab> list = new ArrayList<>();
        list.add(Tab.HOME);
        if (CustomGuiConfig.get().showIslandsTab) {
            list.add(Tab.ISLANDS);
        }
        list.add(Tab.MARKT);
        list.add(Tab.TOOLS);
        list.add(Tab.SETTINGS);
        return list;
    }

    private void computeLayout() {
        tabs = visibleTabs();
        if (!tabs.contains(tab)) {
            tab = Tab.HOME;
        }
        rowW = PW - 36;
        ph = switch (tab) {
            case HOME -> 220;
            case ISLANDS -> 262;
            case MARKT -> 262;
            case TOOLS -> 212;
            case SETTINGS -> 288;
        };
        px = (this.width - PW) / 2;
        py = Math.max(2, (this.height - ph) / 2);
        computeTabs();
    }

    /** Tab-Breiten richten sich nach dem Text. */
    private void computeTabs() {
        int gap = 4;
        int total = gap * (tabs.size() - 1);
        for (int i = 0; i < tabs.size(); i++) {
            tabW[i] = this.font.width(Component.literal(Lang.t(tabs.get(i).key)).withStyle(ChatFormatting.BOLD)) + 16;
            total += tabW[i];
        }
        int x = px + (PW - total) / 2;
        for (int i = 0; i < tabs.size(); i++) {
            tabX[i] = x;
            x += tabW[i] + gap;
        }
    }

    @Override
    protected void init() {
        computeLayout();

        nameBox = new EditBox(this.font, px + 30, py + 82, PW - 60, 20, Component.literal("Name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.literal(Lang.t("form.name.hint")).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(formName);
        nameBox.setResponder(s -> formName = s);
        this.addWidget(nameBox);

        cmdBox = new EditBox(this.font, px + 30, py + 128, PW - 60, 20, Component.literal("Command"));
        cmdBox.setMaxLength(100);
        cmdBox.setHint(Component.literal(Lang.t("form.cmd.hint")).withStyle(ChatFormatting.DARK_GRAY));
        cmdBox.setValue(formCmd);
        cmdBox.setResponder(s -> formCmd = s);
        this.addWidget(cmdBox);

        updateBoxes();
        rebuild();
        openedAt = System.nanoTime();
        lastFrame = openedAt;
    }

    /** Nach Tab-, Layout-, Sprach- oder Sichtbarkeits-Änderung: alles neu berechnen. */
    private void applyLayout() {
        computeLayout();
        nameBox.setX(px + 30);
        nameBox.setY(py + 82);
        nameBox.setWidth(PW - 60);
        nameBox.setHint(Component.literal(Lang.t("form.name.hint")).withStyle(ChatFormatting.DARK_GRAY));
        cmdBox.setX(px + 30);
        cmdBox.setY(py + 128);
        cmdBox.setWidth(PW - 60);
        cmdBox.setHint(Component.literal(Lang.t("form.cmd.hint")).withStyle(ChatFormatting.DARK_GRAY));
        updateBoxes();
        rebuild();
    }

    private void updateBoxes() {
        boolean show = formMode && tab == Tab.ISLANDS;
        nameBox.setVisible(show);
        nameBox.active = show;
        cmdBox.setVisible(show);
        cmdBox.active = show;
        if (show) {
            this.setFocused(nameBox);
            nameBox.setFocused(true);
        } else {
            nameBox.setFocused(false);
            cmdBox.setFocused(false);
        }
    }

    private static String onOff(boolean b) {
        return Lang.t(b ? "on" : "off");
    }

    /** Baut die Buttons für den aktuellen Tab neu auf. */
    private void rebuild() {
        buttons.clear();
        CustomGuiConfig cfg = CustomGuiConfig.get();

        switch (tab) {
            case HOME -> buildHome(cfg);
            case ISLANDS -> buildIslands(cfg);
            case MARKT -> buildMarkt(cfg);
            case TOOLS -> buildTools(cfg);
            case SETTINGS -> buildSettings(cfg);
        }
    }

    private void buildHome(CustomGuiConfig cfg) {
        int n = (cfg.showSpawn ? 1 : 0) + (cfg.showShop ? 1 : 0);
        homeEmpty = n == 0;

        if (cfg.mainLayout == 1) {
            // Kacheln: eine Kachel volle Breite, zwei Kacheln nebeneinander
            int gap = 8;
            int bx = px + 20;
            int bw = n == 2 ? (PW - 40 - gap) / 2 : PW - 40;
            int x = bx;
            if (cfg.showSpawn) {
                buttons.add(new Btn(Lang.t("home.spawn"), Lang.t("tip.spawn"), () -> runCommand("spawn"))
                        .sub("/spawn").bold().at(x, py + 72, bw, 60));
                x += bw + gap;
            }
            if (cfg.showShop) {
                buttons.add(new Btn(Lang.t("home.shop"), Lang.t("tip.shop"), this::openShop)
                        .sub(Lang.t("sub.browser")).bold().at(x, py + 72, bw, 60));
            }
            int closeY = n > 0 ? py + 72 + 60 + gap : py + 72;
            buttons.add(new Btn(Lang.t("home.close"), Lang.t("tip.close"), this::onClose)
                    .fixed(RED).bold().at(bx, closeY, PW - 40, 30));
        } else {
            int bw = Math.min(220, PW - 80);
            int bx = px + (PW - bw) / 2;
            int y = py + 74;
            if (cfg.showSpawn) {
                buttons.add(new Btn(Lang.t("home.spawn"), Lang.t("tip.spawn"), () -> runCommand("spawn"))
                        .bold().at(bx, y, bw, 30));
                y += 40;
            }
            if (cfg.showShop) {
                buttons.add(new Btn(Lang.t("home.shop"), Lang.t("tip.shop"), this::openShop)
                        .bold().at(bx, y, bw, 30));
                y += 40;
            }
            buttons.add(new Btn(Lang.t("home.close"), Lang.t("tip.close"), this::onClose)
                    .fixed(RED).bold().at(bx, y, bw, 30));
        }
    }

    /** Im Normalmodus nur sichtbare Inseln, im Bearbeiten-Modus alle. */
    private List<CustomGuiConfig.Island> shownIslands() {
        List<CustomGuiConfig.Island> out = new ArrayList<>();
        for (CustomGuiConfig.Island is : CustomGuiConfig.get().islands) {
            if (editMode || !is.hidden) {
                out.add(is);
            }
        }
        return out;
    }

    private void buildIslands(CustomGuiConfig cfg) {
        if (formMode) {
            int by = py + 172;
            buttons.add(new Btn(Lang.t("form.save"), Lang.t("tip.form.save"), this::saveIsland)
                    .fixed(GREEN).bold().at(px + PW / 2 - 120, by, 116, 28));
            buttons.add(new Btn(Lang.t("form.cancel"), Lang.t("tip.form.cancel"), () -> {
                formMode = false;
                formError = "";
                updateBoxes();
                rebuild();
            }).fixed(RED).bold().at(px + PW / 2 + 4, by, 116, 28));
            return;
        }

        List<CustomGuiConfig.Island> shown = shownIslands();
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));
        int rx = px + (PW - rowW) / 2;
        int rowH = ROW_PITCH - 6;

        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= shown.size()) break;
            CustomGuiConfig.Island is = shown.get(idx);
            int ry = py + 68 + i * ROW_PITCH;
            String cmd = is.command;

            if (editMode) {
                Btn main = new Btn(is.name, Lang.t("tip.isl.editing"), () -> { })
                        .sub("/" + cmd).left().bold().at(rx, ry, rowW - 62, rowH);
                if (is.hidden) main.dim();
                buttons.add(main);
                buttons.add(new Btn(Lang.t(is.hidden ? "isl.show" : "isl.hide"),
                        Lang.t(is.hidden ? "tip.isl.show" : "tip.isl.hide"), () -> {
                    is.hidden = !is.hidden;
                    CustomGuiConfig.save();
                    rebuild();
                }).bold().at(rx + rowW - 58, ry, 28, rowH));
                buttons.add(new Btn("x", Lang.t("tip.isl.remove"), () -> {
                    CustomGuiConfig.get().islands.remove(is);
                    CustomGuiConfig.save();
                    rebuild();
                }).fixed(RED).bold().at(rx + rowW - 24, ry, 24, rowH));
            } else {
                buttons.add(new Btn(is.name, Lang.t("tip.isl.go", is.name), () -> runCommand(cmd))
                        .sub("/" + cmd).left().bold().at(rx, ry, rowW, rowH));
            }
        }

        int by = py + 68 + ROWS * ROW_PITCH + 4;
        int editW = 74;
        Btn edit = new Btn(Lang.t(editMode ? "isl.done" : "isl.edit"),
                Lang.t(editMode ? "tip.isl.done" : "tip.isl.edit"), () -> {
            editMode = !editMode;
            page = 0;
            rebuild();
        }).bold();
        edit.active = editMode;

        if (pages > 1) {
            int addW = rowW - 24 * 2 - editW - 12;
            buttons.add(new Btn("<", Lang.t("tip.page.prev"), () -> {
                page = (page - 1 + pageCount()) % pageCount();
                rebuild();
            }).bold().at(rx, by, 24, 26));
            buttons.add(new Btn(Lang.t("isl.add") + " (" + (page + 1) + "/" + pages + ")", Lang.t("tip.isl.add"),
                    this::openForm).bold().at(rx + 28, by, addW, 26));
            buttons.add(edit.at(rx + 28 + addW + 4, by, editW, 26));
            buttons.add(new Btn(">", Lang.t("tip.page.next"), () -> {
                page = (page + 1) % pageCount();
                rebuild();
            }).bold().at(rx + rowW - 24, by, 24, 26));
        } else {
            buttons.add(new Btn(Lang.t("isl.add"), Lang.t("tip.isl.add"), this::openForm)
                    .bold().at(rx, by, rowW - editW - 4, 26));
            buttons.add(edit.at(rx + rowW - editW, by, editW, 26));
        }
    }

    /** Markt-Reiter: Marktstand-Fenster (eine Spalte) und Rübenpreise. */
    private void buildMarkt(CustomGuiConfig cfg) {
        int kx = px + 20;
        int kw = PW - 40;

        buttons.add(new Btn(Lang.t("mk.hud"), Lang.t("tip.mk.hud"), () -> {
            cfg.hudEnabled = !cfg.hudEnabled;
            changed();
        }).value(onOff(cfg.hudEnabled), cfg.hudEnabled ? 1 : 2).at(kx, py + 74, kw, 22));
        buttons.add(new Btn(Lang.t("mk.size"), Lang.t("tip.mk.size"), () -> {
            int next = CustomGuiConfig.HUD_SCALES[0];
            for (int v : CustomGuiConfig.HUD_SCALES) {
                if (v > cfg.hudScale) {
                    next = v;
                    break;
                }
            }
            cfg.hudScale = next;
            changed();
        }).value(cfg.hudScale + "%", 0).at(kx, py + 100, kw, 22));
        buttons.add(new Btn(Lang.t("mk.storage"), Lang.t("tip.mk.storage"), () -> {
            cfg.showStorage = !cfg.showStorage;
            changed();
        }).value(onOff(cfg.showStorage), cfg.showStorage ? 1 : 2).at(kx, py + 126, kw, 22));
        buttons.add(new Btn(Lang.t("mk.move"), Lang.t("tip.mk.move"),
                () -> Minecraft.getInstance().gui.setScreen(new HudMoveScreen())).at(kx, py + 152, kw, 22));

        buttons.add(new Btn(Lang.t("mk.turn"), Lang.t("tip.mk.turn"),
                () -> Minecraft.getInstance().gui.setScreen(new TurnipScreen())).bold().at(kx, py + 198, kw, 22));
    }

    /** Tools-Reiter: Haltbarkeits-Warnung und Pflanzen-Hinweis. */
    private void buildTools(CustomGuiConfig cfg) {
        int kx = px + 20;
        int kw = PW - 40;

        buttons.add(new Btn(Lang.t("tool.dur"), Lang.t("tip.tool.dur"), () -> {
            cfg.durabilityWarn = !cfg.durabilityWarn;
            changed();
        }).value(onOff(cfg.durabilityWarn), cfg.durabilityWarn ? 1 : 2).at(kx, py + 74, kw, 22));
        buttons.add(new Btn(Lang.t("tool.pct"), Lang.t("tip.tool.pct"), () -> {
            int next = CustomGuiConfig.DURABILITY_STEPS[0];
            for (int v : CustomGuiConfig.DURABILITY_STEPS) {
                if (v > cfg.durabilityPercent) {
                    next = v;
                    break;
                }
            }
            cfg.durabilityPercent = next;
            changed();
        }).value(cfg.durabilityPercent + "%", 0).at(kx, py + 100, kw, 22));

        buttons.add(new Btn(Lang.t("tool.crop"), Lang.t("tip.tool.crop"), () -> {
            cfg.cropAlert = !cfg.cropAlert;
            changed();
        }).value(onOff(cfg.cropAlert), cfg.cropAlert ? 1 : 2).at(kx, py + 146, kw, 22));
    }

    /** Einstellungs-Reiter: Taste, Darstellung und was sichtbar ist. */
    private void buildSettings(CustomGuiConfig cfg) {
        int kx = px + 20;
        int kw = PW - 40 - 70;

        Btn key = new Btn(Lang.t("set.key"), Lang.t("tip.set.key"), () -> {
            listening = !listening;
            rebuild();
        }).value(listening ? Lang.t("set.key.listen") : keyName(), listening ? 1 : 0);
        key.active = listening;
        buttons.add(key.at(kx, py + 74, kw, 22));

        buttons.add(new Btn(Lang.t("set.key.reset"), Lang.t("tip.set.key.reset"), this::resetKey)
                .at(kx + kw + 6, py + 74, 64, 22));

        List<Btn> look = new ArrayList<>();
        look.add(new Btn(Lang.t("set.lang"), Lang.t("tip.set.lang"), () -> {
            cfg.language = cfg.language == Lang.DE ? Lang.EN : Lang.DE;
            CustomGuiConfig.save();
            applyLayout();
        }).value(Lang.t("lang.name"), 0));
        look.add(new Btn(Lang.t("set.accent"), Lang.t("tip.set.accent"), () -> {
            cfg.accent = (cfg.accent + 1) % CustomGuiConfig.ACCENTS.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.ACCENT_KEYS[cfg.accent]), 0));
        look.add(new Btn(Lang.t("set.layout"), Lang.t("tip.set.layout"), () -> {
            cfg.mainLayout = (cfg.mainLayout + 1) % CustomGuiConfig.LAYOUT_KEYS.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.LAYOUT_KEYS[cfg.mainLayout]), 0));
        look.add(new Btn(Lang.t("set.anim"), Lang.t("tip.set.anim"), () -> {
            cfg.animations = !cfg.animations;
            changed();
        }).value(onOff(cfg.animations), cfg.animations ? 1 : 2));
        look.add(new Btn(Lang.t("tool.debug"), Lang.t("tip.tool.debug"), () -> {
            cfg.debugDump = !cfg.debugDump;
            changed();
        }).value(onOff(cfg.debugDump), cfg.debugDump ? 1 : 2));
        addGrid(look, py + 116);

        List<Btn> show = new ArrayList<>();
        show.add(new Btn(Lang.t("set.showSpawn"), Lang.t("tip.set.showSpawn"), () -> {
            cfg.showSpawn = !cfg.showSpawn;
            changed();
        }).value(onOff(cfg.showSpawn), cfg.showSpawn ? 1 : 2));
        show.add(new Btn(Lang.t("set.showShop"), Lang.t("tip.set.showShop"), () -> {
            cfg.showShop = !cfg.showShop;
            changed();
        }).value(onOff(cfg.showShop), cfg.showShop ? 1 : 2));
        show.add(new Btn(Lang.t("set.showIslands"), Lang.t("tip.set.showIslands"), () -> {
            cfg.showIslandsTab = !cfg.showIslandsTab;
            CustomGuiConfig.save();
            applyLayout();
        }).value(onOff(cfg.showIslandsTab), cfg.showIslandsTab ? 1 : 2));
        show.add(new Btn(Lang.t("set.showHint"), Lang.t("tip.set.showHint"), () -> {
            cfg.showHint = !cfg.showHint;
            changed();
        }).value(onOff(cfg.showHint), cfg.showHint ? 1 : 2));
        addGrid(show, py + 184);

        buttons.add(new Btn(Lang.t("set.reset"), Lang.t("tip.set.reset"), () -> {
            cfg.resetLayout();
            page = 0;
            CustomGuiConfig.save();
            applyLayout();
        }).fixed(RED).at(kx, py + 244, PW - 40, 22));
    }

    /** Ordnet Buttons in zwei Spalten an. */
    private void addGrid(List<Btn> grid, int gy) {
        int kx = px + 20;
        int gap = 6;
        int gw = (PW - 40 - gap) / 2;
        for (int i = 0; i < grid.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            buttons.add(grid.get(i).at(kx + col * (gw + gap), gy + row * 26, gw, 22));
        }
    }

    /** Speichert die Konfiguration und baut die Buttons mit den neuen Werten neu auf. */
    private void changed() {
        CustomGuiConfig.save();
        rebuild();
    }

    private int pageCount() {
        int n = shownIslands().size();
        return Math.max(1, (n + ROWS - 1) / ROWS);
    }

    private void openForm() {
        formMode = true;
        formError = "";
        updateBoxes();
        rebuild();
    }

    private void saveIsland() {
        String name = formName.trim();
        String cmd = formCmd.trim();
        while (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }
        if (name.isEmpty()) {
            formError = Lang.t("form.err.name");
            return;
        }
        if (cmd.isEmpty()) {
            formError = Lang.t("form.err.cmd");
            return;
        }
        CustomGuiConfig.get().islands.add(new CustomGuiConfig.Island(name, cmd));
        CustomGuiConfig.save();

        formName = "";
        formCmd = "";
        nameBox.setValue("");
        cmdBox.setValue("");
        formError = "";
        formMode = false;
        page = pageCount() - 1;
        updateBoxes();
        rebuild();
    }

    // ------------------------------------------------------------------ Taste

    private static String keyName() {
        return CustomGuiMod.OPEN_KEY.getTranslatedKeyMessage().getString();
    }

    private void bindKey(InputConstants.Key key) {
        CustomGuiMod.OPEN_KEY.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = false;
        rebuild();
    }

    private void resetKey() {
        KeyMapping k = CustomGuiMod.OPEN_KEY;
        bindKey(k.getDefaultKey());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int code = event.key();
        if (code == InputConstants.KEY_LSHIFT || code == InputConstants.KEY_RSHIFT) {
            shiftDown = true;
        }
        if (listening) {
            if (code == InputConstants.KEY_ESCAPE) {
                listening = false;
                rebuild();
            } else {
                bindKey(InputConstants.Type.KEYBOARD.getOrCreate(code));
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int code = event.key();
        if (code == InputConstants.KEY_LSHIFT || code == InputConstants.KEY_RSHIFT) {
            shiftDown = false;
        }
        return super.keyReleased(event);
    }

    // ------------------------------------------------------------------ Aktionen

    private void runCommand(String command) {
        String cmd = command;
        while (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand(cmd);
        }
        this.onClose();
    }

    private void openShop() {
        try {
            openInBrowser(CustomGuiConfig.get().shopUrl);
        } catch (Exception e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal(Lang.t("err.shop", e.getMessage())).withStyle(ChatFormatting.RED));
            }
        }
        this.onClose();
    }

    /** Öffnet einen http(s)-Link im Standard-Browser (nur mit Java-Mitteln, ohne Minecraft-API). */
    private static void openInBrowser(String url) throws Exception {
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(Lang.t("err.http"));
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString());
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", uri.toString());
        } else {
            pb = new ProcessBuilder("xdg-open", uri.toString());
        }
        pb.start();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ Rendern

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gfx, mouseX, mouseY, delta); // Hintergrund

        CustomGuiConfig cfg = CustomGuiConfig.get();
        boolean anim = cfg.animations;
        int accentC = 0xFF000000 | CustomGuiConfig.ACCENTS[cfg.accent];

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        float time = (now - openedAt) / 1_000_000_000f;

        float open = 1f;
        int slide = 0;
        if (anim) {
            open = Math.min(1f, time / 0.25f);
            open = 1f - (float) Math.pow(1f - open, 3);
            slide = Math.round((1f - open) * 12f);
        }
        boolean interactive = open > 0.95f;

        // ---- Panel: flach, mit dünner Akzentlinie unter dem Kopf
        int x1 = px, y1 = py + slide, x2 = px + PW, y2 = py + ph + slide;
        gfx.fill(x1, y1, x2, y2, withAlpha(BG, open));
        frame(gfx, x1, y1, x2, y2, withAlpha(BORDER, open));
        gfx.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 28, withAlpha(HEADER, open));
        gfx.fill(x1 + 1, y1 + 28, x2 - 1, y1 + 29, withAlpha((accentC & 0x00FFFFFF) | 0xCC000000, open));

        int tx = x1 + 14;
        int ty = y1 + 10;
        Component t1 = Component.literal("Cytooxien").withStyle(ChatFormatting.BOLD);
        gfx.text(this.font, t1, tx, ty, withAlpha(TEXT, open));
        tx += this.font.width(t1);
        Component t2 = Component.literal(" \u00BB ").withStyle(ChatFormatting.BOLD);
        gfx.text(this.font, t2, tx, ty, withAlpha(accentC, open));
        tx += this.font.width(t2);
        gfx.text(this.font, Component.literal(Lang.t("title.menu")), tx, ty, withAlpha(MUTED, open));
        Component url = Component.literal("cytooxien.net");
        gfx.text(this.font, url, x2 - 14 - this.font.width(url), ty, withAlpha(DIM, open));

        String hoverTip = drawTabs(gfx, mouseX, mouseY - slide, dt, open, slide, interactive, anim, accentC);

        // ---- Inhalt je Tab
        if (tab == Tab.HOME) {
            if (homeEmpty) {
                drawCentered(gfx, Lang.t("home.empty1"), py + 74 + slide, withAlpha(MUTED, open));
                drawCentered(gfx, Lang.t("home.empty2"), py + 88 + slide, withAlpha(DIM, open));
            }
        } else if (tab == Tab.MARKT) {
            drawLabel(gfx, Lang.t("mk.sec"), px + 20, py + 62 + slide, open);
            drawLabel(gfx, Lang.t("mk.turn.sec"), px + 20, py + 186 + slide, open);
            int dy = py + 226 + slide;
            for (String l : wrap(Lang.t("mk.turn.desc"), PW - 40)) {
                gfx.text(this.font, Component.literal(l), px + 20, dy, withAlpha(DIM, open));
                dy += 11;
            }
        } else if (tab == Tab.TOOLS) {
            drawLabel(gfx, Lang.t("tool.dur.sec"), px + 20, py + 62 + slide, open);
            drawLabel(gfx, Lang.t("tool.crop.sec"), px + 20, py + 134 + slide, open);
            int dy = py + 174 + slide;
            for (String l : wrap(Lang.t("tool.crop.desc"), PW - 40)) {
                gfx.text(this.font, Component.literal(l), px + 20, dy, withAlpha(DIM, open));
                dy += 11;
            }
        } else if (tab == Tab.SETTINGS) {
            drawLabel(gfx, Lang.t("set.keysec"), px + 20, py + 62 + slide, open);
            drawLabel(gfx, Lang.t("set.looksec"), px + 20, py + 104 + slide, open);
            drawLabel(gfx, Lang.t("set.showsec"), px + 20, py + 172 + slide, open);
        } else if (tab == Tab.ISLANDS) {
            if (formMode) {
                drawLabel(gfx, Lang.t("form.name"), px + 30, py + 70 + slide, open);
                drawLabel(gfx, Lang.t("form.cmd"), px + 30, py + 116 + slide, open);
                nameBox.extractRenderState(gfx, mouseX, mouseY, delta);
                cmdBox.extractRenderState(gfx, mouseX, mouseY, delta);
                if (!formError.isEmpty()) {
                    Component err = Component.literal(formError).withStyle(ChatFormatting.RED);
                    gfx.text(this.font, err, this.width / 2 - this.font.width(err) / 2,
                            py + 156 + slide, withAlpha(0xFFFFFFFF, open));
                }
            } else if (cfg.islands.isEmpty()) {
                drawCentered(gfx, Lang.t("isl.empty1"), py + 110 + slide, withAlpha(MUTED, open));
                drawCentered(gfx, Lang.t("isl.empty2"), py + 124 + slide, withAlpha(DIM, open));
            } else if (shownIslands().isEmpty()) {
                drawCentered(gfx, Lang.t("isl.allhidden1"), py + 110 + slide, withAlpha(MUTED, open));
                drawCentered(gfx, Lang.t("isl.allhidden2"), py + 124 + slide, withAlpha(DIM, open));
            }
        }

        // ---- Buttons
        for (Btn b : buttons) {
            boolean hovered = interactive && b.contains(mouseX, mouseY - slide);
            if (hovered) hoverTip = b.tip;
            if (hovered && !b.wasHovered) playUi(1.8f);
            b.wasHovered = hovered;
            float target = hovered ? 1f : 0f;
            b.hover = anim ? b.hover + (target - b.hover) * (1f - (float) Math.exp(-dt * 14f)) : target;
            drawButton(gfx, b, slide, open, accentC);
        }

        // ---- Fußzeile: Hinweis auf Shift-Erklärungen (ausblendbar)
        if (cfg.showHint) {
            drawCentered(gfx, Lang.t("hint.shift"), py + ph - 14 + slide, withAlpha(DIM, open));
        }

        // ---- Tooltip mit Erklärung (nur mit gedrückter Shift-Taste)
        if (shiftDown && hoverTip != null && !hoverTip.isEmpty()) {
            drawTooltip(gfx, mouseX, mouseY, hoverTip, accentC);
        }
    }

    /** Zeichnet die Tabs und liefert die Erklärung des Tabs unter der Maus (oder null). */
    private String drawTabs(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float dt, float open,
                            int slide, boolean interactive, boolean anim, int accentC) {
        int tabH = 20;
        int ty = py + 34 + slide;
        String tip = null;

        int selected = Math.max(0, tabs.indexOf(tab));
        float targetX = tabX[selected];
        float targetW = tabW[selected];
        if (indicatorX < 0 || !anim) {
            indicatorX = targetX;
            indicatorW = targetW;
        }
        float k = 1f - (float) Math.exp(-dt * 12f);
        indicatorX += (targetX - indicatorX) * k;
        indicatorW += (targetW - indicatorW) * k;

        for (int i = 0; i < tabs.size(); i++) {
            int tx = tabX[i];
            int tw = tabW[i];
            boolean hovered = interactive && mouseX >= tx && mouseX < tx + tw
                    && mouseY >= py + 34 && mouseY < py + 34 + tabH;
            if (hovered) tip = Lang.t(tabs.get(i).tipKey);
            if (hovered && !tabWasHovered[i] && i != selected) playUi(1.8f);
            tabWasHovered[i] = hovered;
            float target = hovered ? 1f : 0f;
            tabHover[i] = anim ? tabHover[i] + (target - tabHover[i]) * (1f - (float) Math.exp(-dt * 14f)) : target;

            int base = i == selected ? TEXT : MUTED;
            int col = lerpColor(base, TEXT, tabHover[i]);
            Component c = Component.literal(Lang.t(tabs.get(i).key)).withStyle(ChatFormatting.BOLD);
            gfx.text(this.font, c, tx + tw / 2 - this.font.width(c) / 2,
                    ty + tabH / 2 - this.font.lineHeight / 2, withAlpha(col, open));
        }
        int ux = Math.round(indicatorX);
        gfx.fill(ux + 6, ty + tabH + 1, ux + Math.round(indicatorW) - 6, ty + tabH + 3, withAlpha(accentC, open));
        return tip;
    }

    /** Dünner 1-Pixel-Rahmen (lässt das Innere durchscheinen). */
    private static void frame(GuiGraphicsExtractor gfx, int x1, int y1, int x2, int y2, int c) {
        gfx.fill(x1, y1, x2, y1 + 1, c);
        gfx.fill(x1, y2 - 1, x2, y2, c);
        gfx.fill(x1, y1 + 1, x1 + 1, y2 - 1, c);
        gfx.fill(x2 - 1, y1 + 1, x2, y2 - 1, c);
    }

    private void drawLabel(GuiGraphicsExtractor gfx, String text, int x, int y, float open) {
        gfx.text(this.font, Component.literal(text), x, y, withAlpha(MUTED, open));
        int lx = x + this.font.width(Component.literal(text)) + 6;
        int rx = px + PW - 20;
        if (rx > lx) {
            gfx.fill(lx, y + 4, rx, y + 5, withAlpha(BORDER, open));
        }
    }

    private void drawCentered(GuiGraphicsExtractor gfx, String text, int y, int color) {
        Component c = Component.literal(text);
        gfx.text(this.font, c, this.width / 2 - this.font.width(c) / 2, y, color);
    }

    private void drawButton(GuiGraphicsExtractor gfx, Btn b, int slide, float open, int themeAccent) {
        float h = b.active ? 1f : b.hover;
        int x1 = b.x;
        int y1 = b.y + slide;
        int x2 = b.x + b.w;
        int y2 = b.y + slide + b.h;
        int accent = b.fixed != 0 ? (0xFF000000 | b.fixed) : themeAccent;

        int hoverBg = (0x78 << 24) | (mixWithBlack(accent, 0.5f) & 0x00FFFFFF);
        int border = lerpColor(BTN_BORDER, accent, h);
        int bg = lerpColor(BTN, hoverBg, h);
        gfx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, withAlpha(bg, open));
        frame(gfx, x1, y1, x2, y2, withAlpha(border, open));

        int textColor = b.dim ? lerpColor(DIM, 0xFFC9D0DE, h) : lerpColor(0xFFC9D0DE, 0xFFFFFFFF, h);
        Component label = b.bold ? Component.literal(b.label).withStyle(ChatFormatting.BOLD) : Component.literal(b.label);
        boolean hasSub = !b.sub.isEmpty();
        int midY = y1 + (b.h - this.font.lineHeight) / 2 + 1;

        if (b.left) {
            int ny = y1 + (b.h >= 30 ? 5 : 3);
            int sy = y1 + (b.h >= 30 ? 17 : 14);
            gfx.text(this.font, label, x1 + 10, ny, withAlpha(textColor, open));
            if (hasSub) {
                gfx.text(this.font, Component.literal(b.sub), x1 + 10, sy, withAlpha(b.dim ? DIM : MUTED, open));
            }
        } else if (hasSub) {
            // Kachel: Titel mittig, darunter kleine Zeile
            int cy = (y1 + y2) / 2;
            gfx.text(this.font, label, (x1 + x2) / 2 - this.font.width(label) / 2, cy - this.font.lineHeight, withAlpha(textColor, open));
            Component sub = Component.literal(b.sub);
            gfx.text(this.font, sub, (x1 + x2) / 2 - this.font.width(sub) / 2, cy + 3, withAlpha(MUTED, open));
        } else if (!b.value.isEmpty()) {
            // Beschriftung links, Wert rechts
            gfx.text(this.font, label, x1 + 8, midY, withAlpha(textColor, open));
            int vc = b.valueMode == 1 ? accent : (b.valueMode == 2 ? DIM : TEXT);
            Component val = Component.literal(b.value);
            gfx.text(this.font, val, x2 - 8 - this.font.width(val), midY, withAlpha(vc, open));
        } else {
            gfx.text(this.font, label, (x1 + x2) / 2 - this.font.width(label) / 2, midY, withAlpha(textColor, open));
        }
    }

    /** Erklärungs-Tooltip neben dem Mauszeiger. */
    private void drawTooltip(GuiGraphicsExtractor gfx, int mouseX, int mouseY, String text, int accent) {
        List<String> lines = wrap(text, 190);
        int textW = 0;
        for (String l : lines) {
            textW = Math.max(textW, this.font.width(l));
        }
        int lh = this.font.lineHeight + 2;
        int w = textW + 14;
        int h = lines.size() * lh + 8;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + w > this.width - 4) x = mouseX - w - 8;
        if (y + h > this.height - 4) y = this.height - h - 4;
        x = Math.max(4, x);
        y = Math.max(4, y);

        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, accent);
        gfx.fill(x, y, x + w, y + h, 0xFF0A0F1A);
        int ly = y + 5;
        for (String l : lines) {
            gfx.text(this.font, Component.literal(l), x + 7, ly, TEXT);
            ly += lh;
        }
    }

    private List<String> wrap(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String attempt = cur.length() == 0 ? word : cur + " " + word;
            if (this.font.width(attempt) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(attempt);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Beim Tastenwechsel zählt jede Maustaste (links, rechts, Mitte ...) als neue Taste
        if (listening) {
            bindKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true; // Klick auf ein Textfeld
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            // Tabs
            int tabH = 20;
            for (int i = 0; i < tabs.size(); i++) {
                if (event.x() >= tabX[i] && event.x() < tabX[i] + tabW[i]
                        && event.y() >= py + 34 && event.y() < py + 34 + tabH) {
                    Tab target = tabs.get(i);
                    if (target != tab) {
                        tab = target;
                        formMode = false;
                        editMode = false;
                        formError = "";
                        page = 0;
                        applyLayout();
                        playUi(1.0f);
                    }
                    return true;
                }
            }
            // Buttons
            for (Btn b : buttons) {
                if (b.contains(event.x(), event.y())) {
                    playUi(1.0f);
                    b.action.run();
                    return true;
                }
            }
        }
        return false;
    }

    private void playUi(float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // ------------------------------------------------------------------ Helfer

    private static int withAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerp((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
        int r = lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = lerp((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = lerp(c1 & 0xFF, c2 & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static int mixWithBlack(int argb, float keep) {
        int r = Math.round(((argb >> 16) & 0xFF) * keep);
        int g = Math.round(((argb >> 8) & 0xFF) * keep);
        int b = Math.round((argb & 0xFF) * keep);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
