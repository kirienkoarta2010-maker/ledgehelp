package com.example.ledgehelp;

import com.mojang.math.Axis;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Рисует мерцающую (оранжевый/белый) и дрожащую букву клавиши внизу по центру, без рамки. */
public final class HintOverlay {

    public static void render(GuiGraphics gui, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!ClientHintState.active()) return;
        if (PoseState.get(mc.player.getUUID()) != PoseState.NONE) return;

        float t = mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false);

        // мерцание: оранжевый (EF9F27) <-> белый
        float k = (Mth.sin(t * 0.35F) + 1.0F) / 2.0F;
        int r = (int) Mth.lerp(k, 0xEF, 0xFF);
        int g = (int) Mth.lerp(k, 0x9F, 0xFF);
        int b = (int) Mth.lerp(k, 0x27, 0xFF);
        int color = 0xFF000000 | (r << 16) | (g << 8) | b;

        // лёгкая тряска
        float dx = Mth.sin(t * 2.7F) * 1.5F;
        float dy = Mth.cos(t * 3.3F) * 1.0F;
        float angle = Mth.sin(t * 2.2F) * 0.06F;

        String label = ClientModEvents.HOLD.getTranslatedKeyMessage().getString();

        var pose = gui.pose();
        pose.pushPose();
        pose.translate(gui.guiWidth() / 2.0F + dx, gui.guiHeight() - 85.0F + dy, 0.0F);
        pose.mulPose(Axis.ZP.rotation(angle));
        pose.scale(3.0F, 3.0F, 1.0F);
        gui.drawString(mc.font, label, -mc.font.width(label) / 2, -4, color, true);
        pose.popPose();
    }

    private HintOverlay() {}
}
