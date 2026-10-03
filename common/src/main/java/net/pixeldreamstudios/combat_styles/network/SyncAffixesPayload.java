package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record SyncAffixesPayload(Map<String, StyleAffix> affixes, Set<String> configIds,
                                 Set<String> dataIds)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAffixesPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_affixes"));

    private static final StreamCodec<ByteBuf, List<String>> IDS_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAffixesPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public SyncAffixesPayload decode(RegistryFriendlyByteBuf buffer) {
                    Map<String, StyleAffix> affixes = StyleAffix.MAP_STREAM_CODEC.decode(buffer);
                    List<String> configIds = IDS_CODEC.decode(buffer);
                    List<String> dataIds = IDS_CODEC.decode(buffer);
                    return new SyncAffixesPayload(affixes, Set.copyOf(configIds),
                            Set.copyOf(dataIds));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SyncAffixesPayload payload) {
                    StyleAffix.MAP_STREAM_CODEC.encode(buffer, payload.affixes());
                    IDS_CODEC.encode(buffer, List.copyOf(payload.configIds()));
                    IDS_CODEC.encode(buffer, List.copyOf(payload.dataIds()));
                }
            };

    public void applyToClient() {
        AffixRegistry.acceptSync(affixes, configIds, dataIds);
    }

    @Override
    public Type<SyncAffixesPayload> type() {
        return TYPE;
    }
}
