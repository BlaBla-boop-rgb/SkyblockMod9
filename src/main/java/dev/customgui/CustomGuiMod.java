package dev.customgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class CustomGuiMod implements ClientModInitializer {

    /** Taste zum Öffnen des Menüs. Standard: H. Umbelegbar im Einstellungs-Tab des Menüs
     *  oder unter Optionen > Steuerung > Tastenbelegung. */
    public static KeyMapping OPEN_KEY;

    @Override
    public void onInitializeClient() {
        CustomGuiConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("customgui", "main"));

        OPEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.customgui.open",
                InputConstants.KEY_H,
                category));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("customgui", "marktstand"), MarktstandHud::render);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("customgui", "durability"), DurabilityWarning::render);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("customgui", "cropalert"), CropAlert::render);
        ClientTickEvents.END_CLIENT_TICK.register(DebugDump::tick);
        ClientTickEvents.END_CLIENT_TICK.register(MarktstandTracker::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ItemStorage::tick);
        ClientTickEvents.END_CLIENT_TICK.register(DurabilityWarning::tick);
        ClientTickEvents.END_CLIENT_TICK.register(CropAlert::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TurnipPrices::tick);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_KEY.consumeClick()) {
                if (mc.gui.screen() == null && mc.player != null) {
                    mc.gui.setScreen(new CustomGuiScreen());
                }
            }
        });
    }
}
