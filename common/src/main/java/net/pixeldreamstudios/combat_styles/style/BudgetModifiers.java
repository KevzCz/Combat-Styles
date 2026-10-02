package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.api.BudgetModifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BudgetModifiers {

    private static final List<BudgetModifier> MODIFIERS = new CopyOnWriteArrayList<>();

    private BudgetModifiers() {
    }

    public static void register(BudgetModifier modifier) {
        if (modifier != null && !MODIFIERS.contains(modifier)) {
            MODIFIERS.add(modifier);
        }
    }

    public static boolean isEmpty() {
        return MODIFIERS.isEmpty();
    }

    public static double apply(LivingEntity holder, ItemStack stack, double budget) {
        if (MODIFIERS.isEmpty()) {
            return budget;
        }
        String category = SequenceService.categoryOf(stack);
        double result = budget;
        for (BudgetModifier modifier : MODIFIERS) {
            try {
                result = modifier.modify(holder, stack, category, result);
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Budget modifier failed", throwable);
            }
        }
        return Math.max(0.0D, result);
    }
}
