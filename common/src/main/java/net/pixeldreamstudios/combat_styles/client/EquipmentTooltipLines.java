package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTooltip;
import net.pixeldreamstudios.combat_styles.item.EquipmentItem;
import net.pixeldreamstudios.combat_styles.item.ItemTooltip;

import java.util.List;

public final class EquipmentTooltipLines {

    private EquipmentTooltipLines() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty() || lines == null
                || stack.getItem() instanceof EquipmentItem) {
            return;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null
                || (definition.effects().isEmpty() && definition.innateAffixes().isEmpty())) {
            return;
        }
        try {
            ItemTooltip.expandable(lines,
                    details -> EquipmentTooltip.append(definition, details));
        } catch (UnsupportedOperationException ignored) {
            CombatStyles.LOGGER.debug("Tooltip list for {} was immutable", definition.id());
        }
    }
}
