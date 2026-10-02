package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;

import java.util.Map;

public record SyncAffixesPayload(Map<String, StyleAffix> affixes)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAffixesPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_affixes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAffixesPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public SyncAffixesPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SyncAffixesPayload(
                            StyleAffix.MAP_STREAM_CODEC.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SyncAffixesPayload payload) {
                    StyleAffix.MAP_STREAM_CODEC.encode(buffer, payload.affixes());
                }
            };

    @Override
    public Type<SyncAffixesPayload> type() {
        return TYPE;
    }
}
