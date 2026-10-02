package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.HashMap;
import java.util.Map;

public record SyncLearnedPayload(Map<String, Integer> copies,
                                 Map<String, StyleEntry> variants) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncLearnedPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_learned"));

    private static final StreamCodec<ByteBuf, Map<String, Integer>> COPIES_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);

    private static final StreamCodec<ByteBuf, Map<String, StyleEntry>> VARIANTS_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, StyleEntry.STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLearnedPayload> STREAM_CODEC =
            StreamCodec.<ByteBuf, SyncLearnedPayload, Map<String, Integer>, Map<String, StyleEntry>>composite(
                    COPIES_CODEC, SyncLearnedPayload::copies,
                    VARIANTS_CODEC, SyncLearnedPayload::variants,
                    SyncLearnedPayload::new).cast();

    @Override
    public Type<SyncLearnedPayload> type() {
        return TYPE;
    }
}
