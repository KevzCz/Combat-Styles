package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record SyncConfigPayload(String json) implements CustomPacketPayload {

    public static final int MAX_JSON_LENGTH = 1 << 20;

    public static final CustomPacketPayload.Type<SyncConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(MAX_JSON_LENGTH).<SyncConfigPayload>map(
                    SyncConfigPayload::new, SyncConfigPayload::json).cast();

    @Override
    public Type<SyncConfigPayload> type() {
        return TYPE;
    }
}
