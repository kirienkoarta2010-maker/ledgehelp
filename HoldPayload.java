package com.example.ledgehelp;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HoldPayload(boolean holding) implements CustomPacketPayload {
    public static final Type<HoldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LedgeHelp.MODID, "hold"));
    public static final StreamCodec<ByteBuf, HoldPayload> CODEC =
            ByteBufCodecs.BOOL.map(HoldPayload::new, HoldPayload::holding);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
