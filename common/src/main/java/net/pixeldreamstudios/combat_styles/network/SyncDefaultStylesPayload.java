package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.CustomAttack;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.SlotTuning;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SyncDefaultStylesPayload(Map<ResourceLocation, StyleSequence> sequences,
                                       Map<ResourceLocation, Integer> slotLimits,
                                       Set<ResourceLocation> appending,
                                       Set<ResourceLocation> tuning,
                                       Set<ResourceLocation> sticky,
                                       Set<ResourceLocation> locked,
                                       Map<ResourceLocation, List<SlotTuning>> slotTunings,
                                       Set<ResourceLocation> configItems,
                                       Map<String, CustomStyle> customStyles,
                                       Map<String, CustomAttack> customAttacks,
                                       Set<String> configStyles,
                                       Set<String> configAttacks,
                                       Set<String> dataStyles,
                                       Set<String> dataAttacks)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncDefaultStylesPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_default_styles"));

    private static final StreamCodec<ByteBuf, Map<ResourceLocation, StyleSequence>> SEQUENCES_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC.cast(),
                    StyleSequence.STREAM_CODEC);

    private static final StreamCodec<ByteBuf, Map<ResourceLocation, Integer>> LIMITS_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC.cast(), ByteBufCodecs.VAR_INT);

    private static final StreamCodec<ByteBuf, List<ResourceLocation>> APPENDING_CODEC =
            ResourceLocation.STREAM_CODEC.<ByteBuf>cast().apply(ByteBufCodecs.list());

    private static final StreamCodec<ByteBuf, Map<ResourceLocation, List<SlotTuning>>> TUNINGS_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC.cast(),
                    ByteBufCodecs.fromCodec(SlotTuning.CODEC.listOf()));

    private static final StreamCodec<ByteBuf, List<String>> IDS_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDefaultStylesPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public SyncDefaultStylesPayload decode(RegistryFriendlyByteBuf buffer) {
                    Map<ResourceLocation, StyleSequence> sequences = SEQUENCES_CODEC.decode(buffer);
                    Map<ResourceLocation, Integer> limits = LIMITS_CODEC.decode(buffer);
                    List<ResourceLocation> appending = APPENDING_CODEC.decode(buffer);
                    List<ResourceLocation> tuning = APPENDING_CODEC.decode(buffer);
                    List<ResourceLocation> sticky = APPENDING_CODEC.decode(buffer);
                    List<ResourceLocation> locked = APPENDING_CODEC.decode(buffer);
                    Map<ResourceLocation, List<SlotTuning>> tunings = TUNINGS_CODEC.decode(buffer);
                    List<ResourceLocation> configItems = APPENDING_CODEC.decode(buffer);
                    Map<String, CustomStyle> custom = CustomStyle.MAP_STREAM_CODEC.decode(buffer);
                    Map<String, CustomAttack> attacks = CustomAttack.MAP_STREAM_CODEC.decode(buffer);
                    List<String> configStyles = IDS_CODEC.decode(buffer);
                    List<String> configAttacks = IDS_CODEC.decode(buffer);
                    List<String> dataStyles = IDS_CODEC.decode(buffer);
                    List<String> dataAttacks = IDS_CODEC.decode(buffer);
                    return new SyncDefaultStylesPayload(sequences, limits,
                            Set.copyOf(appending), Set.copyOf(tuning), Set.copyOf(sticky), Set.copyOf(locked), tunings, Set.copyOf(configItems), custom, attacks,
                            Set.copyOf(configStyles), Set.copyOf(configAttacks),
                            Set.copyOf(dataStyles), Set.copyOf(dataAttacks));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SyncDefaultStylesPayload payload) {
                    SEQUENCES_CODEC.encode(buffer, payload.sequences());
                    LIMITS_CODEC.encode(buffer, payload.slotLimits());
                    APPENDING_CODEC.encode(buffer, List.copyOf(payload.appending()));
                    APPENDING_CODEC.encode(buffer, List.copyOf(payload.tuning()));
                    APPENDING_CODEC.encode(buffer, List.copyOf(payload.sticky()));
                    APPENDING_CODEC.encode(buffer, List.copyOf(payload.locked()));
                    TUNINGS_CODEC.encode(buffer, payload.slotTunings());
                    APPENDING_CODEC.encode(buffer, List.copyOf(payload.configItems()));
                    CustomStyle.MAP_STREAM_CODEC.encode(buffer, payload.customStyles());
                    CustomAttack.MAP_STREAM_CODEC.encode(buffer, payload.customAttacks());
                    IDS_CODEC.encode(buffer, List.copyOf(payload.configStyles()));
                    IDS_CODEC.encode(buffer, List.copyOf(payload.configAttacks()));
                    IDS_CODEC.encode(buffer, List.copyOf(payload.dataStyles()));
                    IDS_CODEC.encode(buffer, List.copyOf(payload.dataAttacks()));
                }
            };

    public void applyToClient() {
        DefaultStyles.acceptSync(sequences, slotLimits, appending);
        DefaultStyles.acceptSyncTuning(tuning);
        DefaultStyles.acceptSyncSticky(sticky);
        DefaultStyles.acceptSyncLocked(locked);
        DefaultStyles.acceptSyncSlotTunings(slotTunings);
        DefaultStyles.acceptSyncConfigItems(configItems);
        CustomStyles.acceptSync(customStyles, configStyles, dataStyles);
        CustomAttacks.acceptSync(customAttacks, configAttacks, dataAttacks);
    }

    @Override
    public Type<SyncDefaultStylesPayload> type() {
        return TYPE;
    }
}
