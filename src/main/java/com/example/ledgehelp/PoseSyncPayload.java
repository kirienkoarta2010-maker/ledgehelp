package com.example.ledgehelp;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PoseSyncPayload(UUID id, int pose) implements CustomPacketPayload {
    public static final Type<PoseSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LedgeHelp.MODID, "pose_sync"));
    public static final StreamCodec<ByteBuf, PoseSyncPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PoseSyncPayload::id,
            ByteBufCodecs.VAR_INT, PoseSyncPayload::pose,
            PoseSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
