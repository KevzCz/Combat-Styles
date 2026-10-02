package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.attribute.BudgetAttributes;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.progression.ProgressionBudget;

import java.util.Optional;

public final class BudgetSources {

    private BudgetSources() {
    }

    public static double totalFor(LivingEntity holder, ItemStack stack) {
        double base = overrideOf(stack).orElseGet(() -> Math.max(
                StyleCost.budgetOf(SequenceService.baseAttributes(stack),
                        DefaultStyles.slotLimitFor(stack)),
                grantedBudgetOf(stack)));
        return totalWithOverride(holder, stack, base);
    }

    public static double totalWithOverride(LivingEntity holder, ItemStack stack, double base) {
        double budget = BudgetAttributes.apply(holder,
                base + ProgressionBudget.forHolder(holder));
        return BudgetModifiers.apply(holder, stack, budget);
    }

    public static Optional<Double> overrideOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        Double stored = stack.get(ModComponents.BUDGET_OVERRIDE.get());
        return stored == null || stored < 0.0D ? Optional.empty() : Optional.of(stored);
    }

    public static void setOverride(ItemStack stack, Double value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (value == null || value < 0.0D) {
            stack.remove(ModComponents.BUDGET_OVERRIDE.get());
        } else {
            stack.set(ModComponents.BUDGET_OVERRIDE.get(), value);
        }
    }

    public static double rolledBudgetOf(ItemStack stack) {
        StyleSequence rolled = SequenceService.rolledSequenceOf(stack);
        if (rolled.isEmpty()) {
            return 0.0D;
        }
        return StyleCost.totalOf(rolled, SequenceService.categoryOf(stack));
    }

    public static double grantedBudgetOf(ItemStack stack) {
        double rolled = rolledBudgetOf(stack);
        if (rolled > 0.0D) {
            return rolled;
        }
        return defaultBudgetOf(stack);
    }

    public static double defaultBudgetOf(ItemStack stack) {
        StyleSequence defaults = CustomStyles.resolve(DefaultStyles.forStack(stack));
        if (defaults.isEmpty()) {
            return 0.0D;
        }
        return StyleCost.totalOf(defaults, SequenceService.categoryOf(stack));
    }
}
