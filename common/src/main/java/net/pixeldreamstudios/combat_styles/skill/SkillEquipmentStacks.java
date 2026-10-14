package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillEquipmentStacks {

    private static final Item CARRIER = Items.PAPER;
    private static final String TEXTURE_SUFFIX = ".png";

    private record Cached(long generation, EquipmentDefinition definition, String icon, ItemStack stack,
                          ResourceLocation texture) {
    }

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    private SkillEquipmentStacks() {
    }

    public static List<ItemStack> of(Player player) {
        List<String> granted = SkillGrants.equipmentOf(player);
        if (granted.isEmpty()) {
            return List.of();
        }
        long generation = EquipmentRegistry.generation();
        List<ItemStack> stacks = new ArrayList<>(granted.size());
        for (String id : granted) {
            EquipmentDefinition definition = EquipmentRegistry.get(id);
            if (definition == null) {
                continue;
            }
            String icon = SkillGrants.iconOf(player, id);
            Cached cached = CACHE.get(id);
            if (cached == null || cached.generation() != generation || cached.definition() != definition
                    || !Objects.equals(cached.icon(), icon)) {
                cached = new Cached(generation, definition, icon, carrierFor(definition, icon), textureOf(icon));
                CACHE.put(id, cached);
            }
            stacks.add(cached.stack());
        }
        return stacks;
    }

    public static boolean isSkillStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (Cached cached : CACHE.values()) {
            if (cached.stack() == stack) {
                return true;
            }
        }
        return false;
    }

    public static ResourceLocation textureOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        for (Cached cached : CACHE.values()) {
            if (cached.stack() == stack) {
                return cached.texture();
            }
        }
        return null;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static ResourceLocation textureOf(String icon) {
        return icon != null && icon.endsWith(TEXTURE_SUFFIX) ? ResourceLocation.tryParse(icon) : null;
    }

    private static ItemStack carrierFor(EquipmentDefinition definition, String icon) {
        ResourceLocation iconId = icon == null || textureOf(icon) != null ? null : ResourceLocation.tryParse(icon);
        Item item = iconId == null ? CARRIER : BuiltInRegistries.ITEM.getOptional(iconId).orElse(CARRIER);
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.STACK_EQUIPMENT.get(), definition);
        return stack;
    }
}
