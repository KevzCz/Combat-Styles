package net.pixeldreamstudios.combat_styles.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface BudgetModifier {

    double modify(LivingEntity holder, ItemStack stack, String weaponCategory, double budget);
}
