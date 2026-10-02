package net.pixeldreamstudios.combat_styles.equipment.fabric;

import com.google.common.collect.Multimap;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentAttributes;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;

final class TrinketsInternal {

    private static final String SLOT_SEPARATOR = "/";

    private TrinketsInternal() {
    }

    static void registerItem(Item item) {
        TrinketsApi.registerTrinket(item, new Trinket() {
            @Override
            public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(
                    ItemStack stack, SlotReference slot, LivingEntity entity,
                    ResourceLocation id) {
                Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                        Trinket.super.getModifiers(stack, slot, entity, id);
                for (Map.Entry<String, AttributeBonus> entry
                        : EquipmentAttributes.declaredFor(stack).entrySet()) {
                    Holder<Attribute> attribute = attributeOf(entry.getKey());
                    if (attribute == null) {
                        continue;
                    }
                    AttributeBonus bonus = entry.getValue();
                    modifiers.put(attribute, new AttributeModifier(id,
                            bonus.amount(), bonus.operation()));
                }
                return modifiers;
            }
        });
    }

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
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty() || slot == null || slot.isBlank()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<String, Map<String, TrinketInventory>> group
                : component.get().getInventory().entrySet()) {
            for (Map.Entry<String, TrinketInventory> named : group.getValue().entrySet()) {
                if (!slot.equals(group.getKey() + SLOT_SEPARATOR + named.getKey())
                        && !slot.equals(named.getKey())) {
                    continue;
                }
                TrinketInventory inventory = named.getValue();
                for (int index = 0; index < inventory.getContainerSize(); index++) {
                    stacks.add(inventory.getItem(index));
                }
            }
        }
        return stacks;
    }

    static List<String> slotIds(Level level) {
        List<String> ids = new ArrayList<>();
        for (SlotGroup group : TrinketsApi.getPlayerSlots(level).values()) {
            for (String name : group.getSlots().keySet()) {
                ids.add(group.getName() + SLOT_SEPARATOR + name);
            }
        }
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    static List<ItemStack> equipped(Player player) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (var pair : component.get().getEquipped(stack -> !stack.isEmpty())) {
            ItemStack stack = pair.getB();
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
