package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.combat_styles.loot.BookRoll;
import net.pixeldreamstudios.combat_styles.loot.WeaponRoll;
import net.pixeldreamstudios.combat_styles.affix.CastSpell;
import net.pixeldreamstudios.combat_styles.affix.SpawnAnchor;
import net.pixeldreamstudios.combat_styles.affix.SpreadMode;
import net.pixeldreamstudios.combat_styles.api.SchemaRegistry;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.CastGate;
import net.pixeldreamstudios.combat_styles.equipment.CastPhase;
import net.pixeldreamstudios.combat_styles.equipment.SummonGate;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryMatch;
import net.pixeldreamstudios.combat_styles.equipment.ItemGate;
import net.pixeldreamstudios.combat_styles.equipment.UsePhase;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.equipment.SpellLocks;
import net.pixeldreamstudios.combat_styles.equipment.SlotBand;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.AttackPlacement;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.SlashAnchor;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class EffectChoices {

    private static final String KEY_SCALING = "scaling";
    private static final String KEY_PARTICLES = "particles";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_PLACEMENT = "placement";
    private static final String KEY_SPAWN = "spawn";
    private static final String KEY_GRANT = "grant";
    private static final String KEY_DISABLE = "disable";
    private static final String TAG_MARKER = "#";
    private static final String SPELL_SCHOOL_PREFIX = SpellPowerAccess.MOD_ID + ":";
    private static final List<String> SUBSTITUTION_MODES = List.of(
            StyleSubstitution.MODE_REPLACE_ALL,
            StyleSubstitution.MODE_REPLACE_SLOT,
            StyleSubstitution.MODE_APPEND);
    private static final List<String> SUBSTITUTION_APPLY = List.of(
            StyleSubstitution.APPLY_ON_SLOT,
            StyleSubstitution.APPLY_AFTER_SLOT);
    private static final List<String> ATTRIBUTE_OPERATIONS =
            List.of("add_value", "multiply_base", "multiply_total");
    private static final String KEY_SUBSTITUTE = "substitute";

    private EffectChoices() {
    }

    private static final List<String> ENTITY_KEYS = List.of("entity", "summon",
            "become_entity", "attacker", "replaced_summoned_entities", "entities", "target",
            SummonGate.ENTITIES);
    private static final String EQUIPMENT_SEGMENT = "/equipment";
    private static final String ENTITY_KEY = "entity";
    private static final List<String> ABILITY_KEYS = List.of("ability", "finale", "abilities");

    private static final Set<String> ANY_WHEN_EMPTY = Set.of("damage_type", "taken_damage_type",
            "attacker", "target", "entity", "spell", "school", "replaced_summoned_entities",
            "entities", "impact_type", "impact_school", "cast_type", "weapon_categories",
            "item_tags", SummonGate.ENTITIES);
    private static final Set<String> ANY_WHEN_ABSENT = Set.of("hitbox", "style_id", "affix_id",
            "slot_band");
    private static final List<Set<String>> ANY_TOGETHER = List.of(
            Set.of("weapon_categories", "item_tags"));

    public static boolean picksEntity(String key) {
        return key != null && ENTITY_KEYS.contains(key);
    }

    public static boolean anyWhenEmpty(String listPath, String listKey) {
        return listKey != null && ANY_WHEN_EMPTY.contains(listKey)
                && EffectSchema.matchListsAt(listPath, listKey) == null;
    }

    public static boolean anyWhenAbsent(String path, String key) {
        return key != null && ANY_WHEN_ABSENT.contains(key)
                && path != null && path.startsWith(EffectSchema.ROOT_TRIGGER);
    }

    public static String anyValueFor(String path, String key) {
        return EffectSchema.KEY_SLOT.equals(key) && path != null
                && path.contains(ItemGate.ACCESSORIES)
                ? AccessoryMatch.ANY_SLOT
                : null;
    }

    public static Set<String> anyTogetherWith(String listKey) {
        for (Set<String> keys : ANY_TOGETHER) {
            if (keys.contains(listKey)) {
                return keys;
            }
        }
        return Set.of(listKey);
    }

    public static boolean picksSpell(String key) {
        return EffectSchema.KEY_SPELL.equals(key) || KEY_GRANT.equals(key) || KEY_DISABLE.equals(key);
    }

    public static List<String> choicesFor(String key) {
        return choicesFor(EffectSchema.ROOT_EFFECT, key);
    }

    private static String parentOf(String path) {
        int cut = path == null ? -1 : path.lastIndexOf('/');
        return cut < 0 ? "" : path.substring(0, cut);
    }

    private static String keyOf(String path) {
        int cut = path == null ? -1 : path.lastIndexOf('/');
        return path == null ? "" : (cut < 0 ? path : path.substring(cut + 1));
    }

    public static List<String> choicesFor(String path, String key) {
        if (key == null) {
            return List.of();
        }
        String scope = path == null ? "" : path;
        if (EffectSchema.isIndex(key)) {
            return choicesFor(parentOf(scope), keyOf(parentOf(scope)));
        }
        if (SchemaRegistry.hasChoices(key)) {
            return SchemaRegistry.choicesFor(key);
        }
        if (scope.contains(EQUIPMENT_SEGMENT) && EffectSchema.SUMMON_SLOTS.contains(key)) {
            return itemIds();
        }
        return switch (key) {
            case "hitbox" -> EffectSchema.HITBOX_SHAPES;
            case "phase" -> scope.contains(CastGate.KEY)
                    ? names(CastPhase.values())
                    : names(UsePhase.values());
            case "cast_type" -> SpellEngineCompat.castTypeNames();
            case "slot_band" -> names(SlotBand.values());
            case "rarity" -> names(StyleRarity.values());
            case "anchor" -> scope.contains(KEY_SPAWN)
                    ? names(SpawnAnchor.values())
                    : names(SlashAnchor.values());
            case "operation" -> ATTRIBUTE_OPERATIONS;
            case EffectSchema.KEY_SLOT -> {
                if (scope.contains(ItemGate.ACCESSORIES)) {
                    yield accessorySlotIds();
                }
                yield scope.contains(EffectSchema.ROOT_ATTRIBUTE_MAP)
                        || scope.contains(EffectSchema.ROOT_CONDITIONAL_ATTRIBUTES)
                        ? AttributeBonus.SLOTS
                        : List.of();
            }
            case "mode" -> {
                if (scope.contains(EffectSchema.ROOT_STYLE_BOOK)) {
                    yield BookRoll.Mode.NAMES;
                }
                if (scope.contains(EffectSchema.ROOT_WEAPON_ROLL)) {
                    yield WeaponRoll.MODES;
                }
                if (scope.contains(KEY_SPAWN)) {
                    yield names(SpreadMode.values());
                }
                if (scope.contains(KEY_PLACEMENT)) {
                    yield names(AttackPlacement.Mode.values());
                }
                if (scope.contains(KEY_SCALING)) {
                    yield EffectSchema.SCALING_MODES;
                }
                yield scope.contains(EffectSchema.ROOT_SUBSTITUTION) || scope.contains(KEY_SUBSTITUTE)
                        ? SUBSTITUTION_MODES
                        : EffectSchema.DAMAGE_MODES;
            }
            case "apply" -> scope.contains(EffectSchema.ROOT_SUBSTITUTION)
                    || scope.contains(KEY_SUBSTITUTE)
                    ? SUBSTITUTION_APPLY
                    : List.of();
            case "style_id", "substitute_style", "custom_style_id" -> styleIds();
            case "affix_id", "affix", "affixes", "grant_affixes" -> affixIds();
            case "entity", "summon", "become_entity", "attacker",
                    "replaced_summoned_entities", "target", SummonGate.ENTITIES -> entityIds();
            case "attribute" -> attributeIds();
            case "source", "scales_with" -> scalingSourceIds();
            case "damage_type", "taken_damage_type" -> damageTypeIds();
            case "type" -> scope.contains(KEY_PARTICLES)
                    ? particleIds()
                    : damageTypeIds();
            case "school", "disable_schools", "impact_school" -> spellSchoolIds();
            case EffectSchema.KEY_SPELL, "grant", "disable" -> spellIds();
            case "disable_mode" -> names(SpellLocks.Mode.values());
            case "cast_target" -> names(CastSpell.Target.values());
            case "scaling_mode" -> EffectSchema.SCALING_COMBINES;
            case "resolve" -> names(HitboxSet.Resolve.values());
            case "animation", "animations" -> animationIds();
            case "weapon_categories", "categories" -> categoryIds();
            case "item_tags" -> itemTagIds();
            case "styles" -> styleIds();
            case "exempt" -> scope.contains("summon")
                    ? entityIds()
                    : styleIds();
            case "entities" -> entityIds();
            case "effect" -> effectIds();
            case "holding", "wearing", "equipped" -> itemIds();
            case "items" -> scope.contains(EffectSchema.ROOT_WEAPON_ROLL) ? itemIds() : List.of();
            case "tags" -> scope.contains(EffectSchema.ROOT_WEAPON_ROLL) ? itemTagIds() : List.of();
            case "tables" -> scope.contains(EffectSchema.ROOT_WEAPON_ROLL)
                    ? DropSuggestions.of(DropSuggestions.LOOT_TABLES)
                    : List.of();
            case "style" -> scope.contains(EffectSchema.ROOT_STYLE_BOOK)
                    || scope.contains(EffectSchema.ROOT_WEAPON_ROLL)
                    ? styleIds()
                    : List.of();
            case "ability", "finale", "abilities" -> abilityIds();
            case "sound", "swing_sound" -> soundIds();
            case "id" -> scope.contains(KEY_SOUND) ? soundIds() : List.of();
            case "particle_type" -> particleIds();
            case "impact_type" -> SpellEngineCompat.impactTypeNames();
            default -> List.of();
        };
    }

    private static List<String> names(StringRepresentable[] values) {
        List<String> names = new ArrayList<>();
        for (StringRepresentable value : values) {
            names.add(value.getSerializedName());
        }
        return names;
    }

    private static List<String> styleIds() {
        Set<String> ids = new TreeSet<>();
        for (StylePage page : StyleRegistry.all()) {
            ids.add(page.styleId());
        }
        for (CustomStyle style : CustomStyles.all()) {
            ids.add(style.id());
        }
        return List.copyOf(ids);
    }

    private static List<String> affixIds() {
        return AffixRegistry.all().stream().map(StyleAffix::serializedId).sorted().toList();
    }

    private static List<String> entityIds() {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
    }

    static List<String> attributeIds() {
        return BuiltInRegistries.ATTRIBUTE.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
    }

    private static List<String> effectIds() {
        return BuiltInRegistries.MOB_EFFECT.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
    }

    public static List<String> narrowedFor(String key, List<String> choices, String entity) {
        if (!ABILITY_KEYS.contains(key) || entity == null || entity.isBlank()) {
            return choices;
        }
        return choices.stream().filter(ability -> AbilityRegistry.usableBy(ability, entity)).toList();
    }

    public static String entityKey() {
        return ENTITY_KEY;
    }

    private static List<String> abilityIds() {
        List<String> ids = new ArrayList<>(AbilityRegistry.registered());
        ids.sort(String::compareTo);
        return ids;
    }

    private static List<String> itemIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
    }

    private static List<String> spellIds() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return List.of();
        }
        return SpellEngineCompat.spellIds(client.level).stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    private static List<String> scalingSourceIds() {
        Set<String> ids = new TreeSet<>(attributeIds());
        ids.addAll(SpellPowerAccess.schoolIds());
        return List.copyOf(ids);
    }

    private static List<String> spellSchoolIds() {
        List<String> registered = SpellPowerAccess.schoolIds();
        if (!registered.isEmpty()) {
            return registered;
        }
        return BuiltInRegistries.ATTRIBUTE.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.startsWith(SPELL_SCHOOL_PREFIX))
                .map(id -> id.substring(SPELL_SCHOOL_PREFIX.length()))
                .sorted()
                .toList();
    }

    public static List<String> damageTypeIds() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        client.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(ids::add);
        client.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getTagNames()
                .map(tag -> TAG_MARKER + tag.location())
                .sorted()
                .forEach(ids::add);
        return List.copyOf(ids);
    }

    private static List<String> categoryIds() {
        return WeaponCategories.allNames();
    }

    private static List<String> animationIds() {
        return AnimationCatalogue.all();
    }

    private static List<String> accessorySlotIds() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return List.of();
        }
        return AccessoryCompat.slotIds(client.level);
    }

    private static List<String> itemTagIds() {
        return BuiltInRegistries.ITEM.getTagNames()
                .map(tag -> TAG_MARKER + tag.location())
                .sorted()
                .toList();
    }

    private static List<String> soundIds() {
        return BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
    }

    private static List<String> particleIds() {
        return BuiltInRegistries.PARTICLE_TYPE.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
    }
}
