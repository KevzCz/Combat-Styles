package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;

public record ApplyStackEquipmentPayload(EquipmentDefinition definition)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ApplyStackEquipmentPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("apply_stack_equipment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStackEquipmentPayload>
            STREAM_CODEC = EquipmentDefinition.STREAM_CODEC
                    .<ApplyStackEquipmentPayload>map(ApplyStackEquipmentPayload::new,
                            ApplyStackEquipmentPayload::definition)
                    .cast();

    @Override
    public Type<ApplyStackEquipmentPayload> type() {
        return TYPE;
    }
}
