package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.WeaponMatch;

import java.util.List;

public final class PreviewSamples {

    private PreviewSamples() {
    }

    public static ItemStack ofItem(String itemId) {
        ResourceLocation parsed = itemId == null || itemId.isBlank()
                ? null
                : ResourceLocation.tryParse(itemId);
        return parsed == null || !BuiltInRegistries.ITEM.containsKey(parsed)
                ? ItemStack.EMPTY
                : new ItemStack(BuiltInRegistries.ITEM.get(parsed));
    }

    public static ItemStack first(List<String> items, List<String> tags,
                                  List<String> categories) {
        for (String id : items) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null && BuiltInRegistries.ITEM.containsKey(parsed)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(parsed));
            }
        }
        for (String tag : tags) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag);
            if (parsed == null) {
                continue;
            }
            TagKey<Item> key = TagKey.create(Registries.ITEM, parsed);
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty() && stack.is(key)) {
                    return stack;
                }
            }
        }
        if (!categories.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty() && WeaponMatch.matches(stack, categories, List.of())) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
