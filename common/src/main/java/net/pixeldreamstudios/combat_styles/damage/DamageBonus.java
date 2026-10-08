package net.pixeldreamstudios.combat_styles.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.DamageTypeMatch;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DamageBonus(List<String> damageTypes, double amount,
                          AttributeModifier.Operation operation, boolean stacks) {

    public static final String KEY = "damage_bonus";
    private static final String KEY_JOIN = "|";
    private static final String KEY_FLAT = "tooltip.combat_styles.damage_bonus.flat";
    private static final String KEY_PERCENT = "tooltip.combat_styles.damage_bonus.percent";
    private static final String KEY_MULTIPLIER = "tooltip.combat_styles.damage_bonus.multiplier";
    private static final String KEY_ANY = "tooltip.combat_styles.damage_bonus.any";
    private static final double PERCENT = 100.0D;

    public static final Codec<DamageBonus> CODEC = Customs.wrap(DamageBonus.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("damage_type", List.of())
                    .forGetter(DamageBonus::damageTypes),
            Codec.DOUBLE.fieldOf("amount").forGetter(DamageBonus::amount),
            AttackAttribute.OPERATION_CODEC.optionalFieldOf("operation",
                    AttributeModifier.Operation.ADD_VALUE).forGetter(DamageBonus::operation),
            Codec.BOOL.optionalFieldOf("stacks", true).forGetter(DamageBonus::stacks)
    ).apply(instance, DamageBonus::new)));

    public static final Codec<List<DamageBonus>> LIST_CODEC = CODEC.listOf();

    public static final DamageBonus SAMPLE = new DamageBonus(List.of(), 0.2D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);

    public DamageBonus {
        damageTypes = damageTypes == null
                ? List.of()
                : damageTypes.stream().filter(type -> type != null && !type.isBlank()).toList();
        operation = operation == null ? AttributeModifier.Operation.ADD_VALUE : operation;
    }

    public boolean matches(DamageSource source) {
        return DamageTypeMatch.matches(damageTypes, source);
    }

    public MutableComponent describe() {
        Component types = damageTypes.isEmpty()
                ? Component.translatable(KEY_ANY)
                : Component.literal(AffixText.damageTypeNames(damageTypes));
        return switch (operation) {
            case ADD_MULTIPLIED_BASE -> Component.translatable(KEY_PERCENT,
                    StyleText.formatDecimal(amount * PERCENT), types);
            case ADD_MULTIPLIED_TOTAL -> Component.translatable(KEY_MULTIPLIER,
                    StyleText.formatDecimal(1.0D + amount), types);
            default -> Component.translatable(KEY_FLAT, StyleText.formatDecimal(amount), types);
        };
    }

    private String stackKey() {
        return operation.name() + KEY_JOIN + String.join(KEY_JOIN, damageTypes);
    }

    public static float apply(float amount, List<DamageBonus> bonuses, DamageSource source) {
        double flat = 0.0D;
        double base = 0.0D;
        double total = 1.0D;
        Map<String, DamageBonus> strongest = new LinkedHashMap<>();
        for (DamageBonus bonus : bonuses) {
            if (bonus.amount() == 0.0D || !bonus.matches(source)) {
                continue;
            }
            if (!bonus.stacks()) {
                strongest.merge(bonus.stackKey(), bonus,
                        (kept, offered) -> offered.amount() > kept.amount() ? offered : kept);
                continue;
            }
            switch (bonus.operation()) {
                case ADD_MULTIPLIED_BASE -> base += bonus.amount();
                case ADD_MULTIPLIED_TOTAL -> total *= 1.0D + bonus.amount();
                default -> flat += bonus.amount();
            }
        }
        for (DamageBonus bonus : strongest.values()) {
            switch (bonus.operation()) {
                case ADD_MULTIPLIED_BASE -> base += bonus.amount();
                case ADD_MULTIPLIED_TOTAL -> total *= 1.0D + bonus.amount();
                default -> flat += bonus.amount();
            }
        }
        return (float) Math.max(0.0D, (amount + flat) * (1.0D + base) * total);
    }
}
