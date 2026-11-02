package dev.customgui;

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
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hilfsmittel zum Anpassen der Erkennung: Ist "Debug" an, wird jedes geöffnete Menü (Titel, Item-Namen,
 * Lore-Zeilen) einmalig nach config/customgui_debug.txt geschrieben.
 */
public final class DebugDump {

    private static Object lastScreen;
    private static int ticks;

    private DebugDump() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("customgui_debug.txt");
    }

    public static void tick(Minecraft mc) {
        if (!CustomGuiConfig.get().debugDump) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            lastScreen = null;
            return;
        }
        if (screen != lastScreen) {
            lastScreen = screen;
            ticks = 0;
        }
        // Menü braucht einen Moment, bis der Server die Items geschickt hat
        if (++ticks != 20) return;

        StringBuilder sb = new StringBuilder();
        sb.append("==== ").append(LocalDateTime.now()).append(" ====\n");
        sb.append("Titel: ").append(screen.getTitle().getString()).append('\n');
        List<Slot> slots = screen.getMenu().slots;
        int limit = Math.max(0, slots.size() - 36);
        for (int i = 0; i < limit; i++) {
            ItemStack st = slots.get(i).getItem();
            if (st.isEmpty()) continue;
            sb.append("[").append(i).append("] ").append(st.getHoverName().getString())
                    .append(" x").append(st.getCount()).append('\n');
            ItemLore lore = st.get(DataComponents.LORE);
            if (lore != null) {
                for (Component l : lore.lines()) {
                    sb.append("      | ").append(l.getString()).append('\n');
                }
            }
        }
        sb.append('\n');
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[CustomGui] Debug-Datei konnte nicht geschrieben werden: " + e);
        }
    }
}
