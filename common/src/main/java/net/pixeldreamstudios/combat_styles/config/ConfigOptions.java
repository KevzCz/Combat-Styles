package net.pixeldreamstudios.combat_styles.config;

import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.loot.EquipmentDrops;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfigOptions {

    public static final String GENERAL = "General";
    public static final String STATUS_EFFECTS = "Status Effects";
    public static final String EQUIPMENT = "Equipment Drops";
    public static final String SUMMONS = "Summons";

    public static final String SUGGEST_SCHOOLS = "schools";
    public static final String SUGGEST_SCALING_SOURCES = "scaling_sources";
    public static final String SUGGEST_DAMAGE_TYPES = "damage_types";
    public static final String SUGGEST_LOOT_TABLES = "loot_tables";
    public static final String SUGGEST_TABLE_PATTERNS = "table_patterns";
    public static final String SUGGEST_ENTITIES = "entities";
    public static final String SUGGEST_ENTITY_TAGS = "entity_tags";
    public static final String SUGGEST_STRUCTURES = "structures";
    public static final String SUGGEST_STRUCTURE_TAGS = "structure_tags";
    public static final String SUGGEST_DRAG_MOTIONS = "drag_motions";
    public static final String KIND_BOOL = "bool";
    public static final String KIND_HUD_DENSITY = "hud_density";
    public static final String HUD_DENSITY_DEFAULT = "chips";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    private static BiFunction<String, String, List<String>> suggester =
            (kind, typed) -> List.of();

    public static void suggestions(BiFunction<String, String, List<String>> source) {
        suggester = source == null ? (kind, typed) -> List.of() : source;
    }

    private static final String AFFIXES = "Affixes";
    private static final String BUDGET = "Budget";
    private static final String LOOT = "Loot";
    private static final String STYLES = "Styles";
    public static final String VISUALS = "Visuals";

    private static final Map<String, String> GROUP_BY_KEYWORD = Map.ofEntries(
            Map.entry("affix", AFFIXES),
            Map.entry("budget", BUDGET),
            Map.entry("loot", LOOT),
            Map.entry("drop", LOOT),
            Map.entry("chest", LOOT),
            Map.entry("weaponroll", LOOT),
            Map.entry("style", STYLES),
            Map.entry("moveset", STYLES),
            Map.entry("attack", STYLES),
            Map.entry("etch", STYLES),
            Map.entry("animation", VISUALS),
            Map.entry("preview", VISUALS),
            Map.entry("wave", VISUALS),
            Map.entry("swing", VISUALS),
            Map.entry("arc", VISUALS),
            Map.entry("tooltip", VISUALS),
            Map.entry("hud", VISUALS));

    private static String groupFor(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : GROUP_BY_KEYWORD.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return GENERAL;
    }

    public record Option(String group, String key, String label, String description,
                         boolean structural, String kind, Runnable action,
                         Supplier<String> reader, Consumer<String> writer) {

        public Option(String key, String label, Supplier<String> reader,
                      Consumer<String> writer) {
            this(GENERAL, key, label, "", false, "", null, reader, writer);
        }

        public Option in(String named) {
            return new Option(named, key, label, description, structural, kind, action,
                    reader, writer);
        }

        public Option describedAs(String text) {
            return new Option(group, key, label, text == null ? "" : text, structural, kind,
                    action, reader, writer);
        }

        public Option rebuilds() {
            return new Option(group, key, label, description, true, kind, action, reader, writer);
        }

        public Option suggesting(String named) {
            return new Option(group, key, label, description, structural,
                    named == null ? "" : named, action, reader, writer);
        }

        public List<String> suggestions(String typed) {
            return suggestFor(kind, typed);
        }

        public boolean isAction() {
            return action != null;
        }

        public boolean isToggle() {
            return KIND_BOOL.equals(kind);
        }

        public void toggle() {
            writer.accept(Boolean.toString(!Boolean.parseBoolean(reader.get())));
        }

        public void run() {
            if (action != null) {
                action.run();
            }
        }

        public boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            String needle = query.toLowerCase(Locale.ROOT);
            return label.toLowerCase(Locale.ROOT).contains(needle)
                    || key.toLowerCase(Locale.ROOT).contains(needle)
                    || group.toLowerCase(Locale.ROOT).contains(needle);
        }

        public String read() {
            return reader.get();
        }

        public void write(String raw) {
            writer.accept(raw);
        }
    }

    private ConfigOptions() {
    }

    public static List<Option> of(CombatStylesConfig config) {
        List<Option> sorted = new ArrayList<>();
        for (Option option : config.options()) {
            sorted.add(option.in(groupFor(option.key())));
        }
        return sorted;
    }

    public static List<Option> statusEffects() {
        return statusEffects(StatusEffectConfig::get);
    }

    public static List<Option> statusEffectDefaults() {
        return statusEffects(StatusEffectConfig::defaultOf);
    }

    private static List<Option> statusEffects(Function<String, StatusEffectConfig.Entry> source) {
        List<Option> options = new ArrayList<>();
        for (String id : StatusEffectConfig.all().keySet()) {
            StatusEffectConfig.Entry entry = source.apply(id);
            String group = STATUS_EFFECTS;
            options.add(bool(id + ".enabled", "Enabled",
                    () -> entry.enabled, value -> entry.enabled = value).in(group));
            options.add(integer(id + ".duration", "Duration (ticks)",
                    () -> entry.durationTicks, value -> entry.durationTicks = value).in(group));
            options.add(integer(id + ".max_stacks", "Max stacks",
                    () -> entry.maxStacks, value -> entry.maxStacks = value).in(group));
            options.add(decimal(id + ".per_stack", "Bonus per stack",
                    () -> (double) entry.perStack, value -> entry.perStack = value.floatValue()).in(group));
        }
        return options;
    }

    public static String prettify(String raw) {
        return StyleText.prettify(raw);
    }

    public static List<Option> equipmentDrops() {
        List<Option> options = new ArrayList<>();
        for (StyleRarity value : StyleRarity.values()) {
            String id = value.name().toLowerCase(Locale.ROOT);
            EquipmentDropConfig.Rarity rarity = EquipmentDropConfig.rarity(id);
            if (rarity == null) {
                continue;
            }
            options.add(decimal(id + ".chance", "Drop chance",
                    () -> rarity.chance, chance -> rarity.chance = chance)
                    .describedAs("0 to 1. Odds this rarity drops. 0 disables it.").in(EQUIPMENT));
            addSources(options, id + ".loot", rarity);
            List<String> items = new ArrayList<>();
            for (ResourceLocation item : EquipmentDrops.poolOf(id)) {
                items.add(item.toString());
            }
            for (String known : rarity.items.keySet()) {
                if (!items.contains(known)) {
                    items.add(known);
                }
            }
            for (String item : items) {
                EquipmentDropConfig.Item entry = EquipmentDropConfig.item(id, item);
                if (entry == null) {
                    continue;
                }
                String label = item.substring(item.indexOf(':') + 1);
                String base = id + ".items." + label;
                options.add(integer(base + ".weight", "Base weight",
                        () -> entry.weight, weight -> entry.weight = weight)
                        .describedAs("Share of " + id + " drops that are this item. 0 never drops.")
                        .in(EQUIPMENT));
                addBonuses(options, base + ".tables", entry.tableBonus, rarity);
            }
        }
        return options;
    }

    private static void addSources(List<Option> options, String base,
                                   EquipmentDropConfig.Rarity rarity) {
        addList(options, EQUIPMENT, base + ".loot_table.tables", rarity.tables,
                "Exact loot table id. Unknown ids wait until that mod is added.",
                SUGGEST_LOOT_TABLES);
        addList(options, EQUIPMENT, base + ".loot_table.patterns", rarity.tablePatterns,
                "Matches any loot table id containing this text.",
                SUGGEST_TABLE_PATTERNS);
        addList(options, EQUIPMENT, base + ".entity.ids", rarity.entities,
                "Entity id such as minecraft:wither.", SUGGEST_ENTITIES);
        addList(options, EQUIPMENT, base + ".entity.tags", rarity.entityTags,
                "Entity tag such as minecraft:skeletons.", SUGGEST_ENTITY_TAGS);
        addList(options, EQUIPMENT, base + ".structure.ids", rarity.structures,
                "Structure id such as minecraft:ancient_city.", SUGGEST_STRUCTURES);
        addList(options, EQUIPMENT, base + ".structure.tags", rarity.structureTags,
                "Structure tag such as minecraft:on_treasure_maps.", SUGGEST_STRUCTURE_TAGS);
    }

    public static void addList(List<Option> options, String group, String base,
                               List<String> values, String description) {
        addList(options, group, base, values, description, "");
    }

    public static void addList(List<Option> options, String group, String base,
                               List<String> values, String description, String kind) {
        for (int index = 0; index < values.size(); index++) {
            int slot = index;
            options.add(text(base + "." + slot, "#" + (slot + 1),
                    () -> slot < values.size() ? values.get(slot) : "",
                    entered -> {
                        if (slot < values.size()) {
                            values.set(slot, entered);
                        }
                    })
                    .describedAs(description).in(group).suggesting(kind));
            options.add(action(base + "." + slot + "_remove", "   Remove #" + (slot + 1),
                    () -> {
                        if (slot < values.size()) {
                            values.remove(slot);
                        }
                    })
                    .describedAs("Deletes this entry.").in(group));
        }
        options.add(action(base + ".add", "+ Add entry", () -> values.add(""))
                .describedAs("Adds a blank row. Type into it to search ids.")
                .in(group));
    }

    public static List<String> suggestFor(String kind, String typed) {
        if (KIND_BOOL.equals(kind)) {
            return List.of(TRUE, FALSE);
        }
        if (kind == null || kind.isEmpty()) {
            return List.of();
        }
        return suggester.apply(kind, typed == null ? "" : typed);
    }

    private static void addBonuses(List<Option> options, String base,
                                   Map<String, Integer> bonuses,
                                   EquipmentDropConfig.Rarity rarity) {
        List<String> keys = new ArrayList<>(bonuses.keySet());
        for (int index = 0; index < keys.size(); index++) {
            String where = keys.get(index);
            String slot = base + "." + index;
            options.add(integer(slot + ".bonus", where,
                    () -> bonuses.getOrDefault(where, 0),
                    bonus -> bonuses.put(where, bonus))
                    .describedAs("Added to the base weight in " + where
                            + ". Negative lowers it.")
                    .in(EQUIPMENT));
            options.add(action(slot + ".remove", "   Remove " + where,
                    () -> bonuses.remove(where))
                    .describedAs("Stops tuning this item there.").in(EQUIPMENT));
        }
        for (String suggestion : suggestions(rarity)) {
            if (bonuses.containsKey(suggestion)) {
                continue;
            }
            options.add(action(base + ".add_" + slug(suggestion), "+ " + suggestion,
                    () -> bonuses.putIfAbsent(suggestion, 0))
                    .describedAs("Tunes this item's weight in " + suggestion + ".")
                    .in(EQUIPMENT));
        }
    }

    private static List<String> suggestions(EquipmentDropConfig.Rarity rarity) {
        List<String> all = new ArrayList<>(rarity.tablePatterns);
        all.addAll(rarity.tables);
        all.addAll(rarity.entities);
        all.addAll(rarity.structures);
        return all;
    }

    private static String slug(String raw) {
        return raw.replace(':', '_').replace('/', '_').replace('.', '_').replace('#', '_');
    }

    public static List<Option> summons() {
        return summons(SummonTuning::get);
    }

    public static List<Option> summonDefaults() {
        SummonTuning.Values defaults = new SummonTuning.Values();
        return summons(() -> defaults);
    }

    private static List<Option> summons(Supplier<SummonTuning.Values> source) {
        List<Option> options = new ArrayList<>();
        options.add(decimal("summons.follow_start_distance", "Follow start distance",
                () -> source.get().followStartDistance,
                value -> source.get().followStartDistance = value)
                .describedAs("Summons walk back to you once they are this many blocks away.")
                .in(SUMMONS));
        options.add(decimal("summons.follow_stop_distance", "Follow stop distance",
                () -> source.get().followStopDistance,
                value -> source.get().followStopDistance = value)
                .describedAs("Summons stop following once they are this close to you.")
                .in(SUMMONS));
        options.add(decimal("summons.teleport_distance", "Teleport distance",
                () -> source.get().teleportDistance,
                value -> source.get().teleportDistance = value)
                .describedAs("Summons teleport next to you past this distance, even mid-fight. 0 never teleports.")
                .in(SUMMONS));
        options.add(decimal("summons.hunt_range", "Hunt range",
                () -> source.get().huntRange,
                value -> source.get().huntRange = value)
                .describedAs("Summons only chase targets within this many blocks.")
                .in(SUMMONS));
        options.add(decimal("summons.aim_range", "Aim range",
                () -> source.get().aimRange,
                value -> source.get().aimRange = value)
                .describedAs("How far summon abilities look for a target when they aim at the nearest, weakest, looked-at or last-hit enemy.")
                .in(SUMMONS));
        options.add(decimal("summons.roster_radius", "Roster radius",
                () -> source.get().rosterRadius,
                value -> source.get().rosterRadius = value)
                .describedAs("How far around you summons count for commands and limits.")
                .in(SUMMONS));
        options.add(bool("summons.recall_strays", "Recall strays",
                () -> source.get().recallStrays,
                value -> source.get().recallStrays = value)
                .describedAs("Bring summons back when they wander past the roster radius.")
                .in(SUMMONS));
        options.add(integer("summons.max_commanded_per_order", "Max commanded per order",
                () -> source.get().maxCommandedPerOrder,
                value -> source.get().maxCommandedPerOrder = value)
                .describedAs("Most summons a single recall or focus order moves.")
                .in(SUMMONS));
        options.add(decimal("summons.minimum_damage", "Minimum damage",
                () -> (double) source.get().minimumDamage,
                value -> source.get().minimumDamage = value.floatValue())
                .describedAs("Summon hits below this damage are skipped.")
                .in(SUMMONS));
        return options;
    }

    public static Option action(String key, String label, Runnable action) {
        return new Option(GENERAL, key, label, "", true, "", action, () -> "", raw -> {
        });
    }

    public static Option text(String key, String label,
                              Supplier<String> reader, Consumer<String> writer) {
        return new Option(key, label,
                () -> reader.get() == null ? "" : reader.get(),
                raw -> writer.accept(raw.trim()));
    }

    public static Option bool(String key, String label,
                              Supplier<Boolean> reader, Consumer<Boolean> writer) {
        return new Option(key, label,
                () -> reader.get() ? TRUE : FALSE,
                raw -> writer.accept(Boolean.parseBoolean(raw.trim())))
                .suggesting(KIND_BOOL);
    }

    public static Option integer(String key, String label,
                                 Supplier<Integer> reader, Consumer<Integer> writer) {
        return new Option(key, label,
                () -> String.valueOf(reader.get()),
                raw -> {
                    try {
                        writer.accept(Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                });
    }

    public static Option decimal(String key, String label,
                                 Supplier<Double> reader, Consumer<Double> writer) {
        return new Option(key, label,
                () -> String.valueOf(reader.get()),
                raw -> {
                    try {
                        writer.accept(Double.parseDouble(raw.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                });
    }
}
