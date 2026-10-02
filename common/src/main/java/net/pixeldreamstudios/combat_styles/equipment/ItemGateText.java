package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;

public final class ItemGateText {

    private static final String KEY_DUAL_WIELDING = "tooltip.combat_styles.while_dual_wielding";
    private static final String KEY_NOT_DUAL_WIELDING = "tooltip.combat_styles.while_not_dual_wielding";
    private static final String KEY_TWO_HANDED = "tooltip.combat_styles.while_two_handed";
    private static final String KEY_ONE_HANDED = "tooltip.combat_styles.while_one_handed";
    private static final String KEY_WITH_TAGS = "tooltip.combat_styles.while_tagged";
    private static final String KEY_WITH_SOME_TAGS = "tooltip.combat_styles.while_tagged_some";
    private static final String KEY_WITHOUT_TAGS = "tooltip.combat_styles.while_untagged";
    private static final String TAG_JOINER = ", ";
    private static final String KEY_WITH = "tooltip.combat_styles.item_with";
    private static final String KEY_WITHOUT = "tooltip.combat_styles.item_without";
    private static final String KEY_VACANT = "tooltip.combat_styles.item_vacant";
    private static final String KEY_OCCUPIED = "tooltip.combat_styles.item_occupied";
    private static final String TAG_MARKER = "#";
    private static final char NAMESPACE_SEPARATOR = ':';
    private static final String KEY_OR = "tooltip.combat_styles.item_or";
    private static final String KEY_SLOT_PREFIX = "tooltip.combat_styles.item_slot.";
    private static final String KEY_ANY_ACCESSORY = "tooltip.combat_styles.item_any_accessory";
    private static final String TAG_KEY_PREFIX = "tag.item.";
    private static final String KEY_SEPARATOR = ".";
    private static final String PATH_SEPARATOR = "/";
    private static final String WORD_SEPARATOR = "_";
    private static final String SPACE = " ";

    private ItemGateText() {
    }

    public static List<Component> parts(StateGate state) {
        List<Component> parts = new ArrayList<>();
        if (state == null) {
            return parts;
        }
        state.dualWielding().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_DUAL_WIELDING : KEY_NOT_DUAL_WIELDING)));
        state.twoHanded().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_TWO_HANDED : KEY_ONE_HANDED)));
        if (!state.playerTags().isEmpty()) {
            String listed = String.join(TAG_JOINER, state.playerTags());
            int needed = state.tagsNeeded();
            parts.add(needed < state.playerTags().size()
                    ? Component.translatable(KEY_WITH_SOME_TAGS, needed, listed)
                    : Component.translatable(KEY_WITH_TAGS, listed));
        }
        if (!state.missingPlayerTags().isEmpty()) {
            parts.add(Component.translatable(KEY_WITHOUT_TAGS,
                    String.join(TAG_JOINER, state.missingPlayerTags())));
        }
        ItemGate items = state.items();
        for (ItemGate.Slotted slot : items.slotted()) {
            addMatch(slot.match(), Component.translatable(KEY_SLOT_PREFIX + slot.key()), parts);
        }
        for (AccessoryMatch accessory : items.accessories()) {
            if (!accessory.isEmpty()) {
                addMatch(accessory.match(), accessory.anySlot()
                        ? Component.translatable(KEY_ANY_ACCESSORY)
                        : Component.literal(slotName(accessory.slot())), parts);
            }
        }
        return parts;
    }

    private static void addMatch(ItemMatch match, Component where, List<Component> parts) {
        if (match.isEmpty()) {
            return;
        }
        match.empty().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_VACANT : KEY_OCCUPIED, where)));
        if (match.picks()) {
            parts.add(Component.translatable(KEY_WITH, joined(match.items(), match.itemTags(),
                    match.weaponCategories()), where));
        }
        if (match.excludes()) {
            parts.add(Component.translatable(KEY_WITHOUT, joined(match.excludedItems(),
                    match.excludedTags(), match.excludedCategories()), where));
        }
    }

    private static Component joined(List<String> items, List<String> tags, List<String> categories) {
        List<Component> names = new ArrayList<>();
        items.forEach(id -> names.add(itemName(id)));
        tags.forEach(id -> names.add(tagName(id)));
        categories.forEach(id -> names.add(Component.literal(StyleText.prettify(shortName(id)))));
        MutableComponent joined = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                joined.append(Component.translatable(KEY_OR));
            }
            joined.append(names.get(i));
        }
        return joined;
    }

    private static String shortName(String id) {
        int colon = id.indexOf(NAMESPACE_SEPARATOR);
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static Component tagName(String raw) {
        ResourceLocation tag = ResourceLocation.tryParse(raw);
        String shown = TAG_MARKER + raw;
        if (tag == null) {
            return Component.literal(shown);
        }
        return Component.translatableWithFallback(TAG_KEY_PREFIX + tag.getNamespace()
                + KEY_SEPARATOR + tag.getPath().replace(PATH_SEPARATOR, KEY_SEPARATOR), shown);
    }

    private static Component itemName(String entry) {
        ResourceLocation id = ResourceLocation.tryParse(entry);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Component.literal(entry);
        }
        return BuiltInRegistries.ITEM.get(id).getDescription();
    }

    private static String slotName(String slot) {
        int cut = slot.lastIndexOf(PATH_SEPARATOR);
        return (cut < 0 ? slot : slot.substring(cut + 1)).replace(WORD_SEPARATOR, SPACE);
    }
}
