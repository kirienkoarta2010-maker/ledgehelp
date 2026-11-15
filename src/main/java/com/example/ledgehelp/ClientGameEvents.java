package com.example.ledgehelp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = LedgeHelp.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static boolean lastSent;

    /** Отправляем на сервер, держит ли игрок клавишу. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            lastSent = false;
            return;
        }
        boolean down = ClientModEvents.HOLD.isDown();
        if (down != lastSent) {
            lastSent = down;
            PacketDistributor.sendToServer(new HoldPayload(down));
        }
    }

    /** Пока игрок в особой позе, гасим его скорость, чтобы не дёргало. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre e) {
        Player p = e.getEntity();
        if (p.level().isClientSide && p == Minecraft.getInstance().player
                && PoseState.get(p.getUUID()) != PoseState.NONE) {
            p.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Блокируем ходьбу и прыжок (Shift оставляем, им отпускают край). */
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        Player p = e.getEntity();
        if (PoseState.get(p.getUUID()) == PoseState.NONE) return;
        Input in = e.getInput();
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.jumping = false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        PoseState.clear();
        lastSent = false;
    }

    private ClientGameEvents() {}
}
