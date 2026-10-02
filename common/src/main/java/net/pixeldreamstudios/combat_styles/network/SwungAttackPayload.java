package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record SwungAttackPayload(Slot slot, String animation,
                                 String hitbox, float aimX,
                                 float aimZ) implements CustomPacketPayload {

    public record Slot(int index, int size) {

        public static final StreamCodec<ByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Slot::index,
                ByteBufCodecs.VAR_INT, Slot::size,
                Slot::new);
    }

    public static final CustomPacketPayload.Type<SwungAttackPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("swung_attack"));

    public SwungAttackPayload(int slot, int size, String animation,
                              String hitbox, float aimX, float aimZ) {
        this(new Slot(slot, size), animation, hitbox, aimX, aimZ);
    }

    public int slotIndex() {
        return slot.index();
    }

    public int sequenceSize() {
        return slot.size();
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SwungAttackPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Slot.STREAM_CODEC, SwungAttackPayload::slot,
                    ByteBufCodecs.STRING_UTF8, SwungAttackPayload::animation,
                    ByteBufCodecs.STRING_UTF8, SwungAttackPayload::hitbox,
                    ByteBufCodecs.FLOAT, SwungAttackPayload::aimX,
                    ByteBufCodecs.FLOAT, SwungAttackPayload::aimZ,
                    SwungAttackPayload::new);

    @Override
    public Type<SwungAttackPayload> type() {
        return TYPE;
    }
}
