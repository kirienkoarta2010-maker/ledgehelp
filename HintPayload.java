package com.example.ledgehelp;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> игроку: какую подсказку показывать (0 нет, 1 можно схватиться, 2 можно помочь). */
public record HintPayload(int hint) implements CustomPacketPayload {
    public static final Type<HintPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LedgeHelp.MODID, "hint"));
    public static final StreamCodec<ByteBuf, HintPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(HintPayload::new, HintPayload::hint);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
