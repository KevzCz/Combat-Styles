package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record SwingHastePayload(float ratio, int swings, String hitbox)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SwingHastePayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("swing_haste"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwingHastePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, SwingHastePayload::ratio,
                    ByteBufCodecs.VAR_INT, SwingHastePayload::swings,
                    ByteBufCodecs.STRING_UTF8, SwingHastePayload::hitbox,
                    SwingHastePayload::new);

    public static final int THIS_SWING_ONLY = 0;

    public static SwingHastePayload of(float ratio, int swings, String hitbox) {
        return new SwingHastePayload(ratio, swings, hitbox == null ? "" : hitbox);
    }

    public String hitboxOrNull() {
        return hitbox.isEmpty() ? null : hitbox;
    }

    @Override
    public Type<SwingHastePayload> type() {
        return TYPE;
    }
}
