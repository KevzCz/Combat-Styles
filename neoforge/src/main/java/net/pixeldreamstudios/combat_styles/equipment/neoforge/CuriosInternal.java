package net.pixeldreamstudios.combat_styles.equipment.neoforge;

import com.google.common.collect.Multimap;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentAttributes;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;

final class CuriosInternal {

    private CuriosInternal() {
    }

    static void registerItem(Item item) {
        CuriosApi.registerCurio(item, new ICurioItem() {
            @Override
            public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                    SlotContext context, UUID id, ItemStack stack) {
                Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                        ICurioItem.super.getAttributeModifiers(context, id, stack);
                for (Map.Entry<String, AttributeBonus> entry
                        : EquipmentAttributes.declaredFor(stack).entrySet()) {
                    Holder<Attribute> attribute = attributeOf(entry.getKey());
                    if (attribute == null) {
                        continue;
                    }
                    AttributeBonus bonus = entry.getValue();
                    modifiers.put(attribute, new AttributeModifier(
                            CombatStyles.id(MODIFIER_PATH + "/" + context.identifier()
                                    + "/" + entry.getKey().replace(':', '.')),
                            bonus.amount(), bonus.operation()));
                }
                return modifiers;
            }
        });
    }

    private static final String MODIFIER_PATH = "accessory";

    private static Holder<Attribute> attributeOf(String attributeId) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceKey.create(Registries.ATTRIBUTE, id))
                .orElse(null);
    }

    static List<ItemStack> inSlot(Player player, String slot) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        if (optional.isEmpty() || slot == null || slot.isBlank()) {
            return List.of();
        }
        Optional<ICurioStacksHandler> handler = optional.get().getStacksHandler(slot);
        if (handler.isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < handler.get().getSlots(); index++) {
            stacks.add(handler.get().getStacks().getStackInSlot(index));
        }
        return stacks;
    }

    static List<String> slotIds(Level level) {
        List<String> ids = new ArrayList<>(CuriosApi.getPlayerSlots(level).keySet());
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    static List<ItemStack> equipped(Player player) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        if (optional.isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ICurioStacksHandler handler : optional.get().getCurios().values()) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStacks().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
        return stacks;
    }
}
