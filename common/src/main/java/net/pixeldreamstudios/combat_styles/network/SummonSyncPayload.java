package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.UUID;

public record SummonSyncPayload(int entityId, UUID entityUuid, UUID owner, boolean spectral)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SummonSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("summon_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.cast(),
                    SummonSyncPayload::entityId,
                    UUIDUtil.STREAM_CODEC.cast(),
                    SummonSyncPayload::entityUuid,
                    UUIDUtil.STREAM_CODEC.cast(),
                    SummonSyncPayload::owner,
                    ByteBufCodecs.BOOL.cast(),
                    SummonSyncPayload::spectral,
                    SummonSyncPayload::new);

    @Override
    public Type<SummonSyncPayload> type() {
        return TYPE;
    }
}
