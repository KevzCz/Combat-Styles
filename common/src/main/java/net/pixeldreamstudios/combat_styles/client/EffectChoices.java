package net.pixeldreamstudios.combat_styles.client;

import net.pixeldreamstudios.combat_styles.equipment.ItemMatch;
import net.pixeldreamstudios.combat_styles.equipment.DealtGate;
import net.pixeldreamstudios.combat_styles.equipment.SummonCadence;
import net.pixeldreamstudios.combat_styles.entity.BeamLook;
import net.pixeldreamstudios.combat_styles.entity.ParticleShape;
import net.pixeldreamstudios.combat_styles.entity.RaiseProfile;
import net.pixeldreamstudios.combat_styles.entity.RaiseRules;
import net.pixeldreamstudios.combat_styles.entity.BeamOrigin;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;
import net.pixeldreamstudios.combat_styles.entity.SummonCastTarget;
import net.pixeldreamstudios.combat_styles.entity.PassiveTrigger;
import net.pixeldreamstudios.combat_styles.equipment.SummonPassive;
import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.GrantTarget;
import net.pixeldreamstudios.combat_styles.equipment.SummonActives;
import net.pixeldreamstudios.combat_styles.entity.AbilityKind;
import net.pixeldreamstudios.combat_styles.entity.MobFilter;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbility;
import net.pixeldreamstudios.combat_styles.entity.AbilityPick;
import net.pixeldreamstudios.combat_styles.entity.AbilityFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.combat_styles.loot.BookRoll;
import net.pixeldreamstudios.combat_styles.loot.DropPool;
import net.pixeldreamstudios.combat_styles.loot.WeaponRoll;
import net.pixeldreamstudios.combat_styles.affix.CastSpell;
import net.pixeldreamstudios.combat_styles.affix.LibraryRef;
import net.pixeldreamstudios.combat_styles.equipment.EntityLibraries;
import net.pixeldreamstudios.combat_styles.damage.CritGroups;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumber;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.SpawnAnchor;
import net.pixeldreamstudios.combat_styles.affix.SpreadMode;
import net.pixeldreamstudios.combat_styles.api.SchemaRegistry;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.equipment.AttackConditions;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.CastGate;
import net.pixeldreamstudios.combat_styles.equipment.CastPhase;
import net.pixeldreamstudios.combat_styles.equipment.EffectStacking;
import net.pixeldreamstudios.combat_styles.equipment.StateGate;
import net.pixeldreamstudios.combat_styles.equipment.SummonGate;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryMatch;
import net.pixeldreamstudios.combat_styles.equipment.ItemGate;
import net.pixeldreamstudios.combat_styles.equipment.UsePhase;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.custom.CustomRegistry;
import net.pixeldreamstudios.combat_styles.custom.CustomUse;
import net.pixeldreamstudios.combat_styles.custom.Customs;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public final class EffectChoices {

    private static final String KEY_SCALING = "scaling";
    private static final String KEY_CONDITION = "condition";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_SCALES_WITH = "scales_with";
    private static final String KEY_TARGETS = "targets";
    private static final String KEY_ORDER = "order";
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
    private static final List<String> STEP_PHASES = List.of("start", EffectSchema.KEY_AT_WINDUP, "end");

    private EffectChoices() {
    }

    private static final List<String> ENTITY_KEYS = List.of("entity", "summon", EffectSchema.KEY_USED_BY,
            "become_entity", "attacker", "replaced_summoned_entities", "entities", "target",
            SummonGate.ENTITIES);
    private static final String PATH_SEPARATOR = "/";
    private static final String EQUIPMENT_SEGMENT = "equipment";
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

    public static boolean picksScalingSource(String key) {
        return KEY_SOURCE.equals(key) || KEY_SCALES_WITH.equals(key);
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

    private static Supplier<? extends Collection<String>> projectileNames = List::of;
    private static Supplier<? extends Collection<String>> summonProfileNames = List::of;
    private static Supplier<? extends Collection<String>> critNames = List::of;

    public static void offerLibraries(Supplier<? extends Collection<String>> projectiles,
                                      Supplier<? extends Collection<String>> summonProfiles,
                                      Supplier<? extends Collection<String>> crits) {
        projectileNames = projectiles == null ? List::of : projectiles;
        summonProfileNames = summonProfiles == null ? List::of : summonProfiles;
        critNames = crits == null ? List::of : crits;
    }

    private static List<String> critRefs() {
        List<String> refs = new ArrayList<>(refsTo(critNames));
        for (String id : CritGroups.globalIds()) {
            String ref = LibraryRef.refTo(id);
            if (!refs.contains(ref)) {
                refs.add(ref);
            }
        }
        return refs;
    }

    private static List<String> refsTo(Supplier<? extends Collection<String>> names) {
        return names.get().stream().map(LibraryRef::refTo).toList();
    }

    private static String parentOf(String path) {
        int cut = path == null ? -1 : path.lastIndexOf(PATH_SEPARATOR);
        return cut < 0 ? "" : path.substring(0, cut);
    }

    private static String keyOf(String path) {
        int cut = path == null ? -1 : path.lastIndexOf(PATH_SEPARATOR);
        return path == null ? "" : (cut < 0 ? path : path.substring(cut + 1));
    }

    private static String ownerOf(String scope) {
        return keyOf(parentOf(scope));
    }

    private static boolean within(String scope, String segment) {
        for (String part : parentOf(scope).split(PATH_SEPARATOR)) {
            if (part.equals(segment)) {
                return true;
            }
        }
        return false;
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
        if (within(scope, EQUIPMENT_SEGMENT) && EffectSchema.SUMMON_SLOTS.contains(key)) {
            return itemIds();
        }
        return switch (key) {
            case LibraryRef.PROJECTILE_KEY -> within(scope, EntityLibraries.PROJECTILES)
                    ? refsTo(projectileNames) : List.of();
            case LibraryRef.SUMMON_KEY -> refsTo(summonProfileNames);
            case ScaledNumber.Crit.KEY_GROUP -> within(scope, ScaledNumber.KEY_CRIT)
                    ? critRefs() : List.of();
            case AffixEffects.Condition.KEY_CRIT, AffixEffects.Condition.KEY_CRIT_MISSED ->
                    within(scope, KEY_CONDITION) ? critRefs() : List.of();
            case "hitbox" -> EffectSchema.HITBOX_SHAPES;
            case EffectSchema.KEY_CONDITIONS -> EffectSchema.KEY_OVERRIDE.equals(ownerOf(scope))
                    ? AttackConditions.names()
                    : List.of();
            case "phase" -> within(scope, CastGate.KEY)
                    ? names(CastPhase.values())
                    : names(UsePhase.values());
            case "cast_type" -> SpellEngineCompat.castTypeNames();
            case "slot_band" -> names(SlotBand.values());
            case "rarity" -> names(StyleRarity.values());
            case "anchor" -> within(scope, KEY_SPAWN)
                    ? names(SpawnAnchor.values())
                    : names(SlashAnchor.values());
            case "operation" -> ATTRIBUTE_OPERATIONS;
            case EffectSchema.KEY_SLOT -> {
                if (within(scope, ItemGate.ACCESSORIES)) {
                    yield accessorySlotIds();
                }
                yield AttributeBonus.SLOTS;
            }
            case "mode" -> {
                if (ScaledNumber.KEY_CRIT.equals(ownerOf(scope))) {
                    yield ScaledNumber.Crit.Mode.names();
                }
                if (scope.contains(EffectSchema.ROOT_STYLE_BOOK)) {
                    yield BookRoll.Mode.NAMES;
                }
                if (scope.contains(EffectSchema.ROOT_WEAPON_ROLL)) {
                    yield WeaponRoll.MODES;
                }
                if (within(scope, ScaledNumber.KEY_BY)) {
                    yield EffectSchema.SCALING_MODES;
                }
                if (within(scope, KEY_SPAWN)) {
                    yield names(SpreadMode.values());
                }
                if (within(scope, KEY_PLACEMENT)) {
                    yield names(AttackPlacement.Mode.values());
                }
                if (within(scope, KEY_SCALING)) {
                    yield EffectSchema.SCALING_MODES;
                }
                if (scope.contains(EffectSchema.ROOT_STACKING)) {
                    yield names(EffectStacking.Mode.values());
                }
                yield scope.contains(EffectSchema.ROOT_SUBSTITUTION) || within(scope, KEY_SUBSTITUTE)
                        ? SUBSTITUTION_MODES
                        : EffectSchema.DAMAGE_MODES;
            }
            case "apply" -> scope.contains(EffectSchema.ROOT_SUBSTITUTION)
                    || within(scope, KEY_SUBSTITUTE)
                    ? SUBSTITUTION_APPLY
                    : List.of();
            case "style_id", "substitute_style", "custom_style_id" -> styleIds();
            case "affix_id", "affix", "affixes", "grant_affixes" -> affixIds();
            case "target" -> entityIdsAndTags();
            case "entity", "summon", "become_entity", "attacker",
                    "replaced_summoned_entities", SummonGate.ENTITIES -> entityIds();
            case "attribute" -> attributeIds();
            case KEY_SOURCE, KEY_SCALES_WITH -> scalingSourceIds();
            case "damage_type", "taken_damage_type" -> damageTypeIds();
            case "type" -> {
                if (EffectSchema.KEY_ACTION.equals(ownerOf(scope))
                        && scope.contains(EffectSchema.ROOT_SUMMON_ABILITY)) {
                    yield names(ScriptedAbility.ActionType.values());
                }
                yield within(scope, KEY_PARTICLES) ? particleIds() : damageTypeIds();
            }
            case SummonCadence.KEY_ABILITY_PICK -> names(AbilityPick.values());
            case SummonCadence.KEY_AIM -> names(SummonAim.values());
            case ScriptedAbility.Action.KEY_SPELL_TARGET -> names(SummonCastTarget.values());
            case RaiseRules.KEY_PROFILE -> names(RaiseProfile.values());
            case CustomUse.KEY_HOOK -> within(scope, Customs.KEY)
                    ? CustomRegistry.idsFor(EffectSchema.customScopeAt(scope))
                    : List.of();
            case AbilityGrant.KEY_APPLIES_TO -> scope.contains(EffectSchema.ROOT_ABILITY_GRANT)
                    ? names(GrantTarget.values())
                    : List.of();
            case ParticleShape.KEY_FORM -> ParticleShape.KEY.equals(ownerOf(scope))
                    ? names(ParticleShape.Form.values())
                    : List.of();
            case BeamLook.KEY_ORIGIN -> BeamLook.KEY.equals(ownerOf(scope))
                    ? names(BeamOrigin.values())
                    : List.of();
            case SummonPassive.KEY_ON -> within(scope, SummonCadence.KEY_PASSIVES)
                    || scope.contains(EffectSchema.ROOT_SUMMON_ABILITY)
                    ? names(PassiveTrigger.values())
                    : List.of();
            case EffectSchema.KEY_FOCUS -> scope.contains(EffectSchema.ROOT_SUMMON_ABILITY)
                    ? names(AbilityFocus.values())
                    : List.of();
            case EffectSchema.KEY_AT, ScriptedAbility.Step.KEY_UNTIL ->
                    scope.contains(EffectSchema.ROOT_SUMMON_ABILITY)
                    ? STEP_PHASES
                    : List.of();
            case EffectSchema.KEY_PARTICLE -> scope.contains(EffectSchema.ROOT_SUMMON_ABILITY)
                    ? particleIds()
                    : List.of();
            case EffectSchema.KEY_USED_BY -> entityIds();
            case "school", "disable_schools", "impact_school", StateGate.HAS_SPELL_SCHOOL -> spellSchoolIds();
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
            case "entities", MobFilter.KEY_EXEMPT_ENTITIES -> entityIds();
            case MobFilter.KEY_ENTITY_TAGS, MobFilter.KEY_EXEMPT_TAGS ->
                    DropSuggestions.of(DropSuggestions.ENTITY_TAGS);
            case "effect", DealtGate.KEY_TARGET_EFFECTS -> effectIds();
            case "holding", "wearing", "equipped" -> itemIds();
            case "items", ItemMatch.EXCLUDED_ITEMS -> itemIds();
            case ItemMatch.EXCLUDED_TAGS -> itemTagIds();
            case ItemMatch.EXCLUDED_CATEGORIES -> categoryIds();
            case SummonCadence.KEY_PASSIVES -> passiveAbilityIds();
            case "applies" -> effectIds();
            case "tags" -> scope.contains(EffectSchema.ROOT_WEAPON_ROLL) ? itemTagIds() : List.of();
            case DropPool.KEY_TABLE_PATTERNS -> scope.contains(EffectSchema.ROOT_DROP_POOL)
                    ? DropSuggestions.of(DropSuggestions.TABLE_PATTERNS) : List.of();
            case DropPool.KEY_STRUCTURES -> scope.contains(EffectSchema.ROOT_DROP_POOL)
                    ? DropSuggestions.of(DropSuggestions.STRUCTURES) : List.of();
            case DropPool.KEY_STRUCTURE_TAGS -> scope.contains(EffectSchema.ROOT_DROP_POOL)
                    ? DropSuggestions.of(DropSuggestions.STRUCTURE_TAGS) : List.of();
            case "tables" -> scope.contains(EffectSchema.ROOT_WEAPON_ROLL)
                    || scope.contains(EffectSchema.ROOT_DROP_POOL)
                    ? DropSuggestions.of(DropSuggestions.LOOT_TABLES)
                    : List.of();
            case "style" -> scope.contains(EffectSchema.ROOT_STYLE_BOOK)
                    || scope.contains(EffectSchema.ROOT_WEAPON_ROLL)
                    ? styleIds()
                    : List.of();
            case "ability" -> {
                if (within(scope, SummonCadence.KEY_PASSIVES)) {
                    yield passiveAbilityIds();
                }
                yield within(scope, SummonActives.KEY) ? activeAbilityIds() : abilityIds();
            }
            case "abilities" -> within(scope, SummonActives.KEY) ? activeAbilityIds() : abilityIds();
            case KEY_TARGETS -> within(scope, KEY_ORDER) ? targetingNames() : List.of();
            case "finale" -> activeAbilityIds();
            case SummonActives.KEY_PICK -> within(scope, SummonActives.KEY)
                    ? names(AbilityPick.values())
                    : List.of();
            case ScriptedAbility.KEY_KIND -> scope.contains(EffectSchema.ROOT_SUMMON_ABILITY)
                    ? names(AbilityKind.values())
                    : List.of();
            case "sound", "swing_sound" -> soundIds();
            case "id" -> within(scope, KEY_SOUND) ? soundIds() : List.of();
            case "particle_type" -> particleIds();
            case "impact_type" -> SpellEngineCompat.impactTypeNames();
            default -> List.of();
        };
    }

    private static List<String> targetingNames() {
        List<String> names = new ArrayList<>();
        for (SummonTargeting targeting : SummonTargeting.values()) {
            names.add(targeting.key());
        }
        return names;
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
        for (StylePage page : StyleRegistry.distinct()) {
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

    private static List<String> entityIdsAndTags() {
        List<String> ids = new ArrayList<>(entityIds());
        BuiltInRegistries.ENTITY_TYPE.getTagNames()
                .map(tag -> MobFilter.TAG_MARKER + tag.location())
                .sorted()
                .forEach(ids::add);
        return List.copyOf(ids);
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

    private static List<String> activeAbilityIds() {
        List<String> ids = new ArrayList<>();
        for (String id : abilityIds()) {
            if (!AbilityRegistry.passive(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static List<String> passiveAbilityIds() {
        List<String> ids = new ArrayList<>();
        for (String id : abilityIds()) {
            if (AbilityRegistry.passive(id)) {
                ids.add(id);
            }
        }
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
        return ScalingSources.ids();
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
