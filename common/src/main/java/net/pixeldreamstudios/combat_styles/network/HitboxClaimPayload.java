package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.List;

public record HitboxClaimPayload(List<Integer> entityIds, List<Integer> boxIndices,
                                 List<Integer> priorities, int extraIndex, int delayStep,
                                 int cursorTargetId, float yaw, float pitch)
        implements CustomPacketPayload {

    public static final int NO_TARGET = -1;
    public static final int FIRST_STEP = 0;
    public static final int SAME_SWING = -2;

    public static final CustomPacketPayload.Type<HitboxClaimPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("hitbox_claims"));

    private static final StreamCodec<ByteBuf, List<Integer>> INTS =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, HitboxClaimPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public HitboxClaimPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new HitboxClaimPayload(INTS.decode(buffer), INTS.decode(buffer),
                            INTS.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.FLOAT.decode(buffer),
                            ByteBufCodecs.FLOAT.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, HitboxClaimPayload payload) {
                    INTS.encode(buffer, payload.entityIds());
                    INTS.encode(buffer, payload.boxIndices());
                    INTS.encode(buffer, payload.priorities());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.extraIndex());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.delayStep());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.cursorTargetId());
                    ByteBufCodecs.FLOAT.encode(buffer, payload.yaw());
                    ByteBufCodecs.FLOAT.encode(buffer, payload.pitch());
                }
            };

    @Override
    public Type<HitboxClaimPayload> type() {
        return TYPE;
    }
}
