package net.pixeldreamstudios.combat_styles.client;

import net.pixeldreamstudios.combat_styles.equipment.SummonCadence;
import net.pixeldreamstudios.combat_styles.entity.BeamLook;
import net.pixeldreamstudios.combat_styles.entity.RaiseProfile;
import net.pixeldreamstudios.combat_styles.entity.BeamOrigin;
import net.pixeldreamstudios.combat_styles.entity.ParticleShape;
import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.AbilityUse;
import net.pixeldreamstudios.combat_styles.equipment.GrantTarget;
import net.pixeldreamstudios.combat_styles.equipment.StepTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonPassive;
import net.pixeldreamstudios.combat_styles.equipment.SummonActives;
import net.pixeldreamstudios.combat_styles.entity.AbilityKind;
import net.pixeldreamstudios.combat_styles.entity.MobFilter;
import net.pixeldreamstudios.combat_styles.entity.RaiseRules;
import net.pixeldreamstudios.combat_styles.entity.PassiveTrigger;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;
import net.pixeldreamstudios.combat_styles.entity.SummonCastTarget;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbility;
import net.pixeldreamstudios.combat_styles.entity.AbilityStacks;
import net.pixeldreamstudios.combat_styles.entity.AbilityPick;
import net.pixeldreamstudios.combat_styles.entity.AbilityFocus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.sounds.SoundEvents;

