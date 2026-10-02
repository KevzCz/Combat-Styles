package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record AlignComboPayload(int combo) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AlignComboPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("align_combo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlignComboPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<AlignComboPayload>map(
                    AlignComboPayload::new, AlignComboPayload::combo).cast();

    @Override
    public Type<AlignComboPayload> type() {
        return TYPE;
    }
}
