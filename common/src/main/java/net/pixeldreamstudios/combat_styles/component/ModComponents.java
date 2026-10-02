package net.pixeldreamstudios.combat_styles.component;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.StackPose;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(CombatStyles.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<StyleSequence>> SEQUENCE =
            COMPONENTS.register("sequence", () -> DataComponentType.<StyleSequence>builder()
                    .persistent(StyleSequence.CODEC)
                    .networkSynchronized(StyleSequence.STREAM_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<StyleSequence>> ROLLED_SEQUENCE =
            COMPONENTS.register("rolled_sequence", () -> DataComponentType.<StyleSequence>builder()
                    .persistent(StyleSequence.CODEC)
                    .networkSynchronized(StyleSequence.STREAM_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<StyleSequence>> CARRIED_STYLES =
            COMPONENTS.register("carried_styles", () -> DataComponentType.<StyleSequence>builder()
                    .persistent(StyleSequence.CODEC)
                    .networkSynchronized(StyleSequence.STREAM_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<Boolean>> ETCHED =
            COMPONENTS.register("etched", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final RegistrySupplier<DataComponentType<Boolean>> IMPROVISED =
            COMPONENTS.register("improvised", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final RegistrySupplier<DataComponentType<Boolean>> LOCKED =
            COMPONENTS.register("locked", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final RegistrySupplier<DataComponentType<Boolean>> IGNORE_MINIMUM =
            COMPONENTS.register("ignore_minimum", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final RegistrySupplier<DataComponentType<Integer>> SLOT_LIMIT =
            COMPONENTS.register("slot_limit", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final RegistrySupplier<DataComponentType<Double>> BUDGET_OVERRIDE =
            COMPONENTS.register("budget_override", () -> DataComponentType.<Double>builder()
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
                    .build());

    public static final RegistrySupplier<DataComponentType<String>> AFFIX_ID =
            COMPONENTS.register("affix_id", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final RegistrySupplier<DataComponentType<List<String>>> AFFIX_IDS =
            COMPONENTS.register("affix_ids", () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8
                            .apply(ByteBufCodecs.list()))
                    .build());

    public static final RegistrySupplier<DataComponentType<Map<UUID, StyleSequence>>> MOVESETS =
            COMPONENTS.register("movesets", () ->
                    DataComponentType.<Map<UUID, StyleSequence>>builder()
                            .persistent(Codec.unboundedMap(UUIDUtil.STRING_CODEC, StyleSequence.CODEC))
                            .build());

    public static final RegistrySupplier<DataComponentType<StackPose>> STACK_POSE =
            COMPONENTS.register("stack_pose", () -> DataComponentType.<StackPose>builder()
                    .persistent(StackPose.CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<EquipmentDefinition>> STACK_EQUIPMENT =
            COMPONENTS.register("stack_equipment", () ->
                    DataComponentType.<EquipmentDefinition>builder()
                            .persistent(EquipmentDefinition.CODEC)
                            .build());

    public static final RegistrySupplier<DataComponentType<StackPose>> EQUIPMENT_POSE =
            COMPONENTS.register("equipment_pose", () -> DataComponentType.<StackPose>builder()
                    .persistent(StackPose.CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<Map<String, AttackOverride>>> ATTACK_OVERRIDES =
            COMPONENTS.register("attack_overrides", () ->
                    DataComponentType.<Map<String, AttackOverride>>builder()
                            .persistent(Codec.unboundedMap(Codec.STRING, AttackOverride.CODEC))
                            .build());

    public static final RegistrySupplier<DataComponentType<String>> STYLE_ID =
            COMPONENTS.register("style_id", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModComponents() {
    }

    public static void register() {
        COMPONENTS.register();
    }
}
