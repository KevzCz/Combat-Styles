package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record BeamPayload(int sourceId, Vec3 offset, Vec3 end, int rgb, float width, float alpha,
                          int durationTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BeamPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeamPayload> STREAM_CODEC =
            StreamCodec.of(BeamPayload::write, BeamPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, BeamPayload payload) {
        buffer.writeVarInt(payload.sourceId);
        buffer.writeVec3(payload.offset);
        buffer.writeVec3(payload.end);
        buffer.writeInt(payload.rgb);
        buffer.writeFloat(payload.width);
        buffer.writeFloat(payload.alpha);
        buffer.writeVarInt(payload.durationTicks);
    }

    private static BeamPayload read(RegistryFriendlyByteBuf buffer) {
        return new BeamPayload(buffer.readVarInt(), buffer.readVec3(), buffer.readVec3(),
                buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt());
    }

    @Override
    public Type<BeamPayload> type() {
        return TYPE;
    }
}
