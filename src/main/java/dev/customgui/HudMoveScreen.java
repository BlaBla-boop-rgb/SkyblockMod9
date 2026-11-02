package dev.customgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Vollbild zum Verschieben des Marktstand-Fensters: ziehen, loslassen = gespeichert. */
public class HudMoveScreen extends Screen {

    private int w, h, baseW, baseH, curX, curY;
    private int offX, offY;
    private boolean dragging = false;

    public HudMoveScreen() {
        super(Component.literal("HUD"));
    }

    @Override
    protected void init() {
        layoutBox();
    }

    private void layoutBox() {
        List<MarktstandTracker.Entry> list = MarktstandHud.shown(true);
        int[] s = MarktstandHud.size(this.font, list, true);
        baseW = s[0];
        baseH = s[1];
        w = Math.round(baseW * MarktstandHud.scale());
        h = Math.round(baseH * MarktstandHud.scale());
        int[] p = MarktstandHud.position(Minecraft.getInstance(), w, h);
        curX = p[0];
        curY = p[1];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gfx, mouseX, mouseY, delta); // Hintergrund

        CustomGuiConfig cfg = CustomGuiConfig.get();
        int accent = CustomGuiConfig.ACCENTS[cfg.accent];
        MarktstandHud.draw(gfx, this.font, curX, curY, MarktstandHud.shown(true), baseW, baseH, accent, true);

        // Rahmen in Akzentfarbe, damit klar ist, dass man das Fenster greifen kann
        int c = 0xFF000000 | accent;
        gfx.fill(curX - 1, curY - 1, curX + w + 1, curY, c);
        gfx.fill(curX - 1, curY + h, curX + w + 1, curY + h + 1, c);
        gfx.fill(curX - 1, curY, curX, curY + h, c);
        gfx.fill(curX + w, curY, curX + w + 1, curY + h, c);

        Component hint = Component.literal(Lang.t("move.hint"));
        gfx.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, this.height / 2 - 4, 0xFFE6EAF2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            CustomGuiConfig cfg = CustomGuiConfig.get();
            cfg.hudX = -1;
            cfg.hudY = 6;
            CustomGuiConfig.save();
            layoutBox();
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= curX && event.x() < curX + w
                && event.y() >= curY && event.y() < curY + h) {
            dragging = true;
            offX = (int) event.x() - curX;
            offY = (int) event.y() - curY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            curX = Math.max(0, Math.min(Math.max(0, this.width - w), (int) event.x() - offX));
            curY = Math.max(0, Math.min(Math.max(0, this.height - h), (int) event.y() - offY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            CustomGuiConfig cfg = CustomGuiConfig.get();
            cfg.hudX = curX;
            cfg.hudY = curY;
            CustomGuiConfig.save();
            return true;
        }
        return super.mouseReleased(event);
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
