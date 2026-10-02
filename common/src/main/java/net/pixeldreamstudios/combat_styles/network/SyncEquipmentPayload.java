package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record SyncEquipmentPayload(Map<String, EquipmentDefinition> definitions,
                                   Set<String> fromData,
                                   Set<String> overridden)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncEquipmentPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_equipment"));

    private static final StreamCodec<ByteBuf, List<String>> IDS_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEquipmentPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public SyncEquipmentPayload decode(RegistryFriendlyByteBuf buffer) {
                    Map<String, EquipmentDefinition> definitions =
                            EquipmentDefinition.MAP_STREAM_CODEC.decode(buffer);
                    List<String> fromData = IDS_CODEC.decode(buffer);
                    List<String> overridden = IDS_CODEC.decode(buffer);
                    return new SyncEquipmentPayload(definitions,
                            Set.copyOf(fromData), Set.copyOf(overridden));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SyncEquipmentPayload payload) {
                    EquipmentDefinition.MAP_STREAM_CODEC.encode(buffer, payload.definitions());
                    IDS_CODEC.encode(buffer, List.copyOf(payload.fromData()));
                    IDS_CODEC.encode(buffer, List.copyOf(payload.overridden()));
                }
            };

    @Override
    public Type<SyncEquipmentPayload> type() {
        return TYPE;
    }
}
