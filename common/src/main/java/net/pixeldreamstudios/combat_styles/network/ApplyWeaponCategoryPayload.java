package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record ApplyWeaponCategoryPayload(EquipmentSlot slot, String category)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ApplyWeaponCategoryPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("apply_weapon_category"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyWeaponCategoryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(
                            ordinal -> EquipmentSlot.values()[ordinal],
                            EquipmentSlot::ordinal),
                    ApplyWeaponCategoryPayload::slot,
                    ByteBufCodecs.STRING_UTF8.cast(),
                    ApplyWeaponCategoryPayload::category,
                    ApplyWeaponCategoryPayload::new);

    @Override
    public Type<ApplyWeaponCategoryPayload> type() {
        return TYPE;
    }
}
