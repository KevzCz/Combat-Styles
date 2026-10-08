package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record WornEquipment(ItemStack stack, EquipmentDefinition definition, int copy) {

    public String owner() {
        return EffectKeys.ownerOf(definition.id(), copy);
    }

    public static List<WornEquipment> of(LivingEntity wearer) {
        List<WornEquipment> found = new ArrayList<>();
        Map<String, Integer> copies = new HashMap<>();
        for (ItemStack stack : WornItems.of(wearer)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null) {
                continue;
            }
            int copy = copies.merge(definition.id(), 1, Integer::sum) - 1;
            found.add(new WornEquipment(stack, definition, copy));
        }
        return found;
    }

    public static WornEquipment find(LivingEntity wearer, String definitionId, int copy) {
        for (WornEquipment worn : of(wearer)) {
            if (worn.copy() == copy && worn.definition().id().equals(definitionId)) {
                return worn;
            }
        }
        return null;
    }

    public static int copyOf(LivingEntity wearer, ItemStack stack) {
        for (WornEquipment worn : of(wearer)) {
            if (worn.stack() == stack) {
                return worn.copy();
            }
        }
        return 0;
    }
}
