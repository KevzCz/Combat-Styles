package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record HoldComboPayload(int slots) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HoldComboPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("hold_combo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HoldComboPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<HoldComboPayload>map(
                    HoldComboPayload::new, HoldComboPayload::slots).cast();

    @Override
    public Type<HoldComboPayload> type() {
        return TYPE;
    }
}
