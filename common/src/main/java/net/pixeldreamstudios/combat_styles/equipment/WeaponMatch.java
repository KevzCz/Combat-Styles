package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.List;
import java.util.Locale;

public final class WeaponMatch {

    public static final String TAG_MARKER = "#";

    private WeaponMatch() {
    }

    public static boolean matches(ItemStack stack, StyleSubstitution substitution) {
        if (substitution == null) {
            return false;
        }
        return matches(stack, substitution.weaponCategories(), substitution.itemTags());
    }

    public static boolean matches(ItemStack stack, List<String> weaponCategories,
                                  List<String> itemTags) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (weaponCategories.isEmpty() && itemTags.isEmpty()) {
            return true;
        }
        return matchesCategory(stack, weaponCategories) || matchesTag(stack, itemTags);
    }

    public static boolean targets(ItemStack stack, List<String> items, List<String> itemTags,
                                  List<String> weaponCategories) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return listsItem(stack, items)
                || matchesCategory(stack, weaponCategories)
                || matchesTag(stack, itemTags);
    }

    public static boolean listsItem(ItemStack stack, List<String> ids) {
        if (ids.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && ids.contains(itemId.toString());
    }

    private static boolean matchesCategory(ItemStack stack, List<String> weaponCategories) {
        if (weaponCategories.isEmpty()) {
            return false;
        }
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(stack);
        String category = attributes == null ? null : attributes.category();
        if (category == null) {
            return false;
        }
        for (String wanted : weaponCategories) {
            if (category.equalsIgnoreCase(wanted)
                    || category.toLowerCase(Locale.ROOT).endsWith(":" + wanted.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesTag(ItemStack stack, List<String> itemTags) {
        for (String tag : itemTags) {
            ResourceLocation id = ResourceLocation.tryParse(
                    tag.startsWith(TAG_MARKER) ? tag.substring(TAG_MARKER.length()) : tag);
            if (id == null) {
                continue;
            }
            if (stack.is(TagKey.create(Registries.ITEM, id))) {
                return true;
            }
        }
        return false;
    }
}
