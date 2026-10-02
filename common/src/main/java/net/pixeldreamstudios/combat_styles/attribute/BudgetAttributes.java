package net.pixeldreamstudios.combat_styles.attribute;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class BudgetAttributes {

    private static final double PERCENT = 100.0D;

    private BudgetAttributes() {
    }

    public static double apply(LivingEntity entity, double budget) {
        if (entity == null) {
            return budget;
        }
        double flat = valueOf(entity, ModAttributes.BUDGET_FLAT_ID, ModAttributes.FLAT_DEFAULT);
        double multiplier = valueOf(entity, ModAttributes.BUDGET_MULTIPLIER_ID,
                ModAttributes.MULTIPLIER_DEFAULT) / PERCENT;
        return Math.max(0.0D, (budget + flat) * Math.max(0.0D, multiplier));
    }

    public static double valueOf(LivingEntity entity, String path, double fallback) {
        AttributeInstance instance = instanceOf(entity, path);
        return instance == null ? fallback : instance.getValue();
    }

    private static AttributeInstance instanceOf(LivingEntity entity, String path) {
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceKey.create(Registries.ATTRIBUTE, CombatStyles.id(path)))
                .map(holder -> (Holder<Attribute>) holder)
                .orElse(null);
        return attribute == null ? null : entity.getAttribute(attribute);
    }
}
