package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;

import java.util.Optional;

public record ApplyStackEquipmentPayload(Optional<EquipmentDefinition> definition)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ApplyStackEquipmentPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("apply_stack_equipment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStackEquipmentPayload>
            STREAM_CODEC = ByteBufCodecs.optional(EquipmentDefinition.STREAM_CODEC)
                    .<ApplyStackEquipmentPayload>map(ApplyStackEquipmentPayload::new,
                            ApplyStackEquipmentPayload::definition)
                    .cast();

    public static ApplyStackEquipmentPayload applying(EquipmentDefinition definition) {
        return new ApplyStackEquipmentPayload(Optional.ofNullable(definition));
    }

    public static ApplyStackEquipmentPayload clearing() {
        return new ApplyStackEquipmentPayload(Optional.empty());
    }

    @Override
    public Type<ApplyStackEquipmentPayload> type() {
        return TYPE;
    }
}
