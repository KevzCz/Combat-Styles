package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record ItemMatch(List<String> items, List<String> itemTags, List<String> weaponCategories,
                        List<String> excludedItems, List<String> excludedTags,
                        List<String> excludedCategories, Optional<Boolean> empty) {

    public static final String ITEMS = "items";
    public static final String ITEM_TAGS = "item_tags";
    public static final String WEAPON_CATEGORIES = "weapon_categories";
    public static final String EXCLUDED_ITEMS = "excluded_items";
    public static final String EXCLUDED_TAGS = "excluded_tags";
    public static final String EXCLUDED_CATEGORIES = "excluded_categories";
    public static final String EMPTY = "empty";

    public record Lists(String items, String tags, String categories) {
    }

    public static final Lists PICK = new Lists(ITEMS, ITEM_TAGS, WEAPON_CATEGORIES);
    public static final Lists EXCLUDE = new Lists(EXCLUDED_ITEMS, EXCLUDED_TAGS, EXCLUDED_CATEGORIES);

    public static final ItemMatch NONE = new ItemMatch(List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), Optional.empty());

    public static final MapCodec<ItemMatch> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf(ITEMS, List.of()).forGetter(ItemMatch::items),
            Codec.STRING.listOf().optionalFieldOf(ITEM_TAGS, List.of()).forGetter(ItemMatch::itemTags),
            Codec.STRING.listOf().optionalFieldOf(WEAPON_CATEGORIES, List.of())
                    .forGetter(ItemMatch::weaponCategories),
            Codec.STRING.listOf().optionalFieldOf(EXCLUDED_ITEMS, List.of())
                    .forGetter(ItemMatch::excludedItems),
            Codec.STRING.listOf().optionalFieldOf(EXCLUDED_TAGS, List.of())
                    .forGetter(ItemMatch::excludedTags),
            Codec.STRING.listOf().optionalFieldOf(EXCLUDED_CATEGORIES, List.of())
                    .forGetter(ItemMatch::excludedCategories),
            Codec.BOOL.optionalFieldOf(EMPTY).forGetter(ItemMatch::empty)
    ).apply(instance, ItemMatch::new));

    public static final Codec<ItemMatch> CODEC = MAP_CODEC.codec();

    public ItemMatch {
        items = meaningful(items);
        itemTags = meaningful(itemTags);
        weaponCategories = meaningful(weaponCategories);
        excludedItems = meaningful(excludedItems);
        excludedTags = meaningful(excludedTags);
        excludedCategories = meaningful(excludedCategories);
        empty = empty == null ? Optional.empty() : empty;
    }

    private static List<String> meaningful(List<String> entries) {
        return entries == null
                ? List.of()
                : entries.stream().filter(entry -> entry != null && !entry.isBlank()).toList();
    }

    public boolean picks() {
        return !items.isEmpty() || !itemTags.isEmpty() || !weaponCategories.isEmpty();
    }

    public boolean excludes() {
        return !excludedItems.isEmpty() || !excludedTags.isEmpty() || !excludedCategories.isEmpty();
    }

    public boolean isEmpty() {
        return !picks() && !excludes() && empty.isEmpty();
    }

    public boolean test(ItemStack stack) {
        return test(List.of(stack == null ? ItemStack.EMPTY : stack));
    }

    public boolean test(List<ItemStack> stacks) {
        boolean vacant = stacks.stream().allMatch(ItemStack::isEmpty);
        if (empty.isPresent() && empty.get() != vacant) {
            return false;
        }
        if (picks() && stacks.stream().noneMatch(stack ->
                WeaponMatch.targets(stack, items, itemTags, weaponCategories))) {
            return false;
        }
        return !excludes() || stacks.stream().noneMatch(stack ->
                WeaponMatch.targets(stack, excludedItems, excludedTags, excludedCategories));
    }
}
