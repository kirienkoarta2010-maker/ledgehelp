package com.example.ledgehelp;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(LedgeHelp.MODID)
public class LedgeHelp {
    public static final String MODID = "ledgehelp";
    public static final String VERSION = "1.3.0";

    public LedgeHelp(IEventBus modBus) {
        modBus.addListener(this::registerPayloads);
        LogUtils.getLogger().info("Ledge Help {} загружается", VERSION);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // клиент -> сервер: игрок держит / отпустил клавишу
        registrar.playToServer(HoldPayload.TYPE, HoldPayload.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                LedgeServer.setHolding(sp, payload.holding());
            }
        });

        // сервер -> игрок: показать подсказку с клавишей
        registrar.playToClient(HintPayload.TYPE, HintPayload.CODEC,
                (payload, ctx) -> ClientHintState.set(payload.hint()));

        // сервер -> клиенты: какая поза сейчас у игрока
        registrar.playToClient(PoseSyncPayload.TYPE, PoseSyncPayload.CODEC,
                (payload, ctx) -> PoseState.set(payload.id(), payload.pose()));
    }
}
