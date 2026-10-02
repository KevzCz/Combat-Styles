package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record SyncCooldownsPayload(Map<String, Integer> remaining,
                                   Map<String, Integer> total,
                                   Set<String> armed,
                                   String proc) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncCooldownsPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_cooldowns"));

    public static final String NO_PROC = "";

    private static final StreamCodec<ByteBuf, Map<String, Integer>> TICKS_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);

    private static final StreamCodec<ByteBuf, Set<String>> IDS_CODEC =
            ByteBufCodecs.collection(LinkedHashSet::new, ByteBufCodecs.STRING_UTF8)
                    .map(ids -> (Set<String>) ids, LinkedHashSet::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCooldownsPayload> STREAM_CODEC =
            StreamCodec.<ByteBuf, SyncCooldownsPayload, Map<String, Integer>,
                    Map<String, Integer>, Set<String>, String>composite(
                    TICKS_CODEC, SyncCooldownsPayload::remaining,
                    TICKS_CODEC, SyncCooldownsPayload::total,
                    IDS_CODEC, SyncCooldownsPayload::armed,
                    ByteBufCodecs.STRING_UTF8, SyncCooldownsPayload::proc,
                    SyncCooldownsPayload::new).cast();

    @Override
    public Type<SyncCooldownsPayload> type() {
        return TYPE;
    }
}
