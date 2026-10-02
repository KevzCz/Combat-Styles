package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.pixeldreamstudios.combat_styles.style.AttackAttribute;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record AttributeBonus(double amount, AttributeModifier.Operation operation,
                             boolean stacks, boolean set, Optional<String> slot) {

    public static final String SLOT_ACCESSORY = "accessory";

    public static final List<String> SLOTS = List.of(
            SLOT_ACCESSORY, "mainhand", "offhand", "head", "chest", "legs", "feet");

    public static final Codec<AttributeBonus> EXPANDED = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.DOUBLE.fieldOf("amount").forGetter(AttributeBonus::amount),
                    AttackAttribute.OPERATION_CODEC
                            .optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                            .forGetter(AttributeBonus::operation),
                    Codec.BOOL.optionalFieldOf("stacks", true).forGetter(AttributeBonus::stacks),
                    Codec.BOOL.optionalFieldOf("set", false).forGetter(AttributeBonus::set),
                    Codec.STRING.optionalFieldOf("slot").forGetter(AttributeBonus::slot)
            ).apply(instance, AttributeBonus::new));

    public static final Codec<AttributeBonus> CODEC = Codec.either(Codec.DOUBLE, EXPANDED)
            .xmap(either -> either.map(AttributeBonus::flat, value -> value),
                    value -> value.isPlain()
                            ? Either.left(value.amount())
                            : Either.right(value));

    public static AttributeBonus flat(double amount) {
        return new AttributeBonus(amount, AttributeModifier.Operation.ADD_VALUE,
                true, false, Optional.empty());
    }

    public boolean isPlain() {
        return operation == AttributeModifier.Operation.ADD_VALUE
                && stacks && !set && slot.isEmpty();
    }

    public AttributeBonus withAmount(double value) {
        return new AttributeBonus(value, operation, stacks, set, slot);
    }

    public AttributeBonus withOperation(AttributeModifier.Operation value) {
        return new AttributeBonus(amount, value, stacks, set, slot);
    }

    public AttributeBonus withStacks(boolean value) {
        return new AttributeBonus(amount, operation, value, set, slot);
    }

    public AttributeBonus withSet(boolean value) {
        return new AttributeBonus(amount, operation, stacks, value, slot);
    }

    public AttributeBonus withSlot(String value) {
        return new AttributeBonus(amount, operation, stacks, set,
                value == null || value.isBlank() || SLOT_ACCESSORY.equals(value)
                        ? Optional.empty()
                        : Optional.of(value));
    }

    public String slotName() {
        return slot.orElse(SLOT_ACCESSORY);
    }

    public boolean usesAccessorySlot() {
        return slot.map(SLOT_ACCESSORY::equals).orElse(true);
    }

    public EquipmentSlot vanillaSlot() {
        return slot.map(AttributeBonus::slotOf).orElse(null);
    }

    public AttackAttribute asAttackAttribute() {
        return new AttackAttribute(amount, operation, stacks, set);
    }

    public static AttributeBonus of(AttackAttribute value) {
        return value == null ? null : new AttributeBonus(value.amount(), value.operation(),
                value.stacks(), value.set(), Optional.empty());
    }

    private static EquipmentSlot slotOf(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT)) {
            case "mainhand" -> EquipmentSlot.MAINHAND;
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}