import net.pixeldreamstudios.combat_styles.equipment.AttackConditions;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.CastSpell;
import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.damage.DamageBonusList;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.affix.ProjectileSpawn;
import net.pixeldreamstudios.combat_styles.affix.ProjectileVelocity;
import net.pixeldreamstudios.combat_styles.affix.SpawnAnchor;
import net.pixeldreamstudios.combat_styles.affix.SpreadMode;
import net.pixeldreamstudios.combat_styles.api.SchemaRegistry;
import net.pixeldreamstudios.combat_styles.config.SpellCompatState;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.config.OptionField;
import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.entity.SummonCompanion;
import net.pixeldreamstudios.combat_styles.entity.SummonEquipment;
import net.pixeldreamstudios.combat_styles.entity.SummonFollow;
import net.pixeldreamstudios.combat_styles.entity.SummonStats;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryMatch;
import net.pixeldreamstudios.combat_styles.equipment.ItemGate;
import net.pixeldreamstudios.combat_styles.equipment.ItemMatch;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.ProfileCondition;
import net.pixeldreamstudios.combat_styles.equipment.ProfileVariant;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;
import net.pixeldreamstudios.combat_styles.equipment.HealthGate;
import net.pixeldreamstudios.combat_styles.equipment.BlockGate;
import net.pixeldreamstudios.combat_styles.equipment.HurtGate;
import net.pixeldreamstudios.combat_styles.equipment.ShootGate;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileHitGate;
import net.pixeldreamstudios.combat_styles.equipment.CastGate;
import net.pixeldreamstudios.combat_styles.equipment.CastPhase;
import net.pixeldreamstudios.combat_styles.equipment.PhaseGate;
import net.pixeldreamstudios.combat_styles.equipment.UseGate;
import net.pixeldreamstudios.combat_styles.equipment.UsePhase;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.equipment.SpellChainTuning;
import net.pixeldreamstudios.combat_styles.equipment.SpellLaunchTuning;
import net.pixeldreamstudios.combat_styles.equipment.SpellProjectileGate;
import net.pixeldreamstudios.combat_styles.equipment.SpellLocks;
import net.pixeldreamstudios.combat_styles.equipment.SpellShot;
import net.pixeldreamstudios.combat_styles.equipment.SpellTune;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;
import net.pixeldreamstudios.combat_styles.equipment.StateGate;
import net.pixeldreamstudios.combat_styles.loot.BookRoll;
import net.pixeldreamstudios.combat_styles.loot.WeaponRoll;
import net.pixeldreamstudios.combat_styles.equipment.EffectCooldown;
import net.pixeldreamstudios.combat_styles.equipment.SlotBand;
import net.pixeldreamstudios.combat_styles.equipment.SlotSelection;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.equipment.SummonDive;
import net.pixeldreamstudios.combat_styles.equipment.SummonGate;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackPlacement;
import net.pixeldreamstudios.combat_styles.style.InheritGroups;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.SlashAnchor;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EffectSchema {

    private static final String DEFAULT_SOUND =
            BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.PLAYER_ATTACK_SWEEP).toString();

    public enum Trigger {
        ON_HIT,
        ON_SWING,
        ANY
    }

    public record Template(String key, JsonElement value) {
    }

    private static final Map<String, Trigger> TRIGGERS = new LinkedHashMap<>();

    private static final String ATTACK_ATTRIBUTES = "attack_attributes";
    private static final String ROOT_EFFECT_LABEL = "<root>";
    static final String KEY_SPELL = "spell";
    private static final String KEY_TUNE = "tune";
    private static final String KEY_ITEMS = "items";
    static final String KEY_SLOT = AccessoryMatch.SLOT;
    public static final String KEY_OVERRIDE = "override";
    public static final String KEY_CONDITIONS = "conditions";
    public static final String KEY_SUMMON_EQUIPMENT = "equipment";
    private static final String KEY_ATTRIBUTES = "attributes";
    private static final Set<String> SPARSE_KEYS = Set.of(KEY_ITEMS, KEY_ATTRIBUTES,
            KEY_SUMMON_EQUIPMENT);
    private static final Set<String> OPT_IN_KEYS = Set.of(ItemMatch.EMPTY);
    private static final String KEY_CAST_SPELLS = "cast_spells";
    private static final int DEFAULT_CHANNEL_DURATION = 40;

    public static Trigger triggerOf(String key) {
        ensureLoaded();
        return TRIGGERS.getOrDefault(key, Trigger.ANY);
    }

    private static final String DEFAULT_ATTRIBUTE = "minecraft:generic.attack_damage";
    private static final String DEFAULT_OPERATION = "add_value";
    private static final String DEFAULT_STATUS_EFFECT = "minecraft:slowness";
    private static final String DEFAULT_PARTICLE = "minecraft:crit";
    private static final float DEFAULT_DASH_FORWARD = 1.5F;
    private static final float DEFAULT_BLINK_DISTANCE = 3.0F;
    private static final float DEFAULT_VAULT_RADIUS = 2.0F;
    private static final float DEFAULT_VAULT_STRENGTH = 0.5F;
    private static final float DEFAULT_HASTEN_RATIO = 0.5F;
    private static final String DEFAULT_SUMMON_ENTITY = "minecraft:bee";
    private static final String DEFAULT_SUMMON_ITEM = "minecraft:iron_sword";
    private static final String KEY_COMPANION = "companion";
    private static final String KEY_FOLLOW = "follow";
    private static final String KEY_DAMAGE_SPEC = "damage_spec";
    private static final String KEY_DAMAGE_TYPE = "damage_type";
    private static final String KEY_OWN_DAMAGE = "own_damage";
    public static final List<String> SUMMON_SLOTS = List.of("mainhand", "offhand", "head",
            "chest", "legs", "feet", "body");
    private static final String DEFAULT_PROJECTILE_ENTITY = "minecraft:arrow";
    private static final String DEFAULT_COMMAND = "say hello";
    private static final int DEFAULT_ECHO_DELAY = 10;
    private static final float DEFAULT_WAVE_RANGE = 8.0F;
    private static final float DEFAULT_WAVE_RADIUS = 2.0F;
    private static final float DEFAULT_WAVE_SPEED = 0.8F;


    public static final String ROOT_EFFECT = "";
    public static final String ROOT_TRIGGER = "@trigger";
    public static final String ROOT_SUBSTITUTION = "@substitution";
    public static final String ROOT_SUMMON_PROFILE = SummonProfile.SCHEMA;
    public static final String ROOT_ATTRIBUTE_MAP = "@attribute_map";
    public static final String ROOT_CONDITIONAL_ATTRIBUTES = "@conditional_attributes";
    public static final String ROOT_SUMMON_DIVE = "@summon_dive";
    public static final String ROOT_ABILITY_GRANT = "@ability_grant";
    public static final String ROOT_BEHAVIOUR = "@behaviour";
    public static final String ROOT_TUNING = "@tuning";
    public static final String ROOT_ECHO = "@echo";
    public static final String ROOT_SPELLS = "@spells";
    public static final String ROOT_COOLDOWN = "@cooldown";
    public static final String ROOT_TOOLTIP = "@tooltip";
    public static final String ROOT_SELF_EFFECTS = "@self_effects";
    public static final String ROOT_DAMAGE_BONUS = "@damage_bonus";
    public static final String ROOT_STYLE_BOOK = "@style_book";
    public static final String ROOT_WEAPON_ROLL = "@weapon_roll";
    public static final String ROOT_SUMMON_ABILITY = "@summon_ability";
    public static final String ROOT_ABILITY_TUNING = "@ability_tuning";
    private static final String PATH_SEPARATOR = "/";

    private static final String DEFAULT_TRAIL_COLOR = "#FFFFFF";
    private static final int DEFAULT_NTH_SWING = 3;
    private static final int DEFAULT_IDLE_TICKS = 60;
    private static final int DEFAULT_DAMAGED_WITHIN = 40;
    private static final int DEFAULT_HELD_TICKS = 20;
    private static final String DEFAULT_SPELL = "spell_engine:fireball";
    private static final double DEFAULT_SHOT_DISTANCE = 20.0D;
    private static final int DEFAULT_LINGER_TICKS = 100;
    private static final int DEFAULT_SUMMON_LIFESPAN = 200;
    private static final int DEFAULT_TUNING_COOLDOWN = 40;
    private static final int DEFAULT_EFFECT_TICKS = 100;
    private static final int DEFAULT_MAX_AMPLIFIER = 4;

    private static final AffixEffects.Damage SAMPLE_DAMAGE = new AffixEffects.Damage(
            AffixEffects.DamageMode.AMPLIFY, "", Optional.empty(), 0.2F, 0.0F,
            false, Optional.empty());

    private static final int DEFAULT_PROJECTILE_LIFETIME = 100;
    private static final String DEFAULT_SCHOOL = "arcane";
    private static final String DEFAULT_CAST_TYPE = "standard";
    private static final float DEFAULT_TRIGGER_CHANCE = 0.5F;
    private static final String DEFAULT_SCALING_SOURCE =
            SpellPowerAccess.MOD_ID + ":" + DEFAULT_SCHOOL;
    static final List<String> SCALING_MODES = List.of(
            DamageSpec.ScalingKind.PER_POINT.serializedName(),
            DamageSpec.ScalingKind.MULTIPLIER.serializedName(),
            DamageSpec.ScalingKind.DIVISOR.serializedName());
    static final List<String> SCALING_COMBINES = List.of(
            DamageSpec.ScalingMode.SUM.serializedName(),
            DamageSpec.ScalingMode.STRONGEST.serializedName());

    private static final DamageSpec.ScalingEntry SAMPLE_SCALING_ENTRY =
            new DamageSpec.ScalingEntry(DEFAULT_SCALING_SOURCE, 0.25F, Optional.empty(),
                    DamageSpec.ScalingKind.PER_POINT,
                    DamageSpec.ScalingEntry.DEFAULT_BASELINE);
    private static final String DEFAULT_DAMAGE_TYPE = SpellCompatState.MAGIC_SOURCE;
    private static final String DEFAULT_TAKEN_DAMAGE_TAG = "#minecraft:is_fire";
    private static final DamageSpec.Share SAMPLE_SHARE =
            new DamageSpec.Share(DEFAULT_DAMAGE_TYPE, 0.5F);
    private static final SummonFollow SAMPLE_FOLLOW = new SummonFollow(
            Optional.of(SummonTuning.get().followStartDistance),
            Optional.of(SummonTuning.get().followStopDistance),
            Optional.of(SummonTuning.get().teleportDistance));
    private static final DamageSpec SAMPLE_DAMAGE_SPEC = new DamageSpec(
            4.0F, 0.5F, DamageSpec.NO_HEALTH_RATIO, DamageSpec.NO_HEALTH_RATIO,
            DamageSpec.NO_HEALTH_RATIO, Optional.empty(), Optional.empty(),
            List.of(SAMPLE_SCALING_ENTRY), DamageSpec.ScalingMode.SUM, List.of(),
            Optional.empty(), Optional.empty(), DamageSpec.FULL_MULTIPLIER);
    private static final int DEFAULT_TUNING_WEIGHT = 10;

    static final List<String> HITBOX_SHAPES =
            List.of("HORIZONTAL_PLANE", "VERTICAL_PLANE", "FORWARD_BOX");

    public static final String KEY_TUNING = "tuning";
    public static final String KEY_FOCUS = "focus";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_AT = "at";
    public static final String KEY_ACTION = "action";
    public static final String KEY_AT_WINDUP = "windup";
    public static final String KEY_AT_END = "end";
    public static final String KEY_PARTICLE = "particle";
    public static final String KEY_USED_BY = "used_by";
    private static final int DEFAULT_RECAST_TICKS = 60;
    private static final int DEFAULT_STEP_EVERY = 5;
    private static final float DEFAULT_HEAL_RATIO = 0.25F;
    private static final float DEFAULT_STEP_CHANCE = 0.5F;
    private static final String DEFAULT_ENTITY_TAG = "minecraft:undead";
    private static final int DEFAULT_RAISE_MAX = 3;
    private static final float DEFAULT_TRIGGER_CHANCE_ADD = 0.1F;
    private static final String KEY_MODE = "mode";
    static final List<String> DAMAGE_MODES =
            List.of("additive", "amplify", "convert");

    private static final Map<String, List<Template>> TEMPLATES = new LinkedHashMap<>();

    private EffectSchema() {
    }

    private static <T> JsonElement encode(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value)
                .resultOrPartial(error -> CombatStyles.LOGGER.debug(
                        "Effect template encode failed: {}", error))
                .orElse(null);
    }

    private static <T> void register(String path, String key, Codec<T> codec, T sample) {
        JsonElement encoded = encode(codec, sample);
        if (encoded == null) {
            CombatStyles.LOGGER.error(
                    "Effect schema field {}/{} was dropped: its sample failed to encode",
                    path.isEmpty() ? ROOT_EFFECT_LABEL : path, key);
            return;
        }
        TEMPLATES.computeIfAbsent(path, ignored -> new ArrayList<>())
                .add(new Template(key, encoded));
    }

    private static final AffixEffects.Projectile SAMPLE_PROJECTILE = new AffixEffects.Projectile(
            DEFAULT_PROJECTILE_ENTITY, 1, 1.5F, 0.0F, 0.0F, false, 1.0F, Optional.empty(),
            DEFAULT_PROJECTILE_LIFETIME, false, ProjectileSpawn.DEFAULT, SpellShot.NONE, "", false);

    private static void registerProjectileFields(String projectileRoot) {
        register(projectileRoot, "entity", Codec.STRING, DEFAULT_PROJECTILE_ENTITY);
        register(projectileRoot, "name", Codec.STRING, "");
        register(projectileRoot, "ignore_iframes", Codec.BOOL, true);
        if (SpellEngineCompat.isLoaded()) {
            registerSpellShot(projectileRoot);
        }
        register(projectileRoot, "count", Codec.INT, 1);
        register(projectileRoot, "speed", Codec.FLOAT, 1.5F);
        register(projectileRoot, "spread", Codec.FLOAT, 0.25F);
        register(projectileRoot, "damage", Codec.FLOAT, 4.0F);
        register(projectileRoot, "from_target", Codec.BOOL, true);
        register(projectileRoot, "chance", Codec.FLOAT, 1.0F);
        register(projectileRoot, "lifetime_ticks", Codec.INT, DEFAULT_PROJECTILE_LIFETIME);
        register(projectileRoot, "remove_on_landing", Codec.BOOL, true);
        register(projectileRoot, "damage_spec", DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(projectileRoot, "damage_spec"));
        attachSpawn(projectileRoot);
    }

    private static void registerSpellShot(String projectileRoot) {
        register(projectileRoot, "spell_projectile", SpellShot.CODEC,
                new SpellShot(DEFAULT_SPELL, SpellTuning.NONE));
        String spellShotRoot = join(projectileRoot, "spell_projectile");
        register(spellShotRoot, "spell", Codec.STRING, DEFAULT_SPELL);
        registerSpellTuning(spellShotRoot);
    }

    private static void registerCastSpells(String effectRoot) {
        register(effectRoot, KEY_CAST_SPELLS, CastSpell.CODEC.listOf(),
                List.of(new CastSpell(DEFAULT_SPELL, CastSpell.Target.AUTO, 1.0F,
                        Optional.empty(), SpellTuning.NONE, false, "", CastSpell.SPELL_CHANNEL)));
        String castRoot = join(effectRoot, KEY_CAST_SPELLS);
        register(castRoot, KEY_SPELL, Codec.STRING, DEFAULT_SPELL);
        register(castRoot, "cast_target", CastSpell.Target.CODEC, CastSpell.Target.AUTO);
        register(castRoot, "chance", Codec.FLOAT, 1.0F);
        register(castRoot, "ignore_iframes", Codec.BOOL, true);
        register(castRoot, "name", Codec.STRING, "");
        register(castRoot, "channel_duration", Codec.INT, DEFAULT_CHANNEL_DURATION);
        register(castRoot, "damage_spec", DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(castRoot, "damage_spec"));
        registerSpellTuning(castRoot);
    }

    private static void registerItemGate(String root) {
        register(root, "dual_wielding", Codec.BOOL, true);
        register(root, "two_handed", Codec.BOOL, true);
        register(root, StateGate.PLAYER_TAGS, Codec.STRING.listOf(), List.of(""));
        register(root, StateGate.PLAYER_TAGS_MIN, Codec.INT, 1);
        register(root, StateGate.MISSING_PLAYER_TAGS, Codec.STRING.listOf(), List.of(""));
        register(root, KEY_ITEMS, ItemGate.CODEC, ItemGate.NONE);
        String itemsRoot = join(root, KEY_ITEMS);
        for (String slot : ItemGate.SLOTS) {
            register(itemsRoot, slot, ItemMatch.CODEC, ItemMatch.NONE);
            registerItemMatch(join(itemsRoot, slot));
        }
        if (AccessoryCompat.isLoaded()) {
            register(itemsRoot, ItemGate.ACCESSORIES, AccessoryMatch.CODEC.listOf(),
                    List.of(new AccessoryMatch(AccessoryMatch.ANY_SLOT, ItemMatch.NONE)));
            String accessoryRoot = join(itemsRoot, ItemGate.ACCESSORIES);
            register(accessoryRoot, KEY_SLOT, Codec.STRING, AccessoryMatch.ANY_SLOT);
            registerItemMatch(accessoryRoot);
        }
    }

    private static void registerItemMatch(String root) {
        for (ItemMatch.Lists lists : List.of(ItemMatch.PICK, ItemMatch.EXCLUDE)) {
            register(root, lists.items(), Codec.STRING.listOf(), List.of());
            register(root, lists.tags(), Codec.STRING.listOf(), List.of());
            register(root, lists.categories(), Codec.STRING.listOf(), List.of());
        }
        register(root, ItemMatch.EMPTY, Codec.BOOL, true);
    }

    public static ItemMatch.Lists matchListsAt(String path, String key) {
        List<String> segments = List.of((path == null ? "" : path).split("/"));
        if (segments.size() < 2 || !segments.subList(0, segments.size() - 1).contains(KEY_ITEMS)) {
            return null;
        }
        for (ItemMatch.Lists lists : List.of(ItemMatch.PICK, ItemMatch.EXCLUDE)) {
            if (key.equals(lists.items()) || key.equals(lists.tags()) || key.equals(lists.categories())) {
                return lists;
            }
        }
        return null;
    }

    private static void registerSpellLocks() {
        register(ROOT_SPELLS, "grant", Codec.STRING.listOf(), List.of(DEFAULT_SPELL));
        register(ROOT_SPELLS, "disable", Codec.STRING.listOf(), List.of(DEFAULT_SPELL));
        register(ROOT_SPELLS, "disable_schools", Codec.STRING.listOf(), List.of(DEFAULT_SCHOOL));
        register(ROOT_SPELLS, "disable_mode", SpellLocks.Mode.CODEC, SpellLocks.Mode.BLOCK);
        registerSpellTunes();
    }

    private static void registerSpellTuning(String spellRoot) {
        register(spellRoot, "tuning", SpellTuning.CODEC, SpellTuning.NONE);
        registerTuningFields(join(spellRoot, "tuning"));
    }

    private static void registerSpellTunes() {
        register(ROOT_SPELLS, KEY_TUNE, SpellTune.CODEC.listOf(),
                List.of(new SpellTune(DEFAULT_SPELL, SpellTuning.NONE)));
        String tuneRoot = join(ROOT_SPELLS, KEY_TUNE);
        register(tuneRoot, KEY_SPELL, Codec.STRING, DEFAULT_SPELL);
        registerTuningFields(tuneRoot);
    }

    private static void registerTuningFields(String tuningRoot) {
        register(tuningRoot, "cooldown_reduction", Codec.FLOAT, 1.0F);
        register(tuningRoot, "power_multiplier", Codec.FLOAT, 1.5F);
        register(tuningRoot, "critical_chance_bonus", Codec.FLOAT, 0.1F);
        register(tuningRoot, "critical_damage_bonus", Codec.FLOAT, 0.5F);
        register(tuningRoot, "ricochet", Codec.INT, 1);
        register(tuningRoot, "pierce", Codec.INT, 1);
        register(tuningRoot, "bounce", Codec.INT, 1);
        register(tuningRoot, "range_add", Codec.FLOAT, 4.0F);
        register(tuningRoot, "channel_ticks_add", Codec.INT, 10);
        register(tuningRoot, "effect_duration_add", Codec.FLOAT, 2.0F);
        register(tuningRoot, "effect_amplifier_add", Codec.INT, 1);
        register(tuningRoot, "impact_type", Codec.STRING.listOf(), List.of("damage"));
        register(tuningRoot, "impact_school", Codec.STRING.listOf(), List.of(DEFAULT_SCHOOL));
        register(tuningRoot, "ricochet_range", Codec.FLOAT, 4.0F);
        register(tuningRoot, "knockback_multiply", Codec.FLOAT, 0.5F);
        register(tuningRoot, SpellTuning.KEY_TRIGGER_CHANCE_ADD, Codec.FLOAT,
                DEFAULT_TRIGGER_CHANCE_ADD);
        register(tuningRoot, "launch", SpellLaunchTuning.CODEC, SpellLaunchTuning.NONE);
        String launchRoot = join(tuningRoot, "launch");
        register(launchRoot, "extra_launches", Codec.INT, 2);
        register(launchRoot, "launch_delay", Codec.INT, 5);
        register(launchRoot, "velocity_add", Codec.FLOAT, 0.5F);
        register(launchRoot, "scale_add", Codec.FLOAT, 0.5F);
        register(launchRoot, "meteor_radius_add", Codec.FLOAT, 1.0F);
        register(tuningRoot, "chain_reaction", SpellChainTuning.CODEC, SpellChainTuning.NONE);
        String chainReactionRoot = join(tuningRoot, "chain_reaction");
        register(chainReactionRoot, "size", Codec.INT, 2);
        register(chainReactionRoot, "triggers", Codec.INT, 1);
        register(chainReactionRoot, "increment", Codec.INT, 1);
    }

    private static void registerRaw(String path, String key, JsonElement sample) {
        TEMPLATES.computeIfAbsent(path, ignored -> new ArrayList<>())
                .add(new Template(key, sample));
    }

    private static JsonArray blankEntryList() {
        JsonArray list = new JsonArray();
        list.add(new JsonObject());
        return list;
    }

    private static void attachDamageSpec(String path, String key) {
        String root = join(path, key);
        register(root, "damage_spec", DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(root, "damage_spec"));
    }

    private static void attachSpawn(String root) {
        register(root, "spawn", ProjectileSpawn.CODEC, ProjectileSpawn.DEFAULT);
        String spawnRoot = join(root, "spawn");
        register(spawnRoot, "anchor", SpawnAnchor.CODEC, SpawnAnchor.EYES);
        register(spawnRoot, "forward", Codec.FLOAT, 1.0F);
        register(spawnRoot, "up", Codec.FLOAT, 0.0F);
        register(spawnRoot, "right", Codec.FLOAT, 0.0F);
        register(spawnRoot, "yaw", Codec.FLOAT, 0.0F);
        register(spawnRoot, "pitch", Codec.FLOAT, 0.0F);
        register(spawnRoot, "mode", SpreadMode.CODEC, SpreadMode.FAN);
        register(spawnRoot, "arc", Codec.FLOAT, 45.0F);
        register(spawnRoot, "inherit_motion", Codec.FLOAT, 0.0F);
        register(spawnRoot, "aim_at_target", Codec.BOOL, true);
        register(spawnRoot, "velocity", ProjectileVelocity.CODEC, ProjectileVelocity.DEFAULT);
        String velocityRoot = join(spawnRoot, "velocity");
        register(velocityRoot, "gravity", Codec.FLOAT, 0.03F);
        register(velocityRoot, "drag", Codec.FLOAT, 0.0F);
        register(velocityRoot, "acceleration", Codec.FLOAT, 0.05F);
        register(velocityRoot, "max_speed", Codec.FLOAT, 3.0F);
        register(velocityRoot, "divergence", Codec.FLOAT, 0.0F);
        register(velocityRoot, "homing", Codec.FLOAT, 0.1F);
    }

    private static void registerDamageSpec(String root) {
        register(root, "base", Codec.FLOAT, 4.0F);
        register(root, "of_hit", Codec.FLOAT, 0.5F);
        register(root, "of_target_max_health", Codec.FLOAT, 0.05F);
        register(root, "of_target_missing_health", Codec.FLOAT, 0.1F);
        register(root, "of_self_missing_health", Codec.FLOAT, 0.1F);
        register(root, "minimum", Codec.FLOAT, 1.0F);
        register(root, "maximum", Codec.FLOAT, 50.0F);
        register(root, "multiplier", Codec.FLOAT, DamageSpec.FULL_MULTIPLIER);
        register(root, "type", Codec.STRING, DEFAULT_DAMAGE_TYPE);
        register(root, "school", Codec.STRING, DEFAULT_SCHOOL);
        register(root, "scaling_mode", Codec.STRING, SCALING_COMBINES.getFirst());
        register(root, "scaling", DamageSpec.ScalingEntry.CODEC.listOf(),
                List.of(SAMPLE_SCALING_ENTRY));
        register(root, "proportional", DamageSpec.Share.CODEC.listOf(),
                List.of(SAMPLE_SHARE));

        String scaling = join(root, "scaling");
        register(scaling, "source", Codec.STRING, DEFAULT_SCALING_SOURCE);
        register(scaling, "per_point", Codec.FLOAT, 0.25F);
        register(scaling, "cap", Codec.FLOAT, 10.0F);
        register(scaling, "mode", Codec.STRING, SCALING_MODES.getFirst());
        register(scaling, "baseline", Codec.FLOAT, DamageSpec.ScalingEntry.DEFAULT_BASELINE);

        String shares = join(root, "proportional");
        register(shares, "type", Codec.STRING, DEFAULT_DAMAGE_TYPE);
        register(shares, "share", Codec.FLOAT, 0.5F);
    }

    private static void registerTemplate(String path, String key, JsonElement value) {
        TEMPLATES.computeIfAbsent(path, ignored -> new ArrayList<>())
                .add(new Template(key, value));
    }

    private static void deriveSubFieldsFromTemplates() {
        for (Map.Entry<String, List<Template>> entry : List.copyOf(TEMPLATES.entrySet())) {
            for (Template template : List.copyOf(entry.getValue())) {
                deriveChildren(join(entry.getKey(), template.key()), template.value());
            }
        }
    }

    private static void deriveChildren(String path, JsonElement value) {
        JsonObject shape = shapeOf(value);
        if (shape == null || TEMPLATES.containsKey(path)) {
            return;
        }
        for (Map.Entry<String, JsonElement> field : shape.entrySet()) {
            registerTemplate(path, field.getKey(), field.getValue().deepCopy());
        }
        for (Map.Entry<String, JsonElement> field : shape.entrySet()) {
            deriveChildren(join(path, field.getKey()), field.getValue());
        }
    }

    private static JsonObject shapeOf(JsonElement value) {
        if (value == null) {
            return null;
        }
        if (value.isJsonObject()) {
            return value.getAsJsonObject();
        }
        if (value.isJsonArray() && !value.getAsJsonArray().isEmpty()) {
            JsonElement first = value.getAsJsonArray().get(0);
            return first.isJsonObject() ? first.getAsJsonObject() : null;
        }
        return null;
    }

    private static void fillTemplatesFromSubFields() {
        for (Map.Entry<String, List<Template>> entry : TEMPLATES.entrySet()) {
            String path = entry.getKey();
            for (int i = 0; i < entry.getValue().size(); i++) {
                Template template = entry.getValue().get(i);
                List<Template> children = TEMPLATES.get(join(path, template.key()));
                if (children == null || children.isEmpty() || SPARSE_KEYS.contains(template.key())) {
                    continue;
                }
                JsonElement filled = withChildren(template.value(), children);
                if (filled != null) {
                    entry.getValue().set(i, new Template(template.key(), filled));
                }
            }
        }
    }

    private static JsonElement withChildren(JsonElement value, List<Template> children) {
        if (value == null) {
            return null;
        }
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            if (array.isEmpty() || !array.get(0).isJsonObject()) {
                return null;
            }
            JsonArray filled = new JsonArray();
            filled.add(withChildren(array.get(0), children));
            return filled;
        }
        if (!value.isJsonObject()) {
            return null;
        }
        JsonObject object = value.getAsJsonObject().deepCopy();
        for (Template child : children) {
            if (!object.has(child.key()) && !OPT_IN_KEYS.contains(child.key())) {
                object.add(child.key(), child.value().deepCopy());
            }
        }
        return object;
    }

    private static <T> void deriveMissing(String path, Codec<T> codec, T populated) {
        JsonElement encoded = encode(codec, populated);
        if (encoded == null || !encoded.isJsonObject()) {
            return;
        }
        List<Template> existing = TEMPLATES.computeIfAbsent(path,
                ignored -> new ArrayList<>());
        for (String key : encoded.getAsJsonObject().keySet()) {
            boolean known = false;
            for (Template template : existing) {
                if (template.key().equals(key)) {
                    known = true;
                    break;
                }
            }
            if (known) {
                continue;
            }
            existing.add(new Template(key,
                    encoded.getAsJsonObject().get(key).deepCopy()));
            CombatStyles.LOGGER.info(
                    "Effect schema derived '{}' at '{}' straight from the codec", key, path);
        }
    }

    private static <T> void audit(String path, Codec<T> codec) {
        List<Template> registered = TEMPLATES.get(path);
        if (registered == null) {
            return;
        }
        JsonObject probe = new JsonObject();
        for (Template template : registered) {
            probe.add(template.key(), template.value().deepCopy());
        }
        JsonElement roundTrip = codec.parse(JsonOps.INSTANCE, probe)
                .flatMap(parsed -> codec.encodeStart(JsonOps.INSTANCE, parsed))
                .result().orElse(null);
        if (roundTrip == null || !roundTrip.isJsonObject()) {
            CombatStyles.LOGGER.warn("Effect schema at '{}' does not round trip", path);
            return;
        }
        JsonObject accepted = roundTrip.getAsJsonObject();
        for (Template template : registered) {
            if (!accepted.has(template.key())) {
                CombatStyles.LOGGER.warn(
                        "Effect schema offers '{}' at '{}' but the codec discards it",
                        template.key(), path);
            }
        }
        for (String key : accepted.keySet()) {
            boolean offered = false;
            for (Template template : registered) {
                if (template.key().equals(key)) {
                    offered = true;
                    break;
                }
            }
            if (!offered) {
                CombatStyles.LOGGER.warn(
                        "Codec at '{}' accepts '{}' but the effect schema never offers it",
                        path, key);
            }
        }
    }

    private static JsonElement dashTemplate() {
        JsonObject dash = new JsonObject();
        dash.addProperty("forward", DEFAULT_DASH_FORWARD);
        dash.addProperty("upward", 0.0F);
        dash.addProperty("toward_target", true);
        dash.addProperty("chance", 1.0F);
        dash.addProperty("on_connect_ratio", 0.0F);
        dash.addProperty("on_connect_knockback", 0.0F);
        dash.addProperty("iframes", 0);
        return dash;
    }

    private static JsonElement blinkTemplate() {
        JsonObject blink = new JsonObject();
        blink.addProperty("distance", DEFAULT_BLINK_DISTANCE);
        blink.addProperty("behind_target", true);
        blink.addProperty("iframes", 0);
        return blink;
    }

    private static JsonElement vaultTemplate() {
        JsonObject vault = new JsonObject();
        vault.addProperty("radius", DEFAULT_VAULT_RADIUS);
        vault.addProperty("strength", DEFAULT_VAULT_STRENGTH);
        vault.addProperty("upward", true);
        vault.addProperty("chance", 1.0F);
        return vault;
    }

    private static void ensureLoaded() {
        if (!TEMPLATES.isEmpty()) {
            return;
        }
        register("", "damage", AffixEffects.Damage.CODEC, SAMPLE_DAMAGE);
        register("", "ignite", AffixEffects.Ignite.CODEC, new AffixEffects.Ignite(3, 1.0F));
        register("", "lifesteal", AffixEffects.Lifesteal.CODEC,
                new AffixEffects.Lifesteal(0.2F, 1.0F, Optional.empty()));
        attachDamageSpec("", "lifesteal");
        register("", "knockback", AffixEffects.Knockback.CODEC,
                new AffixEffects.Knockback(0.5F, false));
        register("", "splash", AffixEffects.Splash.CODEC,
                new AffixEffects.Splash(3.0F, 0.5F, false, true, Optional.empty(), Optional.empty(), Optional.empty()));
        attachDamageSpec("", "splash");
        String splashRoot = join("", "splash");
        register(splashRoot, "radius", Codec.FLOAT, 3.0F);
        register(splashRoot, "ratio", Codec.FLOAT, 0.5F);
        register(splashRoot, "hits_attacker", Codec.BOOL, true);
        register(splashRoot, "inherit", Codec.BOOL, true);
        register(splashRoot, "damage_type", Codec.STRING, DEFAULT_DAMAGE_TYPE);
        register(splashRoot, "school", Codec.STRING, DEFAULT_SCHOOL);
        register("", "sound", AffixEffects.Sound.CODEC,
                new AffixEffects.Sound(DEFAULT_SOUND, 1.0F, 1.0F));
        register("", "damages", AffixEffects.Damage.CODEC.listOf(),
                List.of(SAMPLE_DAMAGE));
        register("", "effects", AffixEffects.StatusEffect.CODEC.listOf(),
                List.of(new AffixEffects.StatusEffect(DEFAULT_STATUS_EFFECT, 100, 1, 1.0F,
                        true, Optional.of(4))));
        register("", "self_effects", AffixEffects.SelfEffect.CODEC.listOf(),
                List.of(new AffixEffects.SelfEffect(DEFAULT_STATUS_EFFECT, 100, 1,
                        true, Optional.of(4))));

        for (String effectRoot : List.of("effects", "self_effects")) {
            register(effectRoot, "effect", Codec.STRING, DEFAULT_STATUS_EFFECT);
            register(effectRoot, "duration", Codec.INT, DEFAULT_EFFECT_TICKS);
            register(effectRoot, "amplifier", Codec.INT, 1);
            register(effectRoot, "stacks", Codec.BOOL, true);
            register(effectRoot, "max_amplifier", Codec.INT, DEFAULT_MAX_AMPLIFIER);
        }
        register("effects", "chance", Codec.FLOAT, 0.5F);

        for (String damageRoot : List.of("damage", "damages")) {
            register(damageRoot, "mode", Codec.STRING, DAMAGE_MODES.get(1));
            register(damageRoot, "type", Codec.STRING, "");
            register(damageRoot, "school", Codec.STRING, DEFAULT_ATTRIBUTE);
            register(damageRoot, "ratio", Codec.FLOAT, 0.2F);
            register(damageRoot, "flat", Codec.FLOAT, 1.0F);
            register(damageRoot, "ignores_armour", Codec.BOOL, true);
            register(damageRoot, "scaling", AffixEffects.Scaling.CODEC,
                    new AffixEffects.Scaling(List.of(DEFAULT_ATTRIBUTE), 1.0F,
                            Optional.empty(), false));
        }
        register("", "particles", AffixEffects.Particles.CODEC.listOf(),
                List.of(new AffixEffects.Particles(DEFAULT_PARTICLE, 6, 0.35F, 0.0F)));
        register("", "projectiles", AffixEffects.Projectile.CODEC.listOf(), List.of(SAMPLE_PROJECTILE));
        registerProjectileFields(join("", "projectiles"));
        if (SpellEngineCompat.isLoaded()) {
            registerCastSpells(ROOT_EFFECT);
        }
        register("", "echo", AffixEffects.Echo.CODEC,
                new AffixEffects.Echo(1, DEFAULT_ECHO_DELAY, 1.0F, Optional.empty()));
        attachDamageSpec("", "echo");
        register("", "slash_wave", AffixEffects.SlashWave.CODEC,
                new AffixEffects.SlashWave(DEFAULT_WAVE_RANGE, DEFAULT_WAVE_RADIUS, 0.5F,
                        DEFAULT_WAVE_SPEED, 1.0F, true, true,
                        Optional.empty(), false,
                        Optional.empty(), AffixEffects.SlashWave.DEFAULT_FRAMES,
                        1.0F, Optional.empty(), 0, Optional.empty()));
        attachDamageSpec("", "slash_wave");
        register("", "freeze_ticks", Codec.FLOAT, 0.0F);
        register("", "delay_ticks", Codec.INT, 0);
        register("", "mechanics", AffixEffects.Mechanics.CODEC, AffixEffects.Mechanics.NONE);

        register("mechanics", "offense", AffixEffects.Offense.CODEC,
                AffixEffects.Offense.NONE);
        register("mechanics", "motion", AffixEffects.Motion.CODEC,
                AffixEffects.Motion.NONE);
        register("mechanics", "sustain", AffixEffects.Sustain.CODEC,
                AffixEffects.Sustain.NONE);
        register("mechanics", "control", AffixEffects.Control.CODEC,
                AffixEffects.Control.NONE);
        register("mechanics", "spawn", AffixEffects.Spawn.CODEC,
                AffixEffects.Spawn.NONE);
        register("mechanics", "max_targets_per_swing", Codec.INT,
                AffixEffects.Mechanics.UNLIMITED_TARGETS);

        register("mechanics/offense", "execute", AffixEffects.Execute.CODEC,
                new AffixEffects.Execute(0.3F, 1.0F, false, false, Optional.empty()));
        attachDamageSpec("mechanics/offense", "execute");
        String executeRoot = join("mechanics/offense", "execute");
        register(executeRoot, "health_threshold", Codec.FLOAT, 0.2F);
        register(executeRoot, "bonus_ratio", Codec.FLOAT, 1.0F);
        register(executeRoot, "instant_kill", Codec.BOOL, true);
        register(executeRoot, "repeat_on_kill", Codec.BOOL, true);
        register("mechanics/offense", "chain", AffixEffects.Chain.CODEC,
                new AffixEffects.Chain(2, 4.0F, 0.5F, 0.5F, true, Optional.empty(), Optional.empty(), Optional.empty()));
        attachDamageSpec("mechanics/offense", "chain");
        String chainRoot = join("mechanics/offense", "chain");
        register(chainRoot, "max_targets", Codec.INT, 3);
        register(chainRoot, "range", Codec.FLOAT, 4.0F);
        register(chainRoot, "falloff", Codec.FLOAT, 0.5F);
        register(chainRoot, "ratio", Codec.FLOAT, 0.5F);
        register(chainRoot, "inherit", Codec.BOOL, true);
        register(chainRoot, "damage_type", Codec.STRING, DEFAULT_DAMAGE_TYPE);
        register(chainRoot, "school", Codec.STRING, DEFAULT_SCHOOL);
        register("mechanics/offense", "momentum", AffixEffects.Momentum.CODEC,
                new AffixEffects.Momentum(0.1F, 5, 60));
        register("mechanics/offense", "cleave", AffixEffects.Cleave.CODEC,
                new AffixEffects.Cleave(3.0F, 0.5F, 3, true, Optional.empty(), Optional.empty(), Optional.empty()));
        attachDamageSpec("mechanics/offense", "cleave");
        String cleaveRoot = join("mechanics/offense", "cleave");
        register(cleaveRoot, "radius", Codec.FLOAT, 3.0F);
        register(cleaveRoot, "ratio", Codec.FLOAT, 0.5F);
        register(cleaveRoot, "max_targets", Codec.INT, 3);
        register(cleaveRoot, "inherit", Codec.BOOL, true);
        register(cleaveRoot, "damage_type", Codec.STRING, DEFAULT_DAMAGE_TYPE);
        register(cleaveRoot, "school", Codec.STRING, DEFAULT_SCHOOL);
        register("mechanics/offense", "delay_ticks", Codec.INT, 0);

        registerTemplate("mechanics/motion", "dash", dashTemplate());
        registerTemplate("mechanics/motion", "blink", blinkTemplate());
        register("mechanics/motion", "delay_ticks", Codec.INT, 0);

        register("mechanics/sustain", "heal", AffixEffects.Heal.CODEC,
                new AffixEffects.Heal(0.0F, 0.2F, 1.0F, false));
        register("mechanics/sustain", "shield", AffixEffects.Shield.CODEC,
                new AffixEffects.Shield(4.0F, 100, 0.0F, 0));
        register("mechanics/sustain", "delay_ticks", Codec.INT, 0);

        register("mechanics/control", "disarm", AffixEffects.Disarm.CODEC,
                new AffixEffects.Disarm(10, 1.0F, 0.25F));
        registerTemplate("mechanics/control", "vault", vaultTemplate());
        register("mechanics/control", "hasten", AffixEffects.Hasten.CODEC,
                new AffixEffects.Hasten(DEFAULT_HASTEN_RATIO, 1, 1.0F, 1.0F));
        register("mechanics/control", ATTACK_ATTRIBUTES,
                AffixEffects.AttackAttributeBonus.CODEC.listOf(),
                List.of(new AffixEffects.AttackAttributeBonus(DEFAULT_ATTRIBUTE, 1.0D,
                        DEFAULT_OPERATION, true)));
        TRIGGERS.put(ATTACK_ATTRIBUTES, Trigger.ON_SWING);
        register("mechanics/control", "attributes",
                AffixEffects.AttributeBonus.CODEC.listOf(),
                List.of(new AffixEffects.AttributeBonus(DEFAULT_ATTRIBUTE, 1.0D,
                        DEFAULT_OPERATION, 60, 1, false)));
        register("mechanics/control", "delay_ticks", Codec.INT, 0);

        register("mechanics/spawn", "summons", AffixEffects.Summon.CODEC.listOf(),
                List.of(new AffixEffects.Summon(DEFAULT_SUMMON_ENTITY, 1, 200, 2.0F,
                        true, 0.0F, Optional.empty(), Optional.empty(), 1.0F,
                        Optional.empty(), Optional.empty(), SummonEquipment.NONE,
                        SummonStats.NONE, false, false, false)));
        attachDamageSpec("mechanics/spawn", "summons");
        registerRaiseDead("mechanics/spawn");
        attachSpawn(join("mechanics/spawn", "summons"));

        String summons = join("mechanics/spawn", "summons");
        register(summons, "entity", Codec.STRING, DEFAULT_SUMMON_ENTITY);
        register(summons, "count", Codec.INT, 1);
        register(summons, "lifespan", Codec.INT, DEFAULT_SUMMON_LIFESPAN);
        register(summons, "radius", Codec.FLOAT, 2.0F);
        register(summons, "inherit_target", Codec.BOOL, true);
        register(summons, "damage_ratio", Codec.FLOAT, 0.0F);
        register(summons, "ability", Codec.STRING, "");
        register(summons, "chance", Codec.FLOAT, 1.0F);
        register(summons, "max_active", Codec.INT, 1);
        register(summons, "spectral", Codec.BOOL, true);
        register(summons, "keep_gear", Codec.BOOL, true);
        register(summons, KEY_OWN_DAMAGE, Codec.BOOL, true);
        registerSummonLoadout(summons);
        register("mechanics/spawn", "commands", AffixEffects.Command.CODEC.listOf(),
                List.of(new AffixEffects.Command(DEFAULT_COMMAND, true)));
        register("mechanics/spawn", "delay_ticks", Codec.INT, 0);

        register("mechanics/condition", "on_miss", Codec.BOOL, false);
        register("mechanics/condition", "multiplier", Codec.FLOAT, 1.0F);
        register("mechanics/condition", "refund_slot", Codec.BOOL, false);
        register("mechanics/condition", "self_health_below", Codec.FLOAT, 0.5F);
        register("mechanics/condition", "self_health_above", Codec.FLOAT, 0.5F);
        register("mechanics/condition", "target_health_below", Codec.FLOAT, 0.5F);
        register("mechanics/condition", "target_health_above", Codec.FLOAT, 0.5F);
        register("mechanics/condition", "sprinting", Codec.BOOL, true);
        register("mechanics/condition", "crouching", Codec.BOOL, true);
        register("mechanics/condition", "airborne", Codec.BOOL, true);
        register("mechanics/condition", "behind_target", Codec.BOOL, true);
        register("mechanics/condition", "night", Codec.BOOL, true);
        register("mechanics/condition", "damaged_within", Codec.INT, 40);
        registerItemGate("mechanics/condition");
        register("mechanics/condition", "damage_type", Codec.STRING.listOf(),
                List.of(DEFAULT_DAMAGE_TYPE));
        register("mechanics/condition", "taken_damage_type", Codec.STRING.listOf(),
                List.of(DEFAULT_TAKEN_DAMAGE_TAG));

        register("mechanics", "condition", AffixEffects.Condition.CODEC,
                AffixEffects.Condition.NONE);
        register("mechanics", "cooldown", AffixEffects.Cooldown.CODEC,
                new AffixEffects.Cooldown(40));
        register("mechanics", "cost", AffixEffects.Resource.CODEC,
                new AffixEffects.Resource(0.0F, 0, 0, true));
        String costRoot = join("mechanics", "cost");
        register(costRoot, "health", Codec.FLOAT, 2.0F);
        register(costRoot, "hunger", Codec.INT, 1);
        register(costRoot, "experience_levels", Codec.INT, 1);
        register(costRoot, "cancel_if_short", Codec.BOOL, true);
        String cooldownRoot = join("mechanics", "cooldown");
        register(cooldownRoot, "ticks", Codec.INT, 40);
        register(cooldownRoot, "shared", Codec.BOOL, true);

        registerTrigger();
        registerSelfEffects();
        registerDamageBonus();
        registerSubstitution();
        registerCooldown();
        registerAttributeMap();
        registerSummons();
        registerSummonAbilities();
        registerAbilityTuning(ROOT_ABILITY_TUNING);
        registerBehaviour();
        registerTuning();
        registerEcho();
        if (SpellEngineCompat.isLoaded()) {
            registerSpellLocks();
        }
        deriveAll();
        adoptRegistered();
        deriveSubFieldsFromTemplates();
        fillTemplatesFromSubFields();
        auditAll();
    }

    private static void deriveAll() {
        register(ROOT_STYLE_BOOK, KEY_MODE, BookRoll.Mode.CODEC, BookRoll.Mode.VARIANT);
        deriveMissing(ROOT_STYLE_BOOK, BookRoll.CODEC, BookRoll.SAMPLE);
        register(ROOT_WEAPON_ROLL, KEY_MODE, Codec.STRING, WeaponRoll.MODE_APPEND);
        deriveMissing(ROOT_WEAPON_ROLL, WeaponRoll.CODEC, WeaponRoll.SAMPLE);
        deriveMissing(ROOT_SUMMON_ABILITY, ScriptedAbility.CODEC, ScriptedAbility.SAMPLE);
        deriveMissing(ROOT_SUMMON_DIVE, SummonDive.CODEC, SummonDive.SAMPLE);
        deriveMissing(ROOT_ECHO, AffixEffects.Echo.CODEC, AffixEffects.Echo.SAMPLE);
        deriveMissing(ROOT_COOLDOWN, EffectCooldown.EXPANDED, EffectCooldown.SAMPLE);
        deriveMissing(ROOT_SUBSTITUTION, StyleSubstitution.CODEC, StyleSubstitution.SAMPLE);
        deriveMissing(ROOT_BEHAVIOUR, AttackBehaviour.CODEC,
                new AttackBehaviour(Optional.of(AffixEffects.Motion.NONE),
                        Optional.of(new HitboxOffset(1.0F, 0.0F, 0.0F)),
                        List.of(SlashPlacement.DEFAULT), List.of(ExtraAttack.DEFAULT)));
    }

    private static void adoptRegistered() {
        for (String scope : List.of(ROOT_EFFECT, ROOT_TRIGGER, ROOT_SUBSTITUTION,
                ROOT_SUMMON_PROFILE, ROOT_SUMMON_DIVE,
                ROOT_BEHAVIOUR, ROOT_TUNING, ROOT_ECHO, ROOT_COOLDOWN)) {
            for (SchemaRegistry.Entry entry : SchemaRegistry.fieldsFor(scope)) {
                registerTemplate(scope, entry.key(), entry.sample());
                SchemaRegistry.registerChoices(entry.key(), entry.choices());
            }
        }
    }

    private static void auditAll() {
        if (!CombatStylesConfig.get().debugLogging()) {
            return;
        }
        audit(ROOT_EFFECT, AffixEffects.OnHit.CODEC);
        audit(ROOT_TRIGGER, EquipmentTrigger.CODEC);
        audit(ROOT_SUBSTITUTION, StyleSubstitution.CODEC);
        audit(ROOT_SUMMON_PROFILE, SummonProfile.CODEC);
        audit(ROOT_SUMMON_DIVE, SummonDive.CODEC);
        audit(ROOT_ABILITY_GRANT, AbilityGrant.CODEC);
        audit(ROOT_SUMMON_ABILITY, ScriptedAbility.CODEC);
        audit(ROOT_BEHAVIOUR, AttackBehaviour.CODEC);
        audit(ROOT_TUNING, AffixTuning.CODEC);
        audit(ROOT_ECHO, AffixEffects.Echo.CODEC);
        audit(ROOT_COOLDOWN, EffectCooldown.EXPANDED);
        audit(ROOT_SPELLS, SpellLocks.CODEC);
        audit(ROOT_DAMAGE_BONUS, DamageBonusList.CODEC);
    }

    private static final int TRIGGER_NEST_DEPTH = 3;

    private static void registerSelfEffects() {
        register(ROOT_SELF_EFFECTS, "self_effects", AffixEffects.SelfEffect.CODEC.listOf(),
                List.of(new AffixEffects.SelfEffect(DEFAULT_STATUS_EFFECT, 100, 1,
                        true, Optional.of(4))));
        String element = join(ROOT_SELF_EFFECTS, "self_effects");
        register(element, "effect", Codec.STRING, DEFAULT_STATUS_EFFECT);
        register(element, "duration", Codec.INT, 100);
        register(element, "amplifier", Codec.INT, 0);
        register(element, "stacks", Codec.BOOL, true);
        register(element, "max_amplifier", Codec.INT, 4);
    }

    private static void registerDamageBonus() {
        register(ROOT_DAMAGE_BONUS, DamageBonus.KEY, DamageBonus.LIST_CODEC,
                List.of(DamageBonus.SAMPLE));
        String element = join(ROOT_DAMAGE_BONUS, DamageBonus.KEY);
        register(element, "damage_type", Codec.STRING.listOf(), List.of(DEFAULT_DAMAGE_TYPE));
        register(element, "amount", Codec.DOUBLE, DamageBonus.SAMPLE.amount());
        register(element, "operation", AttackAttribute.OPERATION_CODEC,
                DamageBonus.SAMPLE.operation());
        register(element, "stacks", Codec.BOOL, true);
    }

    private static void registerTrigger() {
        registerTriggerFields(ROOT_TRIGGER, TRIGGER_NEST_DEPTH);
        register(ROOT_TRIGGER, PhaseGate.CHANCE, Codec.FLOAT, DEFAULT_TRIGGER_CHANCE);
    }

    private static void registerTriggerFields(String root, int depth) {
        register(root, "on_miss", Codec.BOOL, true);
        register(root, "on_kill", Codec.BOOL, true);
        register(root, "on_summon_hit", Codec.BOOL, true);
        register(root, SummonGate.ON_SPAWN, Codec.BOOL, true);
        register(root, SummonGate.ON_DEATH, Codec.BOOL, true);
        register(root, SummonGate.ON_KILL, Codec.BOOL, true);
        register(root, SummonGate.ENTITIES, Codec.STRING.listOf(), List.of(DEFAULT_SUMMON_ENTITY));
        register(root, StateGate.SUMMONS_ACTIVE, Codec.INT, 1);
        register(root, "on_hurt", HurtGate.CODEC, HurtGate.ANY);
        String hurtRoot = join(root, "on_hurt");
        register(hurtRoot, "taken_damage_type", Codec.STRING.listOf(),
                List.of(DEFAULT_DAMAGE_TYPE));
        register(hurtRoot, "damage_above", Codec.FLOAT, 1.0F);
        register(hurtRoot, "damage_below", Codec.FLOAT, 5.0F);
        register(hurtRoot, "total_damage_above", Codec.FLOAT, 10.0F);
        register(hurtRoot, "attacker", Codec.STRING.listOf(),
                List.of(DEFAULT_SUMMON_ENTITY));
        register(hurtRoot, "enabled", Codec.BOOL, true);
        register(root, "on_shoot", ShootGate.CODEC, ShootGate.ANY);
        String shootRoot = join(root, "on_shoot");
        register(shootRoot, "entity", Codec.STRING.listOf(), List.of(DEFAULT_PROJECTILE_ENTITY));
        register(shootRoot, "from_summon", Codec.BOOL, true);
        register(shootRoot, "enabled", Codec.BOOL, true);
        register(root, "on_shield_block", BlockGate.CODEC, BlockGate.ANY);
        String blockRoot = join(root, "on_shield_block");
        register(blockRoot, "taken_damage_type", Codec.STRING.listOf(),
                List.of(DEFAULT_DAMAGE_TYPE));
        register(blockRoot, "fully_blocked", Codec.BOOL, true);
        register(blockRoot, "enabled", Codec.BOOL, true);
        register(root, "on_use", UseGate.CODEC, UseGate.of(true));
        String useRoot = join(root, "on_use");
        register(useRoot, "enabled", Codec.BOOL, true);
        register(useRoot, "phase", UsePhase.CODEC, UsePhase.ANY);
        register(useRoot, "held_above", Codec.INT, DEFAULT_HELD_TICKS);
        register(useRoot, "held_below", Codec.INT, DEFAULT_HELD_TICKS);
        register(root, "on_swing", Codec.BOOL, true);
        register(root, "on_player_hit", Codec.BOOL, true);
        register(root, "sprinting", Codec.BOOL, true);
        register(root, "crouching", Codec.BOOL, true);
        register(root, "airborne", Codec.BOOL, true);
        register(root, "behind_target", Codec.BOOL, true);
        register(root, "night", Codec.BOOL, true);
        register(root, "damaged_within", Codec.INT, DEFAULT_DAMAGED_WITHIN);
        registerItemGate(root);
        if (SpellEngineCompat.isLoaded()) {
            register(root, "has_spell", Codec.BOOL, true);
            register(root, CastGate.KEY, CastGate.CODEC, CastGate.of(true));
            String castRoot = join(root, CastGate.KEY);
            register(castRoot, "enabled", Codec.BOOL, true);
            register(castRoot, "phase", CastPhase.CODEC, CastPhase.CAST);
            register(castRoot, "spell", Codec.STRING.listOf(), List.of(DEFAULT_SPELL));
            register(castRoot, "school", Codec.STRING.listOf(), List.of(DEFAULT_SCHOOL));
            register(castRoot, "cast_type", Codec.STRING.listOf(), List.of(DEFAULT_CAST_TYPE));
            register(castRoot, "include_triggered", Codec.BOOL, true);
        }
        register(root, "damage_above", Codec.FLOAT, 1.0F);
        register(root, "damage_below", Codec.FLOAT, 5.0F);
        register(root, "damage_type", Codec.STRING.listOf(), List.of(DEFAULT_DAMAGE_TYPE));
        register(root, "target", Codec.STRING.listOf(), List.of(DEFAULT_SUMMON_ENTITY));
        register(root, "on_projectile_hit", ProjectileHitGate.CODEC, ProjectileHitGate.ANY);
        String projectileRoot = join(root, "on_projectile_hit");
        register(projectileRoot, "entity", Codec.STRING.listOf(),
                List.of(DEFAULT_PROJECTILE_ENTITY));
        register(projectileRoot, "from_summon", Codec.BOOL, true);
        register(projectileRoot, "enabled", Codec.BOOL, true);
        register(projectileRoot, "distance_above", Codec.DOUBLE, DEFAULT_SHOT_DISTANCE);
        register(projectileRoot, "distance_below", Codec.DOUBLE, DEFAULT_SHOT_DISTANCE);
        register(projectileRoot, "damage_above", Codec.FLOAT, 1.0F);
        register(projectileRoot, "damage_below", Codec.FLOAT, 5.0F);
        register(projectileRoot, "damage_type", Codec.STRING.listOf(),
                List.of(DEFAULT_DAMAGE_TYPE));
        register(projectileRoot, "target", Codec.STRING.listOf(),
                List.of(DEFAULT_SUMMON_ENTITY));
        if (SpellEngineCompat.isLoaded()) {
            register(projectileRoot, "spell_projectile", SpellProjectileGate.CODEC.codec(),
                    new SpellProjectileGate(true, List.of(), List.of()));
            String spellRoot = join(projectileRoot, "spell_projectile");
            register(spellRoot, "enabled", Codec.BOOL, true);
            register(spellRoot, "spell", Codec.STRING.listOf(), List.of(DEFAULT_SPELL));
            register(spellRoot, "school", Codec.STRING.listOf(), List.of(DEFAULT_SCHOOL));
        }
        register(root, "requires_affix", Codec.BOOL, true);
        register(root, "last_slot", Codec.BOOL, true);
        register(root, "every_nth_swing", Codec.INT, DEFAULT_NTH_SWING);
        register(root, "slot_index", Codec.INT, 0);
        register(root, "idle_ticks", Codec.INT, DEFAULT_IDLE_TICKS);
        register(root, "hitbox", Codec.STRING, "");
        register(root, "style_id", Codec.STRING, "");
        register(root, "affix_id", Codec.STRING, "");
        register(root, "slots", SlotSelection.CODEC,
                new SlotSelection(Optional.of(new SlotSelection.Range(0, 1)), List.of()));
        register(root, "slot_band", SlotBand.CODEC, SlotBand.OPENING);
        register(root, "health", HealthGate.CODEC, HealthGate.ANY);
        String healthRoot = join(root, "health");
        register(healthRoot, "self_above", Codec.FLOAT, 0.5F);
        register(healthRoot, "self_below", Codec.FLOAT, 0.5F);
        register(healthRoot, "target_above", Codec.FLOAT, 0.5F);
        register(healthRoot, "target_below", Codec.FLOAT, 0.5F);
        if (depth <= 0) {
            return;
        }
        registerRaw(root, "any_of", blankEntryList());
        registerRaw(root, "all_of", blankEntryList());
        registerTriggerFields(join(root, "any_of"), depth - 1);
        registerTriggerFields(join(root, "all_of"), depth - 1);
    }

    private static void registerAttributeMap() {
        registerAttributeScope(ROOT_ATTRIBUTE_MAP, false);
        registerAttributeScope(ROOT_CONDITIONAL_ATTRIBUTES, true);
    }

    private static void registerAttributeScope(String root, boolean decaying) {
        for (String attribute : EffectChoices.attributeIds()) {
            register(root, attribute, AttributeBonus.EXPANDED,
                    new AttributeBonus(1.0D, AttributeModifier.Operation.ADD_VALUE,
                            true, false, Optional.empty()));
            String entry = join(root, attribute);
            register(entry, "amount", Codec.DOUBLE, 1.0D);
            register(entry, "operation", AttackAttribute.OPERATION_CODEC,
                    AttributeModifier.Operation.ADD_VALUE);
            register(entry, "stacks", Codec.BOOL, true);
            register(entry, "slot", Codec.STRING, AttributeBonus.SLOT_ACCESSORY);
            if (decaying) {
                register(entry, "set", Codec.BOOL, true);
            }
        }
    }

    private static void registerCooldown() {
        register(ROOT_COOLDOWN, "ticks", Codec.INT, 0);
        register(ROOT_COOLDOWN, "uses", Codec.INT, 1);
        register(ROOT_COOLDOWN, "duration", Codec.INT, EffectCooldown.DEFAULT_DURATION);
        register(ROOT_COOLDOWN, "refresh_on_use", Codec.BOOL, true);
        register(ROOT_COOLDOWN, "hold_while_active", Codec.BOOL, false);
        register(ROOT_TOOLTIP, TriggeredEffect.TOOLTIP_HEADER, Codec.STRING, "");
        register(ROOT_TOOLTIP, TriggeredEffect.TOOLTIP_LINES, Codec.STRING.listOf(), List.of(""));
    }

    private static void registerSubstitution() {
        register(ROOT_SUBSTITUTION, "styles", Codec.STRING.listOf(), List.of(""));
        register(ROOT_SUBSTITUTION, "slot", Codec.INT.listOf(), List.of(0));
        register(ROOT_SUBSTITUTION, "mode", Codec.STRING, "");
        register(ROOT_SUBSTITUTION, "weapon_categories", Codec.STRING.listOf(), List.of(""));
        register(ROOT_SUBSTITUTION, "item_tags", Codec.STRING.listOf(), List.of(""));
        register(ROOT_SUBSTITUTION, "borrow_slots", Codec.INT.listOf(), List.of(0));
        register(ROOT_SUBSTITUTION, "linger_ticks", Codec.INT, DEFAULT_LINGER_TICKS);
        register(ROOT_SUBSTITUTION, "charge_decay_ticks", Codec.INT, 40);
        register(ROOT_SUBSTITUTION, "restart_combo", Codec.BOOL, false);
        register(ROOT_SUBSTITUTION, "apply", Codec.STRING, StyleSubstitution.APPLY_ON_SLOT);
        register(ROOT_SUBSTITUTION, "tuning",
                Codec.unboundedMap(Codec.STRING, AttackOverride.CODEC), Map.of());
    }

    private static void registerSummonLoadout(String root) {
        register(root, KEY_SUMMON_EQUIPMENT, SummonEquipment.CODEC, SummonEquipment.NONE);
        String equipment = join(root, KEY_SUMMON_EQUIPMENT);
        for (String slot : SUMMON_SLOTS) {
            register(equipment, slot, Codec.STRING, DEFAULT_SUMMON_ITEM);
        }
        register(root, KEY_ATTRIBUTES, SummonStats.CODEC, SummonStats.NONE);
        String stats = join(root, KEY_ATTRIBUTES);
        for (String attribute : EffectChoices.attributeIds()) {
            register(stats, attribute, AttributeBonus.EXPANDED,
                    new AttributeBonus(1.0D, AttributeModifier.Operation.ADD_VALUE,
                            true, false, Optional.empty()));
            String entry = join(stats, attribute);
            register(entry, "amount", Codec.DOUBLE, 1.0D);
            register(entry, "operation", AttackAttribute.OPERATION_CODEC,
                    AttributeModifier.Operation.ADD_VALUE);
        }
    }

    private static void registerSummons() {
        for (String root : List.of(ROOT_SUMMON_PROFILE)) {
            register(root, "replaced_summoned_entities", Codec.STRING.listOf(),
                    List.of(DEFAULT_SUMMON_ENTITY));
            register(root, "abilities", Codec.STRING.listOf(), List.of(""));
            register(root, "max_active", Codec.INT, 1);
            register(root, "damage_multiplier", Codec.FLOAT, 1.0F);
            register(root, "lifespan", Codec.INT, DEFAULT_SUMMON_LIFESPAN);
            register(root, "damage_scaling", AffixEffects.Scaling.CODEC,
                    new AffixEffects.Scaling(List.of(DEFAULT_ATTRIBUTE), 1.0F,
                            Optional.empty(), false));
            register(root, "exempt", Codec.STRING.listOf(), List.of(""));
        }
        register(ROOT_SUMMON_PROFILE, "entity", Codec.STRING, DEFAULT_SUMMON_ENTITY);
        register(ROOT_SUMMON_PROFILE, "anchored", Codec.INT, 1);
        register(ROOT_SUMMON_PROFILE, "finale", Codec.STRING, "");
        register(ROOT_SUMMON_PROFILE, "replaces_summons", Codec.BOOL, true);
        register(ROOT_SUMMON_PROFILE, KEY_DAMAGE_SPEC, DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        register(ROOT_SUMMON_PROFILE, KEY_OWN_DAMAGE, Codec.BOOL, true);
        registerDamageSpec(join(ROOT_SUMMON_PROFILE, KEY_DAMAGE_SPEC));
        register(ROOT_SUMMON_PROFILE, KEY_COMPANION, SummonCompanion.CODEC,
                SummonCompanion.ofCount(SummonCompanion.DEFAULT_COUNT));
        String companion = join(ROOT_SUMMON_PROFILE, KEY_COMPANION);
        register(companion, "count", Codec.INT, SummonCompanion.DEFAULT_COUNT);
        register(companion, "respawn_ticks", Codec.INT, SummonCompanion.DEFAULT_RESPAWN_TICKS);
        register(companion, "spectral", Codec.BOOL, true);
        register(companion, KEY_FOLLOW, SummonFollow.CODEC, SAMPLE_FOLLOW);
        String follow = join(companion, KEY_FOLLOW);
        register(follow, "start_distance", Codec.DOUBLE, SummonTuning.get().followStartDistance);
        register(follow, "stop_distance", Codec.DOUBLE, SummonTuning.get().followStopDistance);
        register(follow, "teleport_distance", Codec.DOUBLE, SummonTuning.get().teleportDistance);
        registerSummonLoadout(companion);
        register(ROOT_SUMMON_PROFILE, "tuning",
                Codec.unboundedMap(Codec.STRING, AbilityTuning.CODEC), Map.of());
        keyedMap(join(ROOT_SUMMON_PROFILE, KEY_TUNING), AbilityRegistry.OPTION_SELECTOR);
        registerAbilityTuning(join(join(ROOT_SUMMON_PROFILE, KEY_TUNING), MAP_ENTRY));
        registerCadence(ROOT_SUMMON_PROFILE);
        registerAbilityGrant();
        register(ROOT_SUMMON_PROFILE, "variants", ProfileVariant.CODEC.listOf(),
                List.of(new ProfileVariant(List.of(), Optional.empty(), Map.of(),
                        List.of(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Map.of())));

        String variants = join(ROOT_SUMMON_PROFILE, "variants");
        register(variants, KEY_CONDITIONS, ProfileCondition.LIST_CODEC, List.of());
        register(variants, "finale", Codec.STRING, "");
        register(variants, "swaps", Codec.unboundedMap(Codec.STRING, Codec.STRING), Map.of());
        register(variants, "max_active", Codec.INT, 1);
        register(variants, "anchored", Codec.INT, 1);
        register(variants, "damage_multiplier", Codec.FLOAT, 1.0F);
        register(variants, "tuning",
                Codec.unboundedMap(Codec.STRING, AbilityTuning.CODEC), Map.of());
        keyedMap(join(variants, KEY_TUNING), AbilityRegistry.OPTION_SELECTOR);
        registerAbilityTuning(join(join(variants, KEY_TUNING), MAP_ENTRY));
        registerCadence(variants);

        register(ROOT_SUMMON_DIVE, "damage_share", Codec.FLOAT, 0.5F);
        register(ROOT_SUMMON_DIVE, SummonCadence.KEY_AIM, SummonAim.CODEC, SummonAim.HIT_TARGET);
        register(ROOT_SUMMON_DIVE, "max_diving", Codec.INT, 1);
        register(ROOT_SUMMON_DIVE, "entities", Codec.STRING.listOf(),
                List.of(DEFAULT_SUMMON_ENTITY));
        register(ROOT_SUMMON_DIVE, "tuning", AbilityTuning.CODEC, SummonDive.SAMPLE_TUNING);

        registerAbilityTuning(join(ROOT_SUMMON_DIVE, KEY_TUNING));
    }

    private static void registerMobFilter(String root) {
        register(root, MobFilter.KEY_ENTITIES, Codec.STRING.listOf(), List.of(DEFAULT_SUMMON_ENTITY));
        register(root, MobFilter.KEY_ENTITY_TAGS, Codec.STRING.listOf(), List.of(DEFAULT_ENTITY_TAG));
        register(root, MobFilter.KEY_EXEMPT_ENTITIES, Codec.STRING.listOf(), List.of(DEFAULT_SUMMON_ENTITY));
        register(root, MobFilter.KEY_EXEMPT_TAGS, Codec.STRING.listOf(), List.of(DEFAULT_ENTITY_TAG));
    }

    private static void registerAbilityGrant() {
        register(ROOT_ABILITY_GRANT, AbilityGrant.KEY_APPLIES_TO, GrantTarget.CODEC, GrantTarget.BOTH);
        registerMobFilter(ROOT_ABILITY_GRANT);
        registerCadence(ROOT_ABILITY_GRANT);
    }

    private static void registerRaiseDead(String root) {
        register(root, RaiseRules.KEY, RaiseRules.CODEC, RaiseRules.DEFAULT);
        String raise = join(root, RaiseRules.KEY);
        registerMobFilter(raise);
        register(raise, RaiseRules.KEY_LIFESPAN_TICKS, Codec.INT, RaiseRules.DEFAULT_LIFESPAN);
        register(raise, RaiseRules.KEY_KEEP_GEAR, Codec.BOOL, true);
        register(raise, RaiseRules.KEY_SPECTRAL, Codec.BOOL, true);
        register(raise, RaiseRules.KEY_MAX_ACTIVE, Codec.INT, DEFAULT_RAISE_MAX);
        register(raise, RaiseRules.KEY_CHANCE, Codec.FLOAT, RaiseRules.ALWAYS);
        register(raise, RaiseRules.KEY_PROFILE, RaiseProfile.CODEC, RaiseRules.DEFAULT.profile());
        register(raise, RaiseRules.KEY_PROFILES, SummonProfile.CODEC.listOf(), List.of(SummonProfile.EMPTY));
        mount(join(raise, RaiseRules.KEY_PROFILES), ROOT_SUMMON_PROFILE);
    }

    private static void registerCadence(String root) {
        register(root, SummonActives.KEY, SummonActives.CODEC, SummonActives.SAMPLE);
        String actives = join(root, SummonActives.KEY);
        register(actives, SummonActives.KEY_ABILITIES, AbilityUse.CODEC.listOf(), List.of(AbilityUse.SAMPLE));
        String uses = join(actives, SummonActives.KEY_ABILITIES);
        register(uses, AbilityUse.KEY_ABILITY, Codec.STRING, "");
        register(uses, AbilityUse.KEY_TUNING, AbilityTuning.CODEC, AbilityTuning.NONE);
        registerAbilityTuning(join(uses, AbilityUse.KEY_TUNING));
        register(actives, SummonActives.KEY_PICK, AbilityPick.CODEC, AbilityPick.RANDOM);
        register(actives, SummonCadence.KEY_RECAST_TICKS, Codec.INT, DEFAULT_RECAST_TICKS);
        register(actives, SummonCadence.KEY_AIM, SummonAim.CODEC, SummonAim.HIT_TARGET);
        register(root, SummonCadence.KEY_PASSIVES, SummonPassive.CODEC.listOf(),
                List.of(SummonPassive.SAMPLE));
        String passives = join(root, SummonCadence.KEY_PASSIVES);
        register(passives, SummonPassive.KEY_ABILITY, Codec.STRING, "");
        register(passives, SummonPassive.KEY_ON, PassiveTrigger.CODEC, PassiveTrigger.HIT);
        register(passives, SummonPassive.KEY_CHANCE, Codec.FLOAT, DEFAULT_STEP_CHANCE);
        register(passives, SummonPassive.KEY_COOLDOWN_TICKS, Codec.INT, DEFAULT_RECAST_TICKS);
        register(passives, SummonPassive.KEY_OPTIONS, AbilityTuning.OPTIONS_CODEC, new JsonObject());
        register(passives, SummonPassive.KEY_TUNING, AbilityTuning.CODEC, AbilityTuning.NONE);
        registerAbilityTuning(join(passives, SummonPassive.KEY_TUNING));
    }

    private static void registerSummonAbilities() {
        register(ROOT_SUMMON_ABILITY, ScriptedAbility.KEY_KIND, AbilityKind.CODEC, AbilityKind.ACTIVE);
        register(ROOT_SUMMON_ABILITY, ScriptedAbility.KEY_ON, PassiveTrigger.CODEC, PassiveTrigger.HIT);
        register(ROOT_SUMMON_ABILITY, ScriptedAbility.KEY_CHANCE, Codec.FLOAT, DEFAULT_STEP_CHANCE);
        register(ROOT_SUMMON_ABILITY, "used_by", Codec.STRING.listOf(),
                List.of(DEFAULT_SUMMON_ENTITY));
        register(ROOT_SUMMON_ABILITY, KEY_FOCUS, AbilityFocus.CODEC, AbilityFocus.TARGET);
        register(ROOT_SUMMON_ABILITY, KEY_TUNING, AbilityTuning.CODEC, AbilityTuning.NONE);
        registerAbilityTuning(join(ROOT_SUMMON_ABILITY, KEY_TUNING));
        register(ROOT_SUMMON_ABILITY, KEY_STEPS, ScriptedAbility.Step.CODEC.listOf(),
                List.of(ScriptedAbility.Step.SAMPLE));
        String steps = join(ROOT_SUMMON_ABILITY, KEY_STEPS);
        register(steps, KEY_AT, Codec.STRING, KEY_AT_WINDUP);
        register(steps, "every", Codec.INT, DEFAULT_STEP_EVERY);
        register(steps, ScriptedAbility.Step.KEY_UNTIL, Codec.STRING, KEY_AT_END);
        register(steps, ScriptedAbility.Step.KEY_CHANCE, Codec.FLOAT, DEFAULT_STEP_CHANCE);
        register(steps, KEY_ACTION, ScriptedAbility.Action.CODEC, ScriptedAbility.Action.SAMPLE);
        String action = join(steps, KEY_ACTION);
        register(action, "type", ScriptedAbility.ActionType.CODEC, ScriptedAbility.ActionType.BEAM);
        register(action, "damage_ratio", Codec.FLOAT, ScriptedAbility.Action.FULL_DAMAGE);
        register(action, ScriptedAbility.KEY_DAMAGE_SPEC, DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(action, ScriptedAbility.KEY_DAMAGE_SPEC));
        register(action, "at_focus", Codec.BOOL, true);
        register(action, "effect", Codec.STRING, "");
        register(action, "stacks", Codec.INT, 1);
        register(action, "duration_ticks", Codec.INT, AbilityStacks.DEFAULT_DURATION_TICKS);
        register(action, "max_stacks", Codec.INT, AbilityStacks.DEFAULT_MAX_STACKS);
        register(action, "per_stack", Codec.FLOAT, ScriptedAbility.Action.DEFAULT_PER_STACK);
        register(action, "heal_ratio", Codec.FLOAT, DEFAULT_HEAL_RATIO);
        register(action, "sound", Codec.STRING, DEFAULT_SOUND);
        register(action, "particle", Codec.STRING, DEFAULT_PARTICLE);
        register(action, ScriptedAbility.KEY_PROJECTILE, AffixEffects.Projectile.CODEC, SAMPLE_PROJECTILE);
        registerProjectileFields(join(action, ScriptedAbility.KEY_PROJECTILE));
        register(action, ParticleShape.KEY, ParticleShape.CODEC, ParticleShape.DEFAULT);
        String particleShape = join(action, ParticleShape.KEY);
        register(particleShape, ParticleShape.KEY_FORM, ParticleShape.Form.CODEC, ParticleShape.Form.RING);
        register(particleShape, ParticleShape.KEY_RADIUS, Codec.FLOAT, ParticleShape.FROM_TUNING);
        register(particleShape, ParticleShape.KEY_POINTS, Codec.INT, ParticleShape.POINTS_FROM_TUNING);
        register(particleShape, ParticleShape.KEY_HEIGHT, Codec.FLOAT, ParticleShape.DEFAULT_HEIGHT);
        register(particleShape, ParticleShape.KEY_TURNS, Codec.FLOAT, ParticleShape.DEFAULT_TURNS);
        register(particleShape, ParticleShape.KEY_Y_OFFSET, Codec.FLOAT, ParticleShape.NO_OFFSET);
        register(particleShape, ParticleShape.KEY_GROW, Codec.BOOL, true);
        register(action, BeamLook.KEY, BeamLook.CODEC, BeamLook.DEFAULT);
        String beam = join(action, BeamLook.KEY);
        register(beam, BeamLook.KEY_COLOR, Codec.STRING, BeamLook.DEFAULT_COLOR);
        register(beam, BeamLook.KEY_WIDTH, Codec.FLOAT, BeamLook.DEFAULT_WIDTH);
        register(beam, BeamLook.KEY_LIFT, Codec.FLOAT, BeamLook.NO_LIFT);
        register(beam, BeamLook.KEY_ORIGIN, BeamOrigin.CODEC, BeamOrigin.ABILITY);
        register(beam, BeamLook.KEY_DURATION_TICKS, Codec.INT, BeamLook.DEFAULT_DURATION_TICKS);
        register(beam, BeamLook.KEY_ALPHA, Codec.FLOAT, BeamLook.DEFAULT_ALPHA);
        register(action, "spell", Codec.STRING, "");
        register(action, "spell_tuning", SpellTuning.CODEC, SpellTuning.NONE);
        registerTuningFields(join(action, "spell_tuning"));
        register(action, ScriptedAbility.Action.KEY_SPELL_TARGET, SummonCastTarget.CODEC,
                SummonCastTarget.AUTO);
        register(action, ScriptedAbility.Action.KEY_ROLL_SPELL_CHANCE, Codec.BOOL, true);
    }

    private static final StepTuning SAMPLE_STEP_TUNING = new StepTuning(StepTuning.FIRST_STEP,
            Optional.of(SAMPLE_DAMAGE_SPEC));

    private static void registerAbilityTuning(String tuning) {
        register(tuning, "damage", AbilityTuning.Damage.CODEC, AbilityTuning.Damage.NONE);
        register(tuning, "timing", AbilityTuning.Timing.CODEC, AbilityTuning.Timing.NONE);
        register(tuning, "shape", AbilityTuning.Shape.CODEC, AbilityTuning.Shape.NONE);
        register(tuning, "fx", AbilityTuning.Presentation.CODEC,
                AbilityTuning.Presentation.NONE);
        register(tuning, "extra", Codec.unboundedMap(Codec.STRING, Codec.DOUBLE), Map.of());
        register(tuning, OptionSources.KEY_OPTIONS, AbilityTuning.OPTIONS_CODEC, new JsonObject());
        register(tuning, AbilityTuning.KEY_STEPS, StepTuning.CODEC.listOf(), List.of(SAMPLE_STEP_TUNING));
        String stepTuning = join(tuning, AbilityTuning.KEY_STEPS);
        register(stepTuning, StepTuning.KEY_STEP, Codec.INT, StepTuning.FIRST_STEP);
        register(stepTuning, StepTuning.KEY_DAMAGE_SPEC, DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(stepTuning, StepTuning.KEY_DAMAGE_SPEC));

        register(join(tuning, "damage"), "hits", Codec.INT, 1);
        register(join(tuning, "damage"), "lifesteal", Codec.FLOAT, 0.5F);
        register(join(tuning, "damage"), "applies", Codec.STRING, "");
        register(join(tuning, "damage"), "applies_stacks", Codec.INT, 1);
        register(join(tuning, "damage"), "applies_chance", Codec.FLOAT, 1.0F);
        register(join(tuning, "damage"), "detonates", Codec.BOOL, true);
        register(join(tuning, "damage"), KEY_DAMAGE_SPEC, DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(join(tuning, "damage"), KEY_DAMAGE_SPEC));

        register(join(tuning, "timing"), "duration_ticks", Codec.INT, DEFAULT_TUNING_COOLDOWN);
        register(join(tuning, "timing"), "windup_ticks", Codec.INT, 1);
        register(join(tuning, "timing"), "cooldown_ticks", Codec.INT, DEFAULT_TUNING_COOLDOWN);

        register(join(tuning, "shape"), "radius", Codec.DOUBLE, 1.0D);
        register(join(tuning, "shape"), "range", Codec.DOUBLE, 1.0D);
        register(join(tuning, "shape"), "rise", Codec.DOUBLE, 1.0D);
        register(join(tuning, "shape"), "pull", Codec.DOUBLE, 1.0D);
        register(join(tuning, "shape"), "lift", Codec.DOUBLE, 1.0D);
        register(join(tuning, "shape"), "max_targets", Codec.INT, 1);

        register(join(tuning, "fx"), "particles", Codec.INT, 1);
        register(join(tuning, "fx"), "spread", Codec.DOUBLE, 1.0D);
        register(join(tuning, "fx"), "volume", Codec.FLOAT, 1.0F);
        register(join(tuning, "fx"), "pitch", Codec.FLOAT, 1.0F);
    }

    private static void registerBehaviour() {
        register(ROOT_BEHAVIOUR, "motion", AffixEffects.Motion.CODEC, AffixEffects.Motion.NONE);
        register(ROOT_BEHAVIOUR, "hitbox_offset", HitboxOffset.CODEC,
                new HitboxOffset(1.0F, 0.0F, 0.0F));
        String offsetRoot = join(ROOT_BEHAVIOUR, "hitbox_offset");
        register(offsetRoot, "forward", Codec.FLOAT, 1.0F);
        register(offsetRoot, "up", Codec.FLOAT, 1.0F);
        register(offsetRoot, "right", Codec.FLOAT, 1.0F);
        register(offsetRoot, "yaw", Codec.FLOAT, 45.0F);
        register(offsetRoot, "pitch", Codec.FLOAT, 45.0F);
        register(offsetRoot, "roll", Codec.FLOAT, 45.0F);

        register(ROOT_BEHAVIOUR, "placement", AttackPlacement.CODEC,
                new AttackPlacement(AttackPlacement.Mode.CURSOR,
                        AttackPlacement.DEFAULT_RANGE, true,
                        AttackPlacement.DEFAULT_MARGIN));
        String placementRoot = join(ROOT_BEHAVIOUR, "placement");
        register(placementRoot, "mode", Codec.STRING,
                AttackPlacement.Mode.CURSOR.getSerializedName());
        register(placementRoot, "range", Codec.FLOAT, AttackPlacement.DEFAULT_RANGE);
        register(placementRoot, "stop_at_entities", Codec.BOOL, true);
        register(placementRoot, "stop_margin", Codec.FLOAT, AttackPlacement.DEFAULT_MARGIN);

        register(ROOT_BEHAVIOUR, "boxes", HitboxSet.CODEC,
                new HitboxSet(HitboxSet.Resolve.PRIORITY,
                        List.of(new HitboxSet.Shape(new HitboxOffset(2.0F, 0.0F, 0.0F),
                                10, Optional.of(SAMPLE_DAMAGE_SPEC)))));
        String boxesRoot = join(ROOT_BEHAVIOUR, "boxes");
        register(boxesRoot, "resolve", Codec.STRING,
                HitboxSet.Resolve.PRIORITY.getSerializedName());
        register(boxesRoot, "shapes", HitboxSet.Shape.CODEC.listOf(),
                List.of(new HitboxSet.Shape(new HitboxOffset(2.0F, 0.0F, 0.0F), 10,
                        Optional.of(SAMPLE_DAMAGE_SPEC))));
        String shapesRoot = join(boxesRoot, "shapes");
        register(shapesRoot, "priority", Codec.INT, 10);
        register(shapesRoot, "width", Codec.FLOAT, HitboxSet.Shape.DEFAULT_SIZE);
        register(shapesRoot, "height", Codec.FLOAT, HitboxSet.Shape.DEFAULT_SIZE);
        register(shapesRoot, "depth", Codec.FLOAT, HitboxSet.Shape.DEFAULT_SIZE);
        register(shapesRoot, "offset", HitboxOffset.CODEC,
                new HitboxOffset(2.0F, 0.0F, 0.0F));
        register(shapesRoot, "damage_spec", DamageSpec.CODEC, SAMPLE_DAMAGE_SPEC);
        registerDamageSpec(join(shapesRoot, "damage_spec"));
        String shapeOffsetRoot = join(shapesRoot, "offset");
        register(shapeOffsetRoot, "forward", Codec.FLOAT, 2.0F);
        register(shapeOffsetRoot, "up", Codec.FLOAT, 1.0F);
        register(shapeOffsetRoot, "right", Codec.FLOAT, 1.0F);
        register(shapeOffsetRoot, "yaw", Codec.FLOAT, 45.0F);
        register(shapeOffsetRoot, "pitch", Codec.FLOAT, 45.0F);
        register(shapeOffsetRoot, "roll", Codec.FLOAT, 45.0F);
        register(ROOT_BEHAVIOUR, "slashes", SlashPlacement.CODEC.listOf(),
                List.of(SlashPlacement.DEFAULT));
        register(ROOT_BEHAVIOUR, "extras", ExtraAttack.CODEC.listOf(),
                List.of(ExtraAttack.DEFAULT));

        String extras = join(ROOT_BEHAVIOUR, "extras");
        register(extras, "delay_ticks", Codec.INT, ExtraAttack.MAX_DELAY / 2);
        register(extras, "anchor", Codec.STRING, SlashAnchor.HITBOX.getSerializedName());
        register(extras, "inherit", InheritGroups.CODEC, InheritGroups.ALL);
        register(extras, "affixes", Codec.STRING.listOf(), List.of(""));
        register(extras, KEY_OVERRIDE, AttackOverride.CODEC, AttackOverride.EMPTY);

        String override = join(extras, KEY_OVERRIDE);
        register(override, "damage_multiplier", Codec.DOUBLE, 1.0D);
        register(override, "angle", Codec.DOUBLE, 90.0D);
        register(override, "upswing", Codec.DOUBLE, 0.5D);
        register(override, "range_multiplier", Codec.FLOAT, 1.0F);
        register(override, "movement_speed_multiplier", Codec.FLOAT, 1.0F);
        register(override, "hitbox", Codec.STRING, HITBOX_SHAPES.get(0));
        register(override, "animation", Codec.STRING, "");
        register(override, "swing_sound", Codec.STRING, DEFAULT_SOUND);
        register(override, "rarity", StyleRarity.CODEC, StyleRarity.UNCOMMON);
        register(override, "cost_override", Codec.DOUBLE, 1.0D);
        register(override, "disabled", Codec.BOOL, true);
        register(override, "ignore_affinity", Codec.BOOL, true);
        register(override, "attributes",
                Codec.unboundedMap(Codec.STRING, AttackAttribute.CODEC), Map.of());
        register(override, "cooldown_scale", AttackAttribute.CODEC,
                AttackAttribute.flat(1.0D));
        register(override, "hastens_next", AttackAttribute.CODEC,
                AttackAttribute.flat(1.0D));
        register(override, "scale_animation", Codec.BOOL, true);
        register(override, KEY_CONDITIONS, Codec.STRING.listOf(),
                List.of(AttackConditions.names().getFirst()));

        String slashes = join(ROOT_BEHAVIOUR, "slashes");
        register(slashes, "particle_type", Codec.STRING, DEFAULT_PARTICLE);
        register(slashes, "x", Codec.DOUBLE, 0.5D);
        register(slashes, "y", Codec.DOUBLE, 0.5D);
        register(slashes, "z", Codec.DOUBLE, 0.5D);
        register(slashes, "local_yaw", Codec.DOUBLE, 15.0D);
        register(slashes, "pitch", Codec.DOUBLE, 15.0D);
        register(slashes, "roll", Codec.DOUBLE, 15.0D);
        register(slashes, "delay_ticks", Codec.INT, 2);
        register(slashes, "anchor", Codec.STRING, SlashAnchor.HITBOX.getSerializedName());
    }

    private static void registerEcho() {
        register(ROOT_ECHO, "times", Codec.INT, 1);
        register(ROOT_ECHO, "delay_ticks", Codec.INT, DEFAULT_ECHO_DELAY);
        register(ROOT_ECHO, "ratio", Codec.FLOAT, 1.0F);
    }

    private static void registerTuning() {
        register(ROOT_TUNING, "cost_modifier", Codec.DOUBLE, 1.0D);
        register(ROOT_TUNING, "damage_multiplier", Codec.DOUBLE, 1.0D);
        register(ROOT_TUNING, "chance", Codec.DOUBLE, 1.0D);
        register(ROOT_TUNING, "cooldown", Codec.INT, DEFAULT_TUNING_COOLDOWN);
        register(ROOT_TUNING, "weight", Codec.INT, DEFAULT_TUNING_WEIGHT);
        register(ROOT_TUNING, "rarity", StyleRarity.CODEC, StyleRarity.UNCOMMON);
        register(ROOT_TUNING, "trail", StyleAffix.TrailColor.CODEC,
                new StyleAffix.TrailColor(DEFAULT_TRAIL_COLOR, true));
        register(ROOT_TUNING, "applies_to", StyleAffix.AppliesTo.CODEC,
                StyleAffix.AppliesTo.ANY);
        register(ROOT_TUNING, "on_hit", AffixEffects.OnHit.CODEC, AffixEffects.OnHit.EMPTY);
        register(ROOT_TUNING, "on_swing", AffixEffects.OnHit.CODEC, AffixEffects.OnHit.EMPTY);
    }

    public static List<Template> addableAt(String path, JsonElement root) {
        return addableAt(path, root, Trigger.ANY);
    }

    public static List<Template> addableAt(String path, JsonElement root, Trigger trigger) {
        return addableAt(ROOT_EFFECT, path, root, trigger);
    }

    public static String join(String schemaRoot, String path) {
        String node = path == null ? "" : path;
        if (schemaRoot == null || schemaRoot.isEmpty()) {
            return node;
        }
        return node.isEmpty() ? schemaRoot : schemaRoot + "/" + node;
    }

    public static List<Template> addableAt(String schemaRoot, String path,
                                           JsonElement root, Trigger trigger) {
        return addableAt(schemaRoot, path, root, trigger, Context.NONE);
    }

    public record Context(List<String> allowed, String selector, String selectorValue, String scope) {
        public static final Context NONE = new Context(null, null, null, OptionSources.SCOPE_SELF);

        public Context withAllowed(List<String> paths) {
            return new Context(paths, selector, selectorValue, scope);
        }

        public Context withSource(String key, String value, String sourceScope) {
            return new Context(allowed, key, value,
                    sourceScope == null ? OptionSources.SCOPE_SELF : sourceScope);
        }
    }

    private static final String MAP_ENTRY = "*";
    private static final Map<String, String> MOUNTS = new LinkedHashMap<>();
    private static final Map<String, String> OPTION_MOUNTS = new LinkedHashMap<>();

    private record Rebased(String schemaRoot, String path, String at) {
    }

    private static void mount(String schemaPath, String mountRoot) {
        MOUNTS.put(schemaPath, mountRoot);
    }

    private static String optionMount(String key) {
        return OPTION_MOUNTS.computeIfAbsent(key, missing -> {
            String found = OptionSources.mountOf(missing);
            return found == null ? "" : found;
        });
    }

    private static Rebased rebased(String schemaRoot, String path) {
        String[] parts = segmentsOf(path);
        for (int index = 0; index < parts.length; index++) {
            String prefix = joined(parts, 0, index + 1);
            String mounted = MOUNTS.get(join(schemaRoot, withoutIndex(keyedNormalized(schemaRoot, prefix))));
            if (mounted == null && index > 0 && OptionSources.KEY_OPTIONS.equals(parts[index - 1])) {
                String option = optionMount(parts[index]);
                mounted = option.isEmpty() ? null : option;
            }
            if (mounted == null) {
                continue;
            }
            int end = index + 1;
            if (end < parts.length && isIndex(parts[end])) {
                end++;
            } else if (end == parts.length) {
                return null;
            }
            String at = joined(parts, 0, end);
            String rest = joined(parts, end, parts.length);
            Rebased inner = rebased(mounted, rest);
            return inner == null
                    ? new Rebased(mounted, rest, at)
                    : new Rebased(inner.schemaRoot(), inner.path(), at + PATH_SEPARATOR + inner.at());
        }
        return null;
    }
    private static final Map<String, String> KEYED_MAPS = new LinkedHashMap<>();

    private static void keyedMap(String schemaPath, String selector) {
        KEYED_MAPS.put(schemaPath, selector);
    }

    private static String[] segmentsOf(String path) {
        String node = path == null ? "" : (path.startsWith(PATH_SEPARATOR) ? path.substring(1) : path);
        return node.isEmpty() ? new String[0] : node.split(PATH_SEPARATOR);
    }

    private static String keyedNormalized(String schemaRoot, String path) {
        StringBuilder walked = new StringBuilder();
        StringBuilder normalized = new StringBuilder();
        for (String part : segmentsOf(path)) {
            String schemaPath = join(schemaRoot, withoutIndex(walked.toString()));
            String shown = KEYED_MAPS.containsKey(schemaPath) && !isIndex(part) ? MAP_ENTRY : part;
            if (!walked.isEmpty()) {
                walked.append(PATH_SEPARATOR);
                normalized.append(PATH_SEPARATOR);
            }
            walked.append(part);
            normalized.append(shown);
        }
        return normalized.toString();
    }

    private record KeyedEntry(String selector, String value, String rest, String mapKey) {
    }

    private static KeyedEntry keyedEntryOf(String schemaRoot, String path) {
        String[] parts = segmentsOf(path);
        StringBuilder walked = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            String selector = KEYED_MAPS.get(join(schemaRoot, withoutIndex(walked.toString())));
            if (selector != null && !isIndex(parts[index])) {
                StringBuilder rest = new StringBuilder();
                for (int after = index + 1; after < parts.length; after++) {
                    if (!rest.isEmpty()) {
                        rest.append(PATH_SEPARATOR);
                    }
                    rest.append(parts[after]);
                }
                return new KeyedEntry(selector, parts[index], rest.toString(),
                        index == 0 ? "" : parts[index - 1]);
            }
            if (!walked.isEmpty()) {
                walked.append(PATH_SEPARATOR);
            }
            walked.append(parts[index]);
        }
        return null;
    }

    private record OptionSource(String selector, String value) {
        private static final OptionSource NONE = new OptionSource(null, null);
    }

    private record Anchor(OptionSource source, String scope, String rest) {
    }

    private static String joined(String[] parts, int from, int to) {
        return String.join(PATH_SEPARATOR, Arrays.copyOfRange(parts, from, Math.max(from, to)));
    }

    private static Anchor anchorAt(String schemaRoot, String path, JsonElement root, Context context) {
        KeyedEntry entry = keyedEntryOf(schemaRoot, path);
        if (entry != null) {
            return new Anchor(new OptionSource(entry.selector(), entry.value()), entry.mapKey(), entry.rest());
        }
        String[] parts = segmentsOf(path);
        for (int depth = parts.length; depth >= 0; depth--) {
            JsonElement node = JsonTreeModel.resolve(root, joined(parts, 0, depth));
            if (node == null || !node.isJsonObject()) {
                continue;
            }
            for (String selector : OptionSources.selectors()) {
                JsonElement value = node.getAsJsonObject().get(selector);
                if (value == null || !value.isJsonPrimitive()) {
                    continue;
                }
                OptionSource source = new OptionSource(selector, value.getAsString());
                return depth == parts.length
                        ? new Anchor(source, OptionSources.SCOPE_SELF, "")
                        : new Anchor(source, parts[depth], joined(parts, depth + 1, parts.length));
            }
        }
        Context resolved = context == null ? Context.NONE : context;
        return resolved.selector() == null
                ? null
                : new Anchor(new OptionSource(resolved.selector(), resolved.selectorValue()),
                        resolved.scope(), joined(parts, 0, parts.length));
    }

    private static List<Template> keyedMapEntries(String schemaRoot, String path, JsonObject object) {
        String[] parts = segmentsOf(path);
        String selector = KEYED_MAPS.get(join(schemaRoot, withoutIndex(keyedNormalized(schemaRoot, path))));
        if (selector == null || object == null || parts.length == 0) {
            return null;
        }
        String mapKey = parts[parts.length - 1];
        List<Template> entries = new ArrayList<>();
        for (String value : OptionSources.values(selector)) {
            if (object.has(value)) {
                continue;
            }
            JsonElement made = OptionSources.template(selector, value, OptionSources.SCOPE_SELF, mapKey);
            entries.add(new Template(value, made == null ? new JsonObject() : made));
        }
        return entries;
    }

    private static OptionSource optionSourceAt(String schemaRoot, String optionsPath,
                                               JsonElement root, Context context) {
        String[] parts = segmentsOf(optionsPath);
        if (parts.length == 0 || !OptionSources.KEY_OPTIONS.equals(parts[parts.length - 1])) {
            return OptionSource.NONE;
        }
        String ownerPath = String.join(PATH_SEPARATOR, Arrays.copyOf(parts, parts.length - 1));
        KeyedEntry entry = keyedEntryOf(schemaRoot, ownerPath);
        if (entry != null && entry.rest().isEmpty()) {
            return new OptionSource(entry.selector(), entry.value());
        }
        String[] owner = segmentsOf(ownerPath);
        for (int depth = owner.length; depth >= 0; depth--) {
            JsonElement node = JsonTreeModel.resolve(root,
                    String.join(PATH_SEPARATOR, Arrays.copyOf(owner, depth)));
            if (node == null || !node.isJsonObject()) {
                continue;
            }
            for (String selector : OptionSources.selectors()) {
                JsonElement value = node.getAsJsonObject().get(selector);
                if (value != null && value.isJsonPrimitive()) {
                    return new OptionSource(selector, value.getAsString());
                }
            }
        }
        Context resolved = context == null ? Context.NONE : context;
        return new OptionSource(resolved.selector(), resolved.selectorValue());
    }

    public static List<OptionField> optionFieldsAt(String schemaRoot, String optionsPath,
                                                   JsonElement root, Context context) {
        OptionSource source = optionSourceAt(schemaRoot, optionsPath, root, context);
        return OptionSources.fieldsFor(source.selector(), source.value());
    }

    public static JsonObject optionsPrefilledAt(String schemaRoot, String optionsPath,
                                                JsonElement root, Context context) {
        OptionSource source = optionSourceAt(schemaRoot, optionsPath, root, context);
        return OptionSources.prefilled(source.selector(), source.value());
    }

    public static boolean permitted(List<String> allowed, String path) {
        return OptionSources.permitted(allowed, path);
    }

    public static List<Template> addableAt(String schemaRoot, String path,
                                           JsonElement root, Trigger trigger,
                                           Context context) {
        ensureLoaded();
        Rebased mountedAt = rebased(schemaRoot, path);
        if (mountedAt != null) {
            return addableAt(mountedAt.schemaRoot(), mountedAt.path(),
                    JsonTreeModel.resolve(root, mountedAt.at()), trigger, Context.NONE);
        }
        Context resolved = context == null ? Context.NONE : context;
        List<String> allowed = resolved.allowed();
        JsonElement node = JsonTreeModel.resolve(root, path == null ? "" : path);
        JsonObject object = node != null && node.isJsonObject() ? node.getAsJsonObject() : null;
        String[] parts = segmentsOf(path);
        if (parts.length > 0 && OptionSources.KEY_OPTIONS.equals(parts[parts.length - 1])) {
            List<Template> fields = new ArrayList<>();
            for (OptionField field : optionFieldsAt(schemaRoot, path, root, resolved)) {
                if (object == null || !object.has(field.key())) {
                    fields.add(new Template(field.key(), field.sample().deepCopy()));
                }
            }
            return fields;
        }
        List<Template> mapEntries = keyedMapEntries(schemaRoot, path, object);
        if (mapEntries != null) {
            return mapEntries;
        }
        List<Template> candidates = TEMPLATES.get(join(schemaRoot, path));
        if (candidates == null) {
            candidates = TEMPLATES.get(join(schemaRoot,
                    withoutIndex(keyedNormalized(schemaRoot, path))));
        }
        if (candidates == null) {
            return List.of();
        }
        Anchor anchor = anchorAt(schemaRoot, path, root, resolved);
        List<String> chosenAllowed = anchor == null
                ? null
                : OptionSources.allowed(anchor.source().selector(), anchor.source().value(), anchor.scope());
        ScriptedAbility.ActionType action = actionTypeAt(schemaRoot, path, object);
        List<Template> result = new ArrayList<>();
        for (Template template : candidates) {
            if (object != null && object.has(template.key())) {
                continue;
            }
            if (action != null && !action.reads(template.key())) {
                continue;
            }
            String nodePath = path == null || path.isEmpty()
                    ? template.key()
                    : path + PATH_SEPARATOR + template.key();
            if (!permitted(allowed, nodePath)) {
                continue;
            }
            if (chosenAllowed != null && !permitted(chosenAllowed, anchor.rest().isEmpty()
                    ? template.key()
                    : anchor.rest() + PATH_SEPARATOR + template.key())) {
                continue;
            }
            if (anchor != null && anchor.rest().isEmpty()) {
                JsonElement chosen = OptionSources.template(anchor.source().selector(),
                        anchor.source().value(), anchor.scope(), template.key());
                if (chosen != null) {
                    template = new Template(template.key(), chosen);
                }
            }
            if (OptionSources.KEY_OPTIONS.equals(template.key())) {
                JsonObject prefilled = optionsPrefilledAt(schemaRoot, nodePath, root, resolved);
                if (prefilled.isEmpty()) {
                    continue;
                }
                template = new Template(template.key(), prefilled);
            }
            if (!allows(template.key(), trigger)) {
                continue;
            }
            if (blockedByExclusive(template.key(), object)) {
                continue;
            }
            result.add(template);
        }
        return result;
    }

    public static ScriptedAbility.ActionType actionTypeAt(String schemaRoot, String path,
                                                          JsonObject node) {
        if (!ROOT_SUMMON_ABILITY.equals(schemaRoot) || path == null || node == null
                || !KEY_ACTION.equals(path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1))
                || !node.has(ScriptedAbility.KEY_TYPE)) {
            return null;
        }
        JsonElement type = node.get(ScriptedAbility.KEY_TYPE);
        return type.isJsonPrimitive() ? ScriptedAbility.ActionType.byName(type.getAsString()) : null;
    }

    private static final List<List<String>> EXCLUSIVE = List.of(
            List.of("on_miss", "on_kill", "on_summon_hit", "on_swing", "on_player_hit",
                    "on_projectile_hit", SummonGate.ON_SPAWN, SummonGate.ON_DEATH,
                    SummonGate.ON_KILL),
            List.of("slot_index", "slots", "slot_band", "last_slot"),
            List.of("style_id", "affix_id"),
            List.of("any_of", "all_of"));

    private static final List<String> SLOT_BOUND = List.of("slot_index", "slots", "slot_band",
            "last_slot", "hitbox", "every_nth_swing");

    private static boolean blockedByExclusive(String key, JsonObject object) {
        if (object == null) {
            return false;
        }
        if (object.has("on_projectile_hit") && SLOT_BOUND.contains(key)) {
            return true;
        }
        for (List<String> group : EXCLUSIVE) {
            if (!group.contains(key)) {
                continue;
            }
            for (String sibling : group) {
                if (!sibling.equals(key) && object.has(sibling)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean allows(String key, Trigger trigger) {
        if (trigger == null || trigger == Trigger.ANY) {
            return true;
        }
        Trigger required = TRIGGERS.get(key);
        return required == null || required == Trigger.ANY || required == trigger;
    }

    private static String withoutIndex(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder kept = new StringBuilder();
        for (String part : path.split("/")) {
            if (part.isEmpty() || isIndex(part)) {
                continue;
            }
            if (!kept.isEmpty()) {
                kept.append('/');
            }
            kept.append(part);
        }
        return kept.toString();
    }

    static boolean isIndex(String part) {
        if (part == null || part.isEmpty()) {
            return false;
        }
        if (part.charAt(0) == '[') {
            return true;
        }
        for (int i = 0; i < part.length(); i++) {
            if (!Character.isDigit(part.charAt(i))) {
                return false;
            }
        }
        return !part.isEmpty();
    }

    public static JsonElement elementFor(String schemaRoot, String path) {
        Template template = templateAt(schemaRoot, path);
        if (template == null) {
            return null;
        }
        JsonElement value = template.value();
        if (value != null && value.isJsonArray() && !value.getAsJsonArray().isEmpty()) {
            return value.getAsJsonArray().get(0).deepCopy();
        }
        return null;
    }

    public static boolean knowsField(String schemaRoot, String path) {
        return templateAt(schemaRoot, path) != null;
    }

    private static Template templateAt(String schemaRoot, String path) {
        ensureLoaded();
        if (path == null || path.isEmpty()) {
            return null;
        }
        Rebased mountedAt = rebased(schemaRoot, path);
        if (mountedAt != null) {
            return mountedAt.path().isEmpty() ? null : templateAt(mountedAt.schemaRoot(),
                    withoutIndex(keyedNormalized(mountedAt.schemaRoot(), mountedAt.path())));
        }
        int slash = path.lastIndexOf('/');
        String parent = slash < 0 ? "" : path.substring(0, slash);
        String key = slash < 0 ? path : path.substring(slash + 1);
        List<Template> candidates = TEMPLATES.get(join(schemaRoot, parent));
        if (candidates == null) {
            return null;
        }
        for (Template template : candidates) {
            if (template.key().equals(key)) {
                return template;
            }
        }
        return null;
    }

    public static boolean hasTemplates(String path) {
        return hasTemplates(ROOT_EFFECT, path);
    }

    public static boolean hasTemplates(String schemaRoot, String path) {
        ensureLoaded();
        Rebased mountedAt = rebased(schemaRoot, path);
        if (mountedAt != null) {
            return hasTemplates(mountedAt.schemaRoot(), mountedAt.path());
        }
        return TEMPLATES.containsKey(join(schemaRoot, path))
                || TEMPLATES.containsKey(join(schemaRoot, withoutIndex(path)));
    }
}

