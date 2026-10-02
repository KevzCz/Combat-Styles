package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Map;

public record ApplySequencePayload(EquipmentSlot slot, StyleSequence sequence,
                                   Map<String, AttackOverride> attackOverrides,
                                   boolean improvised,
                                   Optional<Double> budgetOverride)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ApplySequencePayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("apply_sequence"));

    private static final StreamCodec<RegistryFriendlyByteBuf, EquipmentSlot> SLOT_CODEC =
            ByteBufCodecs.idMapper(index -> EquipmentSlot.values()[index], EquipmentSlot::ordinal)
                    .cast();

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, AttackOverride>> OVERRIDES_CODEC =
            ByteBufCodecs.<ByteBuf, String, AttackOverride, Map<String, AttackOverride>>map(
                            size -> new LinkedHashMap<>(), ByteBufCodecs.STRING_UTF8,
                            AttackOverride.STREAM_CODEC)
                    .cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplySequencePayload> STREAM_CODEC =
            StreamCodec.composite(
                    SLOT_CODEC, ApplySequencePayload::slot,
                    StyleSequence.STREAM_CODEC, ApplySequencePayload::sequence,
                    OVERRIDES_CODEC, ApplySequencePayload::attackOverrides,
                    ByteBufCodecs.BOOL.cast(), ApplySequencePayload::improvised,
                    ByteBufCodecs.optional(ByteBufCodecs.DOUBLE).cast(),
                            ApplySequencePayload::budgetOverride,
                    ApplySequencePayload::new);

    @Override
    public Type<ApplySequencePayload> type() {
        return TYPE;
    }
}
