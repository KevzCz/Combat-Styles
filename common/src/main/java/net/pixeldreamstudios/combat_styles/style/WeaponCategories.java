package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class WeaponCategories {

    private record ItemCategory(long generation, Optional<String> category) {
    }

    private record Adjusted(WeaponAttributes source, String category, WeaponAttributes adjusted) {
    }

    private static final ThreadLocal<Integer> RESOLVING = ThreadLocal.withInitial(() -> 0);
    private static final Map<ResourceLocation, ItemCategory> ITEMS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Adjusted> ADJUSTED = new ConcurrentHashMap<>();
    private static final EquipmentSlot[] HANDS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private WeaponCategories() {
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String name = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return name.isEmpty() || !ResourceLocation.isValidPath(name) ? Optional.empty() : Optional.of(name);
    }

    public static Optional<String> stackCategoryOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return normalize(stack.get(ModComponents.STACK_WEAPON_CATEGORY.get()));
    }

    private static Optional<String> equipmentCategoryOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || RESOLVING.get() > 0) {
            return Optional.empty();
        }
        RESOLVING.set(RESOLVING.get() + 1);
        try {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            return definition == null ? Optional.empty() : definition.weaponCategory();
        } finally {
            RESOLVING.set(RESOLVING.get() - 1);
        }
    }

    public static Optional<String> assigned(ItemStack stack) {
        Optional<String> own = stackCategoryOf(stack);
        return own.isPresent() ? own : equipmentCategoryOf(stack);
    }

    public static Optional<String> assigned(ResourceLocation itemId) {
        if (itemId == null || RESOLVING.get() > 0 || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return Optional.empty();
        }
        long generation = EquipmentRegistry.generation();
        ItemCategory cached = ITEMS.get(itemId);
        if (cached != null && cached.generation() == generation) {
            return cached.category();
        }
        Optional<String> category = equipmentCategoryOf(new ItemStack(BuiltInRegistries.ITEM.get(itemId)));
        ITEMS.put(itemId, new ItemCategory(generation, category));
        return category;
    }

    public static String categoryFor(ItemStack stack, String fallback) {
        return assigned(stack).orElse(fallback);
    }

    public static String categoryFor(ResourceLocation itemId, String fallback) {
        return assigned(itemId).orElse(fallback);
    }

    public static void store(ItemStack stack, String category) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Optional<String> name = normalize(category);
        if (name.isEmpty()) {
            stack.remove(ModComponents.STACK_WEAPON_CATEGORY.get());
            return;
        }
        stack.set(ModComponents.STACK_WEAPON_CATEGORY.get(), name.get());
    }

    public static WeaponAttributes applyTo(ItemStack stack, WeaponAttributes attributes) {
        if (attributes == null || stack == null || stack.isEmpty()) {
            return attributes;
        }
        return withCategory(BuiltInRegistries.ITEM.getKey(stack.getItem()), attributes, assigned(stack));
    }

    public static WeaponAttributes applyTo(ResourceLocation itemId, WeaponAttributes attributes) {
        if (attributes == null || itemId == null) {
            return attributes;
        }
        return withCategory(itemId, attributes, assigned(itemId));
    }

    private static WeaponAttributes withCategory(ResourceLocation itemId, WeaponAttributes attributes,
                                                 Optional<String> category) {
        if (category.isEmpty() || category.get().equals(attributes.category())) {
            return attributes;
        }
        Adjusted cached = ADJUSTED.get(itemId);
        if (cached != null && cached.source() == attributes && cached.category().equals(category.get())) {
            return cached.adjusted();
        }
        WeaponAttributes adjusted = new WeaponAttributes(attributes.attackRange(),
                attributes.rangeBonus(), attributes.pose(), attributes.offHandPose(),
                attributes.two_handed(), category.get(), attributes.attacks(),
                attributes.trailAppearance());
        ADJUSTED.put(itemId, new Adjusted(attributes, category.get(), adjusted));
        return adjusted;
    }

    public static Set<ResourceLocation> itemsIn(String category) {
        Set<ResourceLocation> items = new HashSet<>();
        if (category == null) {
            return items;
        }
        for (ResourceLocation itemId : BuiltInRegistries.ITEM.keySet()) {
            if (assigned(itemId).filter(category::equalsIgnoreCase).isPresent()) {
                items.add(itemId);
            }
        }
        return items;
    }

    public static List<String> allNames() {
        Set<String> names = new TreeSet<>(StyleRegistry.categories());
        for (EquipmentDefinition definition : EquipmentRegistry.all()) {
            definition.weaponCategory().ifPresent(names::add);
        }
        Player player = ClientHolder.localPlayer();
        if (player != null) {
            for (EquipmentSlot hand : HANDS) {
                assigned(player.getItemBySlot(hand)).ifPresent(names::add);
            }
        }
        return List.copyOf(names);
    }
}
