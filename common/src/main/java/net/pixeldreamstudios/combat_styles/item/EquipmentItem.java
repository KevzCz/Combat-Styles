package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTooltip;

import java.util.List;

public class EquipmentItem extends Item {

    private static final String KEY_NO_SLOT = "item.combat_styles.equipment.no_slot";
    private static final String DESC_SUFFIX = ".desc";

    private final String equipmentId;

    public EquipmentItem(Properties properties, String equipmentId) {
        super(properties);
        this.equipmentId = equipmentId;
    }

    public String equipmentId() {
        return equipmentId;
    }

    public EquipmentDefinition definition() {
        return EquipmentRegistry.get(equipmentId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        EquipmentDefinition own = EquipmentTooltip.shownFor(stack);
        EquipmentDefinition definition = own == null ? definition() : own;

        tooltip.add(Component.translatable(getDescriptionId() + DESC_SUFFIX)
                .withStyle(ChatFormatting.DARK_GRAY));
        if (definition != null && !definition.hideRarity()) {
            definition.rarity().ifPresent(rarity -> tooltip.add(
                    Component.translatable(rarity.translationKey()).withStyle(rarity.color())));
        }

        ItemTooltip.equipmentExpandable(tooltip, lines -> EquipmentTooltip.append(definition, lines));

        if (!AccessoryCompat.isLoaded()) {
            tooltip.add(Component.translatable(KEY_NO_SLOT).withStyle(ChatFormatting.RED));
        }
    }
}
