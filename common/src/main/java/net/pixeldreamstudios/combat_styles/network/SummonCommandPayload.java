package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public record SummonCommandPayload(Command command, int targetId) implements CustomPacketPayload {

    public static final int NO_TARGET = -1;

    public enum Command {
        RECALL,
        FOCUS
    }

    public static final CustomPacketPayload.Type<SummonCommandPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("summon_command"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonCommandPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(
                            ordinal -> Command.values()[Math.floorMod(ordinal, Command.values().length)],
                            Command::ordinal),
                    SummonCommandPayload::command,
                    ByteBufCodecs.VAR_INT.cast(),
                    SummonCommandPayload::targetId,
                    SummonCommandPayload::new);

    @Override
    public Type<SummonCommandPayload> type() {
        return TYPE;
    }
}
