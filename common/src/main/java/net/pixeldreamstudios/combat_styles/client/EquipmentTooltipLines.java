package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTooltip;
import net.pixeldreamstudios.combat_styles.item.EquipmentItem;
import net.pixeldreamstudios.combat_styles.item.ItemTooltip;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.List;

public final class EquipmentTooltipLines {

    private static final int NAME_LINE = 0;
    private static final int RARITY_LINE = 1;

    private EquipmentTooltipLines() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty() || lines == null) {
            return;
        }
        EquipmentDefinition definition = EquipmentTooltip.shownFor(stack);
        if (definition == null && stack.getItem() instanceof EquipmentItem equipment) {
            definition = equipment.definition();
        }
        try {
            applyRarity(stack, definition, lines);
            if (stack.getItem() instanceof EquipmentItem || !EquipmentTooltip.describes(definition)) {
                return;
            }
            EquipmentDefinition shown = definition;
            ItemTooltip.equipmentExpandable(lines, details -> EquipmentTooltip.append(shown, details));
        } catch (UnsupportedOperationException ignored) {
            CombatStyles.LOGGER.debug("Tooltip list for {} was immutable",
                    definition == null ? stack : definition.id());
        }
    }

    private static void applyRarity(ItemStack stack, EquipmentDefinition definition,
                                    List<Component> lines) {
        if (definition == null || definition.rarity().isEmpty() || lines.isEmpty()) {
            return;
        }
        StyleRarity rarity = definition.rarity().get();
        lines.set(NAME_LINE, lines.get(NAME_LINE).copy().withStyle(rarity.color()));
        if (stack.getItem() instanceof EquipmentItem || definition.hideRarity()) {
            return;
        }
        lines.add(Math.min(RARITY_LINE, lines.size()), rarityLine(rarity));
    }

    public static Component rarityLine(StyleRarity rarity) {
        return Component.translatable(rarity.translationKey()).withStyle(rarity.color());
    }
}
