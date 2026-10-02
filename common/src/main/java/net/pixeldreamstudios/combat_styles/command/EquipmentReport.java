package net.pixeldreamstudios.combat_styles.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.equipment.WeaponMatch;
import net.pixeldreamstudios.combat_styles.equipment.WornItems;
import net.pixeldreamstudios.combat_styles.item.EquipmentItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EquipmentReport {

    private static final String PREFIX = "command.combat_styles.equipment.";
    private static final String KEY_SUMMARY = PREFIX + "summary";
    private static final String KEY_ACCESSORY_YES = PREFIX + "accessory_yes";
    private static final String KEY_ACCESSORY_NO = PREFIX + "accessory_no";
    private static final String KEY_DEFINITION = PREFIX + "definition";
    private static final String KEY_WORN_HEADER = PREFIX + "worn_header";
    private static final String KEY_WORN_ENTRY = PREFIX + "worn_entry";
    private static final String KEY_WORN_NONE = PREFIX + "worn_none";
    private static final String KEY_NOTHING_WORN = PREFIX + "nothing_worn";
    private static final String KEY_STACK_OVERRIDE = PREFIX + "stack_override";
    private static final String KEY_SWING_COUNT = PREFIX + "swing_count";
    private static final String KEY_DETAIL_HINT = PREFIX + "detail_hint";
    private static final String KEY_UNKNOWN = PREFIX + "unknown";
    private static final String KEY_DETAIL_HEADER = PREFIX + "detail_header";
    private static final String KEY_SOURCES = PREFIX + "sources";
    private static final String KEY_EXCLUDES = PREFIX + "excludes";
    private static final String KEY_GROUP = PREFIX + "group";
    private static final String KEY_MORE = PREFIX + "more";
    private static final String KEY_REASON_OWN = PREFIX + "reason_own";
    private static final String KEY_REASON_LISTED = PREFIX + "reason_listed";
    private static final String KEY_REASON_TAG = PREFIX + "reason_tag";
    private static final String KEY_REASON_CATEGORY = PREFIX + "reason_category";
    private static final String KEY_SHADOWED = PREFIX + "shadowed";
    private static final String KEY_SHADOWED_ENTRY = PREFIX + "shadowed_entry";
    private static final String KEY_EXCLUDED = PREFIX + "excluded";
    private static final String KEY_NO_MATCHES = PREFIX + "no_matches";

    private static final int MAX_LISTED = 32;
    private static final String SEPARATOR = ", ";
    private static final String NONE = "-";

    private record Group(Component label, List<String> items) {
    }

    private record Shadow(String item, String winner) {
    }

    private EquipmentReport() {
    }

    public static int overview(CommandSourceStack source, ServerPlayer player) {
        List<EquipmentDefinition> definitions = sorted();
        Component accessories = Component.translatable(AccessoryCompat.isLoaded()
                ? KEY_ACCESSORY_YES : KEY_ACCESSORY_NO);
        source.sendSuccess(() -> Component.translatable(KEY_SUMMARY, definitions.size(),
                accessories), false);

        Map<String, Integer> resolved = resolvedCounts();
        for (EquipmentDefinition definition : definitions) {
            int count = resolved.getOrDefault(definition.id(), 0);
            source.sendSuccess(() -> Component.translatable(KEY_DEFINITION,
                    Component.literal(definition.id()).withStyle(ChatFormatting.AQUA), count,
                    definition.items().size(), definition.itemTags().size(),
                    definition.weaponCategories().size()), false);
        }

        source.sendSuccess(() -> Component.translatable(KEY_WORN_HEADER)
                .withStyle(ChatFormatting.GOLD), false);
        List<ItemStack> worn = WornItems.of(player);
        if (worn.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(KEY_NOTHING_WORN), false);
        }
        for (ItemStack stack : worn) {
            source.sendSuccess(() -> wornLine(stack), false);
        }
        int swings = EquipmentRuntime.swingCount(player);
        source.sendSuccess(() -> Component.translatable(KEY_SWING_COUNT, swings), false);
        source.sendSuccess(() -> Component.translatable(KEY_DETAIL_HINT)
                .withStyle(ChatFormatting.GRAY), false);
        return definitions.size();
    }

    public static int detail(CommandSourceStack source, String id) {
        EquipmentDefinition definition = EquipmentRegistry.get(id);
        if (definition == null) {
            source.sendFailure(Component.translatable(KEY_UNKNOWN, id));
            return 0;
        }

        Map<String, Group> groups = new LinkedHashMap<>();
        List<Shadow> shadowed = new ArrayList<>();
        int excluded = 0;
        int matched = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            boolean own = item instanceof EquipmentItem equipment
                    && id.equals(equipment.equipmentId());
            boolean listed = WeaponMatch.listsItem(stack, definition.items());
            List<String> tags = definition.itemTags().stream()
                    .filter(tag -> WeaponMatch.matches(stack, List.of(), List.of(tag)))
                    .toList();
            List<String> categories = definition.weaponCategories().stream()
                    .filter(category -> WeaponMatch.matches(stack, List.of(category), List.of()))
                    .toList();
            if (!own && !listed && tags.isEmpty() && categories.isEmpty()) {
                continue;
            }
            if (!own && !definition.matches(stack)) {
                excluded++;
                continue;
            }
            matched++;
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            if (own) {
                add(groups, KEY_REASON_OWN, Component.translatable(KEY_REASON_OWN), itemId);
            }
            if (listed) {
                add(groups, KEY_REASON_LISTED, Component.translatable(KEY_REASON_LISTED), itemId);
            }
            for (String tag : tags) {
                add(groups, KEY_REASON_TAG + tag,
                        Component.translatable(KEY_REASON_TAG, tagLabel(tag)), itemId);
            }
            for (String category : categories) {
                add(groups, KEY_REASON_CATEGORY + category,
                        Component.translatable(KEY_REASON_CATEGORY, category), itemId);
            }
            EquipmentDefinition winner = EquipmentProviders.definitionFor(stack);
            if (winner != null && !winner.id().equals(id)) {
                shadowed.add(new Shadow(itemId, winner.id()));
            }
        }

        int total = matched;
        source.sendSuccess(() -> Component.translatable(KEY_DETAIL_HEADER,
                Component.literal(id).withStyle(ChatFormatting.AQUA), total), false);
        source.sendSuccess(() -> Component.translatable(KEY_SOURCES,
                joined(definition.items()), joined(definition.itemTags().stream()
                        .map(EquipmentReport::tagLabel).toList()),
                joined(definition.weaponCategories())).withStyle(ChatFormatting.GRAY), false);
        if (!definition.excludedItems().isEmpty() || !definition.excludedTags().isEmpty()
                || !definition.excludedCategories().isEmpty()) {
            source.sendSuccess(() -> Component.translatable(KEY_EXCLUDES,
                    joined(definition.excludedItems()), joined(definition.excludedTags().stream()
                            .map(EquipmentReport::tagLabel).toList()),
                    joined(definition.excludedCategories())).withStyle(ChatFormatting.GRAY), false);
        }
        if (groups.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(KEY_NO_MATCHES)
                    .withStyle(ChatFormatting.RED), false);
        }
        for (Group group : groups.values()) {
            source.sendSuccess(() -> groupLine(group), false);
        }
        if (excluded > 0) {
            int dropped = excluded;
            source.sendSuccess(() -> Component.translatable(KEY_EXCLUDED, dropped)
                    .withStyle(ChatFormatting.GRAY), false);
        }
        if (!shadowed.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(KEY_SHADOWED, shadowed.size())
                    .withStyle(ChatFormatting.YELLOW), false);
            for (Shadow shadow : shadowed.subList(0, Math.min(MAX_LISTED, shadowed.size()))) {
                source.sendSuccess(() -> Component.translatable(KEY_SHADOWED_ENTRY,
                        shadow.item(), shadow.winner()).withStyle(ChatFormatting.YELLOW), false);
            }
            if (shadowed.size() > MAX_LISTED) {
                int more = shadowed.size() - MAX_LISTED;
                source.sendSuccess(() -> Component.translatable(KEY_MORE, more)
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }
        return total;
    }

    public static List<String> ids() {
        return sorted().stream().map(EquipmentDefinition::id).toList();
    }

    private static List<EquipmentDefinition> sorted() {
        List<EquipmentDefinition> definitions = new ArrayList<>(EquipmentRegistry.all());
        definitions.sort(Comparator.comparing(EquipmentDefinition::id));
        return definitions;
    }

    private static Map<String, Integer> resolvedCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition != null) {
                counts.merge(definition.id(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Component wornLine(ItemStack stack) {
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        Component resolved = definition == null
                ? Component.translatable(KEY_WORN_NONE).withStyle(ChatFormatting.DARK_GRAY)
                : Component.literal(definition.id()).withStyle(ChatFormatting.AQUA);
        MutableComponent line = Component.translatable(KEY_WORN_ENTRY,
                stack.getHoverName(), resolved);
        if (stack.get(ModComponents.STACK_EQUIPMENT.get()) != null) {
            line.append(Component.translatable(KEY_STACK_OVERRIDE)
                    .withStyle(ChatFormatting.GRAY));
        }
        return line;
    }

    private static Component groupLine(Group group) {
        List<String> items = group.items();
        String shown = String.join(SEPARATOR, items.subList(0, Math.min(MAX_LISTED, items.size())));
        MutableComponent line = Component.translatable(KEY_GROUP,
                group.label().copy().withStyle(ChatFormatting.GREEN), items.size(), shown);
        if (items.size() > MAX_LISTED) {
            line.append(Component.translatable(KEY_MORE, items.size() - MAX_LISTED)
                    .withStyle(ChatFormatting.GRAY));
        }
        return line;
    }

    private static void add(Map<String, Group> groups, String key, Component label, String item) {
        groups.computeIfAbsent(key, ignored -> new Group(label, new ArrayList<>())).items().add(item);
    }

    private static String tagLabel(String tag) {
        return tag.startsWith(WeaponMatch.TAG_MARKER) ? tag : WeaponMatch.TAG_MARKER + tag;
    }

    private static String joined(List<String> values) {
        return values.isEmpty() ? NONE : String.join(SEPARATOR, values);
    }
}
