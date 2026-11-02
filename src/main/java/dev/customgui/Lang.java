package dev.customgui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Einfache Übersetzung für das Menü (Deutsch / Englisch), unabhängig von der Minecraft-Sprache. */
public final class Lang {

    public static final int DE = 0;
    public static final int EN = 1;

    private static final Map<String, String[]> T = new HashMap<>();

    private static void a(String key, String de, String en) {
        T.put(key, new String[]{de, en});
    }

    static {
        a("title.menu", "Skyblock", "Skyblock");
        a("hint.shift", "Shift halten + Maus \u00FCber ein Element = Erkl\u00E4rung", "Hold Shift + hover = explanation");
        a("on", "An", "On");
        a("off", "Aus", "Off");

        // Tabs
        a("tab.home", "Start", "Home");
        a("tab.islands", "Inseln", "Islands");
        a("tab.settings", "Einstellungen", "Settings");
        a("tip.tab.home", "Schnellzugriff: Spawn und Shop.", "Quick access: spawn and shop.");
        a("tip.tab.islands", "Deine gespeicherten Inseln und Warps mit dem Befehl, der dich hinbringt.", "Your saved islands and warps with the command that takes you there.");
        a("tip.tab.settings", "Taste, Design und was im Men\u00FC sichtbar ist.", "Key, design and what is visible in the menu.");

        // Start
        a("home.spawn", "Spawn", "Spawn");
        a("tip.spawn", "F\u00FChrt /spawn aus und bringt dich zum Skyblock-Spawn.", "Runs /spawn and takes you to the Skyblock spawn.");
        a("home.shop", "Shop", "Shop");
        a("sub.browser", "Im Browser", "In browser");
        a("tip.shop", "\u00D6ffnet den Cytooxien-Shop im Standard-Browser.", "Opens the Cytooxien shop in your default browser.");
        a("home.close", "Schlie\u00DFen", "Close");
        a("tip.close", "Schlie\u00DFt dieses Men\u00FC.", "Closes this menu.");
        a("home.empty1", "Alles ausgeblendet.", "Everything is hidden.");
        a("home.empty2", "Unter Einstellungen > Anzeigen wieder einblenden.", "Show it again under Settings > Display.");

        // Inseln
        a("isl.empty1", "Noch keine Inseln gespeichert.", "No islands saved yet.");
        a("isl.empty2", "F\u00FCge deine erste Insel hinzu!", "Add your first island!");
        a("isl.allhidden1", "Alle Inseln sind ausgeblendet.", "All islands are hidden.");
        a("isl.allhidden2", "Mit \u201EBearbeiten\u201C kannst du sie wieder einblenden.", "Use \"Edit\" to show them again.");
        a("isl.add", "+ Hinzuf\u00FCgen", "+ Add");
        a("tip.isl.add", "Speichert eine neue Insel mit dem Befehl, der dich dorthin bringt.", "Saves a new island together with the command that takes you there.");
        a("isl.edit", "Bearbeiten", "Edit");
        a("isl.done", "Fertig", "Done");
        a("tip.isl.edit", "Zeigt Buttons zum Ausblenden und L\u00F6schen. Ausgeblendete Inseln sind sonst nicht sichtbar.", "Shows buttons to hide and delete. Hidden islands are otherwise not shown.");
        a("tip.isl.done", "Beendet den Bearbeiten-Modus.", "Leaves edit mode.");
        a("tip.isl.go", "Klicke, um dich zu %s zu bringen.", "Click to travel to %s.");
        a("tip.isl.editing", "Bearbeiten-Modus: Zum Reisen erst auf \u201EFertig\u201C klicken.", "Edit mode: click \"Done\" first to travel.");
        a("isl.hide", "Aus", "Hide");
        a("isl.show", "An", "Show");
        a("tip.isl.hide", "Blendet diese Insel aus. Sie bleibt gespeichert.", "Hides this island. It stays saved.");
        a("tip.isl.show", "Blendet diese Insel wieder ein.", "Shows this island again.");
        a("tip.isl.remove", "L\u00F6scht diese Insel dauerhaft aus der Liste.", "Permanently deletes this island from the list.");
        a("tip.page.prev", "Vorherige Seite.", "Previous page.");
        a("tip.page.next", "N\u00E4chste Seite.", "Next page.");
        a("form.name", "Name der Insel", "Island name");
        a("form.cmd", "Befehl (zum Hinkommen)", "Command (to get there)");
        a("form.name.hint", "z. B. Markshop", "e.g. Markshop");
        a("form.cmd.hint", "z. B. /warp Markshop", "e.g. /warp Markshop");
        a("form.save", "Speichern", "Save");
        a("tip.form.save", "Speichert die Insel.", "Saves the island.");
        a("form.cancel", "Abbrechen", "Cancel");
        a("tip.form.cancel", "Zur\u00FCck zur Liste.", "Back to the list.");
        a("form.err.name", "Bitte gib einen Namen ein.", "Please enter a name.");
        a("form.err.cmd", "Bitte gib einen Befehl ein.", "Please enter a command.");
        a("err.shop", "Shop-Link konnte nicht ge\u00F6ffnet werden: %s", "Could not open the shop link: %s");
        a("err.http", "Es sind nur http(s)-Links erlaubt", "Only http(s) links are allowed");

        // Einstellungen
        a("set.keysec", "Tastenbelegung", "Key binding");
        a("set.looksec", "Darstellung", "Appearance");
        a("set.showsec", "Anzeigen", "Display");
        a("set.key", "Men\u00FC-Taste", "Menu key");
        a("set.key.listen", "Dr\u00FCcke Taste / Maus ...", "Press key / mouse ...");
        a("tip.set.key", "Klicken, dann eine Taste oder Maustaste dr\u00FCcken (links/rechts m\u00F6glich). Esc bricht ab. Achtung: Maustasten k\u00F6nnen mit Angriff/Benutzen kollidieren.",
                "Click, then press a key or mouse button (left/right possible). Esc cancels. Note: mouse buttons can clash with attack/use.");
        a("set.key.reset", "Standard", "Default");
        a("tip.set.key.reset", "Setzt die Men\u00FC-Taste auf H zur\u00FCck.", "Resets the menu key to H.");
        a("set.lang", "Sprache", "Language");
        a("lang.name", "Deutsch", "English");
        a("tip.set.lang", "Wechselt die Sprache des Men\u00FCs zwischen Deutsch und Englisch.", "Switches the menu language between German and English.");
        a("set.accent", "Akzent", "Accent");
        a("tip.set.accent", "Akzentfarbe des Men\u00FCs.", "Accent color of the menu.");
        a("accent.cyan", "Cyan", "Cyan");
        a("accent.green", "Gr\u00FCn", "Green");
        a("accent.purple", "Lila", "Purple");
        a("accent.gold", "Gold", "Gold");
        a("accent.pink", "Rosa", "Pink");
        a("accent.white", "Wei\u00DF", "White");
        a("set.layout", "Start-Tab", "Home tab");
        a("tip.set.layout", "Zeigt den Start-Tab als Liste oder als Kacheln.", "Shows the home tab as a list or as tiles.");
        a("layout.list", "Liste", "List");
        a("layout.tiles", "Kacheln", "Tiles");
        a("set.anim", "Animationen", "Animations");
        a("tip.set.anim", "Gleit- und Hover-Effekte ein- oder ausschalten.", "Turns slide and hover effects on or off.");
        a("set.showSpawn", "Spawn-Button", "Spawn button");
        a("tip.set.showSpawn", "Blendet den Spawn-Button im Start-Tab ein oder aus.", "Shows or hides the spawn button on the home tab.");
        a("set.showShop", "Shop-Button", "Shop button");
        a("tip.set.showShop", "Blendet den Shop-Button im Start-Tab ein oder aus.", "Shows or hides the shop button on the home tab.");
        a("set.showIslands", "Inseln-Tab", "Islands tab");
        a("tip.set.showIslands", "Blendet den ganzen Inseln-Tab aus. Deine gespeicherten Inseln bleiben erhalten.", "Hides the whole islands tab. Your saved islands are kept.");
        a("set.showHint", "Hinweiszeile", "Hint line");
        a("tip.set.showHint", "Die Zeile am unteren Rand, die auf die Shift-Erkl\u00E4rungen hinweist.", "The line at the bottom that points to the Shift explanations.");
        a("set.reset", "Design & Anzeige zur\u00FCcksetzen", "Reset design & display");
        a("tip.set.reset", "Setzt Akzent, Layout und Sichtbarkeit auf Standard. Inseln, Shop-Link und Taste bleiben.", "Resets accent, layout and visibility. Islands, shop link and key are kept.");

        // Extras: Marktstand-HUD und Auktions-Preis
        a("tab.markt", "Markt", "Market");
        a("tab.tools", "Tools", "Tools");
        a("tip.tab.tools", "Haltbarkeits-Warnung und Pflanzen-Hinweis.", "Durability warning and plant marker.");
        a("tool.dur.sec", "Haltbarkeit", "Durability");
        a("tool.crop.sec", "Pflanzen", "Plants");
        a("tool.crop.desc", "Gelbes Ausrufezeichen \u00FCber ausgewachsenen Pflanzen im Umkreis von 12 Bl\u00F6cken.", "Yellow exclamation mark above fully grown plants within 12 blocks.");
        a("tip.tab.markt", "Marktstand-Fenster und R\u00FCbenpreis-Verlauf.", "Market stall window and turnip price history.");
        a("mk.sec", "Marktstand", "Market stall");
        a("mk.hud", "Anzeige (HUD)", "Display (HUD)");
        a("tip.mk.hud", "Zeigt im Spiel, welche Items die Marktstand-Kunden noch brauchen. Die Liste aktualisiert sich selbst, sobald du den Marktstand \u00F6ffnest.", "Shows in game which items the market stall customers still need. The list updates itself whenever you open the market stall.");
        a("mk.move", "Position \u00E4ndern", "Change position");
        a("tip.mk.move", "\u00D6ffnet eine Ansicht, in der du das Fenster mit der Maus an eine beliebige Stelle ziehst.", "Opens a view where you drag the window anywhere with the mouse.");
        a("mk.turn.sec", "R\u00FCbenpreise", "Turnip prices");
        a("mk.turn", "\u00DCbersicht \u00F6ffnen", "Open overview");
        a("tip.mk.turn", "Zeigt den Verlauf des R\u00FCbenpreises als Diagramm (Woche, Monat, Jahr) mit Prognose.", "Shows the turnip price history as a chart (week, month, year) with a forecast.");
        a("mk.turn.desc", "Der Preis wird automatisch gespeichert, sobald du das R\u00FCbenpreis-Men\u00FC \u00F6ffnest. Einmal am Tag reicht.", "The price is saved automatically when you open the turnip price menu. Once a day is enough.");
        a("tr.title", "R\u00FCbenpreis", "Turnip price");
        a("tr.week", "Woche", "Week");
        a("tr.month", "Monat", "Month");
        a("tr.year", "Jahr", "Year");
        a("tr.today", "Heute:", "Today:");
        a("tr.yesterday", "Vortag:", "Yesterday:");
        a("tr.avg7", "\u00D8 7 Tage:", "Avg 7 days:");
        a("tr.min", "Tief:", "Low:");
        a("tr.max", "Hoch:", "High:");
        a("tr.tracked", "Tage erfasst:", "Days tracked:");
        a("tr.few", "Noch wenig Daten: Die Prognose ist sehr unsicher und wird mit jedem Tag besser.", "Not much data yet: the forecast is very uncertain and improves every day.");
        a("tr.now", "Heute", "Today");
        a("tr.tomorrow", "Morgen", "Tomorrow");
        a("tr.week1", "In 1 Woche", "In 1 week");
        a("tr.month1", "In 1 Monat", "In 1 month");
        a("tr.year1", "In 1 Jahr", "In 1 year");
        a("tr.disclaimer", "Grobe Sch\u00E4tzung aus bisherigen Preisen, keine Garantie.", "Rough estimate from past prices, no guarantee.");
        a("tr.nodata", "Noch keine Preise gespeichert. \u00D6ffne das R\u00FCbenpreis-Men\u00FC einmal.", "No prices saved yet. Open the turnip price menu once.");
        a("hud.owner", "Besitzer:", "Owner:");
        a("hud.island", "Insel:", "Island:");
        a("hud.missing", "Fehlt", "Missing");
        a("hud.inv", "Inv", "Inv");
        a("hud.done", "Abgegeben", "Delivered");
        a("hud.storage", "Lager", "Storage");
        a("mk.size", "Gr\u00F6\u00DFe", "Size");
        a("tip.mk.size", "Gr\u00F6\u00DFe des Marktstand-Fensters. Klicken wechselt zwischen 60 % und 200 %.", "Size of the market stall window. Click to cycle between 60 % and 200 %.");
        a("mk.storage", "Lager-Anzeige", "Storage display");
        a("tip.mk.storage", "Zeigt pro Item, wie viel in deinem Itemlager liegt. Wird gemerkt, sobald du das Lager \u00F6ffnest.", "Shows per item how much is in your item storage. Remembered whenever you open the storage.");
        a("tool.sec", "Werkzeuge & Pflanzen", "Tools & plants");
        a("tool.dur", "Haltbarkeit", "Durability");
        a("tip.tool.dur", "Warnt auf dem Bildschirm, wenn ein Werkzeug, eine Waffe oder R\u00FCstung fast kaputt ist.", "Warns on screen when a tool, weapon or armor is almost broken.");
        a("tool.pct", "Warnung ab", "Warn at");
        a("tip.tool.pct", "Ab wie viel Prozent Haltbarkeit gewarnt wird (5, 10, 15, 20, 25 %).", "At what durability percentage the warning appears (5, 10, 15, 20, 25 %).");
        a("tool.crop", "Pflanzen-Hinweis", "Plant marker");
        a("tip.tool.crop", "Zeigt ein gelbes Ausrufezeichen \u00FCber ausgewachsenen Pflanzen in deiner N\u00E4he (Ego-Perspektive).", "Shows a yellow exclamation mark above fully grown plants nearby (first person).");
        a("tool.debug", "Debug-Datei", "Debug file");
        a("tip.tool.debug", "Schreibt den Inhalt jedes ge\u00F6ffneten Men\u00FCs nach config/customgui_debug.txt. Hilft, die Erkennung anzupassen.", "Writes the content of every opened menu to config/customgui_debug.txt. Helps tuning the detection.");
        a("dur.title", "Haltbarkeit niedrig", "Low durability");
        a("hud.title", "Marktstand: fehlt", "Market stall: missing");
        a("hud.more", "+%s weitere", "+%s more");
        a("move.hint", "Fenster ziehen \u00B7 Rechtsklick = Standard \u00B7 Esc = fertig", "Drag the window \u00B7 Right click = default \u00B7 Esc = done");
    }

    private Lang() {
    }

    public static String t(String key) {
        String[] v = T.get(key);
        if (v == null) return key;
        int l = CustomGuiConfig.get().language;
        return v[l == EN ? EN : DE];
    }

    public static String t(String key, Object... args) {
        return String.format(Locale.ROOT, t(key), args);
    }
}
