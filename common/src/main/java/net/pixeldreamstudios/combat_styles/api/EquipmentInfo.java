package net.pixeldreamstudios.combat_styles.api;

import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;

public interface EquipmentInfo {

    EquipmentDefinition definitionFor(ItemStack stack);

    default boolean isCombatEquipment(ItemStack stack) {
        return definitionFor(stack) != null;
    }
}
