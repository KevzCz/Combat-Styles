package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class StackAttributes {

    private StackAttributes() {
    }

    public static void forEachBonus(ItemStack stack, EquipmentSlotGroup slotGroup,
                                    BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        emit(stack, slotGroup == null
                ? null
                : declared -> EquipmentSlotGroup.bySlot(declared) == slotGroup, consumer);
    }

    public static void forEachBonus(ItemStack stack, EquipmentSlot slot,
                                    BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        emit(stack, slot == null ? null : declared -> declared == slot, consumer);
    }

    private static void emit(ItemStack stack, Predicate<EquipmentSlot> accepts,
                             BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        if (stack == null || stack.isEmpty() || accepts == null || EquipmentRegistry.isEmpty()) {
            return;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }
        for (Map.Entry<String, AttributeBonus> entry : summedBonuses(definition)) {
            AttributeBonus bonus = entry.getValue();
            if (bonus == null || bonus.amount() == 0.0D || bonus.usesAccessorySlot()) {
                continue;
            }
            EquipmentSlot declared = bonus.vanillaSlot();
            if (declared == null || !accepts.test(declared)) {
                continue;
            }
            Holder<Attribute> attribute = holderOf(entry.getKey());
            if (attribute == null) {
                continue;
            }
            consumer.accept(attribute, new AttributeModifier(itemId,
                    bonus.amount(), bonus.operation()));
        }
    }

    private static List<Map.Entry<String, AttributeBonus>> summedBonuses(
            EquipmentDefinition definition) {
        return List.copyOf(definition.passiveAttributes().entrySet());
    }

    private static Holder<Attribute> holderOf(String attributeId) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceKey.create(Registries.ATTRIBUTE, id))
                .orElse(null);
    }
}
