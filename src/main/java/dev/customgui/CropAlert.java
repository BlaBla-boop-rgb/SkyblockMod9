package dev.customgui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeigt ein gelbes Ausrufezeichen über ausgewachsenen Pflanzen (Weizen, Karotten, Kartoffeln, Rote Bete,
 * Netherwarzen). Gesucht wird alle 10 Ticks im Umkreis von 12 Blöcken; gezeichnet wird auf dem Bildschirm
 * (in der Ego-Perspektive), ohne etwas in der Welt zu verändern.
 */
public final class CropAlert {

    private static final int RADIUS = 12;
    private static final int HEIGHT = 4;
    private static final int MAX_MARKS = 60;

    private static List<Vec3> marks = new ArrayList<>();
    private static int ticks = 0;

    private CropAlert() {
    }

    private static boolean ripe(BlockState st) {
        if (st.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(st);
        }
        if (st.getBlock() instanceof NetherWartBlock) {
            return st.getValue(NetherWartBlock.AGE) >= 3;
        }
        return false;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !CustomGuiConfig.get().cropAlert) {
            marks = new ArrayList<>();
            return;
        }
        if (++ticks % 10 != 0) return;

        List<Vec3> list = new ArrayList<>();
        BlockPos c = mc.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS && list.size() < MAX_MARKS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS && list.size() < MAX_MARKS; dz++) {
                for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                    pos.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (ripe(mc.level.getBlockState(pos))) {
                        list.add(new Vec3(pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5));
                    }
                }
            }
        }
        marks = list;
    }

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || marks.isEmpty() || !CustomGuiConfig.get().cropAlert) return;
        if (mc.gui.screen() != null || !mc.options.getCameraType().isFirstPerson()) return;

        float pt = delta.getGameTimeDeltaPartialTick(false);
        Vec3 eye = mc.player.getEyePosition(pt);
        double yaw = Math.toRadians(mc.player.getViewYRot(pt));
        double pitch = Math.toRadians(mc.player.getViewXRot(pt));

        // Blickrichtung, rechts und oben (Minecraft: Yaw 0 = Süden)
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        double fov = Math.toRadians(mc.options.fov().get());
        double focal = (sh / 2.0) / Math.tan(fov / 2.0);

        Component mark = Component.literal("!").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
        for (Vec3 m : marks) {
            double dx = m.x - eye.x;
            double dy = m.y - eye.y;
            double dz = m.z - eye.z;
            double z = dx * fx + dy * fy + dz * fz;
            if (z < 0.5) continue;
            double x = dx * rx + dz * rz;
            double y = dx * ux + dy * uy + dz * uz;
            float sx = (float) (sw / 2.0 + x / z * focal);
            float sy = (float) (sh / 2.0 - y / z * focal);
            if (sx < -20 || sx > sw + 20 || sy < -20 || sy > sh + 20) continue;

            float s = (float) Math.max(0.7, Math.min(2.2, 12.0 / z));
            gfx.pose().pushMatrix();
            gfx.pose().translate(sx, sy);
            gfx.pose().scale(s, s);
            gfx.text(mc.font, mark, -mc.font.width(mark) / 2, -8, 0xFFFFD83A);
            gfx.pose().popMatrix();
        }
    }
}
