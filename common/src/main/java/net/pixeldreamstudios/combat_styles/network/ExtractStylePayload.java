package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record ExtractStylePayload(int slotIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ExtractStylePayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("extract_style"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractStylePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<ExtractStylePayload>map(
                    ExtractStylePayload::new, ExtractStylePayload::slotIndex).cast();

    @Override
    public Type<ExtractStylePayload> type() {
        return TYPE;
    }
}
