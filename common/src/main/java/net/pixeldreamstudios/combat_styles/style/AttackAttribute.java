package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.Locale;

public record AttackAttribute(double amount, AttributeModifier.Operation operation,
                              boolean stacks, boolean set) {

    public static final String OPERATION_SET = "set";

    public AttackAttribute(double amount, AttributeModifier.Operation operation,
                           boolean stacks) {
        this(amount, operation, stacks, false);
    }

    public static AttributeModifier.Operation operationOf(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT)) {
            case "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    public static String operationName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_MULTIPLIED_BASE -> "multiply_base";
            case ADD_MULTIPLIED_TOTAL -> "multiply_total";
            default -> "add_value";
        };
    }

    public static final Codec<AttributeModifier.Operation> OPERATION_CODEC = Codec.STRING.xmap(
            AttackAttribute::operationOf, AttackAttribute::operationName);

    private static final Codec<AttackAttribute> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(AttackAttribute::amount),
            OPERATION_CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                    .forGetter(AttackAttribute::operation),
            Codec.BOOL.optionalFieldOf("stacks", true).forGetter(AttackAttribute::stacks),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(AttackAttribute::set)
    ).apply(instance, AttackAttribute::new));

    public static final Codec<AttackAttribute> CODEC = Customs.wrap(AttackAttribute.class, Codec.either(Codec.DOUBLE, FULL)
            .xmap(either -> either.map(AttackAttribute::flat, value -> value),
                    value -> value.operation() == AttributeModifier.Operation.ADD_VALUE
                            && value.stacks()
                            ? Either.left(value.amount())
                            : Either.right(value)));

    public AttackAttribute(double amount, AttributeModifier.Operation operation) {
        this(amount, operation, true);
    }

    public AttackAttribute withStacks(boolean value) {
        return new AttackAttribute(amount, operation, value, false);
    }

    public static AttackAttribute flat(double amount) {
        return new AttackAttribute(amount, AttributeModifier.Operation.ADD_VALUE, true, false);
    }
}
