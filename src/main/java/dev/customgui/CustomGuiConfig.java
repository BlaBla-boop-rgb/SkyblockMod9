package dev.customgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Speichert Shop-Link, Inseln und Anzeige-Einstellungen in config/customgui.json. */
public final class CustomGuiConfig {

    public static class Island {
        public String name = "";
        public String command = "";
        /** Ausgeblendete Inseln erscheinen nur im Bearbeiten-Modus. */
        public boolean hidden = false;

        public Island() {
        }

        public Island(String name, String command) {
            this.name = name;
            this.command = command;
        }
    }

    // ---- Auswahlmöglichkeiten (die Namen sind Schlüssel für Lang.t)
    public static final String[] ACCENT_KEYS = {"accent.cyan", "accent.green", "accent.purple", "accent.gold", "accent.pink", "accent.white"};
    public static final int[] ACCENTS = {0x3CC8F0, 0x55FF55, 0xB388FF, 0xFFC107, 0xFF5E7E, 0xFFFFFF};
    public static final String[] LAYOUT_KEYS = {"layout.list", "layout.tiles"};

    /** Dieser Link wird beim Klick auf "Shop" im Standard-Browser geöffnet. */
    public String shopUrl = "https://www.cytooxien.de/de";
    public List<Island> islands = new ArrayList<>();

    /** 0 = Deutsch, 1 = English. */
    public int language = 0;
    /** Index in ACCENTS. */
    public int accent = 0;
    /** 0 = Buttons untereinander, 1 = Kacheln nebeneinander. */
    public int mainLayout = 0;
    public boolean animations = true;

    // ---- Was im Menü sichtbar ist
    public boolean showSpawn = true;
    public boolean showShop = true;
    public boolean showIslandsTab = true;
    public boolean showHint = true;

    // ---- Marktstand-HUD (hudX = -1: rechts oben)
    public boolean hudEnabled = true;
    public int hudX = -1;
    public int hudY = 6;
    /** Größe des Marktstand-Fensters in Prozent (50 bis 250). */
    public int hudScale = 100;
    /** Zeigt im Marktstand-Fenster, wie viel im Itemlager liegt (wird beim Öffnen des Lagers gemerkt). */
    public boolean showStorage = true;
    /** Teil des Titels, an dem das Itemlager erkannt wird (Kleinschreibung egal). */
    public String storageKeyword = "lager";

    // ---- Haltbarkeits-Warnung
    public boolean durabilityWarn = true;
    /** Warnung, sobald nur noch so viel Prozent Haltbarkeit übrig sind. */
    public int durabilityPercent = 10;

    // ---- Ausrufezeichen über ausgewachsenen Pflanzen
    public boolean cropAlert = true;

    /** Schreibt die Inhalte geöffneter Menüs nach config/customgui_debug.txt (zum Anpassen der Erkennung). */
    public boolean debugDump = false;

    public static final int[] HUD_SCALES = {60, 80, 100, 120, 150, 200};
    public static final int[] DURABILITY_STEPS = {5, 10, 15, 20, 25};


    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CustomGuiConfig instance;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("customgui.json");
    }

    public static CustomGuiConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path p = path();
        CustomGuiConfig loaded = null;
        try {
            if (Files.exists(p)) {
                loaded = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), CustomGuiConfig.class);
            }
        } catch (Exception e) {
            System.err.println("[CustomGui] Konfiguration konnte nicht gelesen werden: " + e);
        }
        if (loaded == null) {
            loaded = new CustomGuiConfig();
        }
        if (loaded.islands == null) {
            loaded.islands = new ArrayList<>();
        }
        if (loaded.shopUrl == null || loaded.shopUrl.isBlank()) {
            loaded.shopUrl = "https://www.cytooxien.de/de";
        }
        loaded.sanitize();
        instance = loaded;
        save();
    }

    /** Fängt ungültige Werte ab (z. B. von Hand falsch in der JSON-Datei eingetragen). */
    private void sanitize() {
        language = clamp(language, 0, 1);
        accent = clamp(accent, 0, ACCENTS.length - 1);
        mainLayout = clamp(mainLayout, 0, LAYOUT_KEYS.length - 1);
        hudScale = clamp(hudScale, 50, 250);
        durabilityPercent = clamp(durabilityPercent, 1, 90);
        if (storageKeyword == null || storageKeyword.isBlank()) {
            storageKeyword = "lager";
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Setzt Design und Sichtbarkeit zurück (Inseln, Shop-Link und Taste bleiben erhalten). */
    public void resetLayout() {
        accent = 0;
        mainLayout = 0;
        animations = true;
        showSpawn = true;
        showShop = true;
        showIslandsTab = true;
        showHint = true;
    }

    public static void save() {
        if (instance == null) return;
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[CustomGui] Konfiguration konnte nicht gespeichert werden: " + e);
        }
    }
}
