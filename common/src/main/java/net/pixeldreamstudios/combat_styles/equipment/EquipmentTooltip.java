package net.pixeldreamstudios.combat_styles.equipment;

import net.pixeldreamstudios.combat_styles.affix.AffixGrants;
import net.pixeldreamstudios.combat_styles.custom.CustomScopes;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import net.pixeldreamstudios.combat_styles.entity.AbilityPick;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbilities;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbility;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.client.tooltip.AffixPlates;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.affix.EffectLines;
import net.pixeldreamstudios.combat_styles.affix.EquipmentAffix;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.item.HeldPreview;
import net.pixeldreamstudios.combat_styles.item.ItemTooltip;
import net.pixeldreamstudios.combat_styles.item.TooltipGem;
import net.pixeldreamstudios.combat_styles.item.TooltipIcon;
import net.pixeldreamstudios.combat_styles.item.TooltipContextAccess;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.StyleTooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class EquipmentTooltip {
    private static final String KEY_SPELL_GRANT = "tooltip.combat_styles.equip_spell_grant";
    private static final String KEY_SPELL_BLOCK = "tooltip.combat_styles.equip_spell_block";
    private static final String KEY_SPELL_TUNE = "tooltip.combat_styles.equip_spell_tune";
    private static final String KEY_SPELL_REMOVE = "tooltip.combat_styles.equip_spell_remove";
    private static final String KEY_SPELL_REPLACE = "tooltip.combat_styles.equip_spell_replace";
    private static final String KEY_GRANT_REMOVED = "tooltip.combat_styles.grant_removed";
    private static final String KEY_SCHOOL_BLOCK = "tooltip.combat_styles.equip_school_block";
    private static final String KEY_SCHOOL_REMOVE = "tooltip.combat_styles.equip_school_remove";
    private static final String KEY_REWRITES = "item.combat_styles.effect.rewrites";
    private static final String KEY_REWRITES_MORE = "item.combat_styles.effect.rewrites_more";
    private static final String KEY_KEEPS = "item.combat_styles.effect.keeps";
    private static final String KEY_AT_MOST = "item.combat_styles.effect.at_most";
    private static final String KEY_RESPAWNS = "item.combat_styles.effect.respawns";
    private static final String KEY_REPLACES = "tooltip.combat_styles.equip_replaces";
    private static final String KEY_ECHO = "tooltip.combat_styles.equip_echo";
    private static final String KEY_WORN = "tooltip.combat_styles.equip_worn";
    private static final String KEY_REPEAT = "tooltip.combat_styles.equip_repeat";
    private static final String KEY_COOLDOWN = "tooltip.combat_styles.equip_cooldown";
    private static final String KEY_GRANT_UNKNOWN = "tooltip.combat_styles.grant_unknown";
    private static final String KEY_TUNED_DAMAGE = "tooltip.combat_styles.tuned_damage";
    private static final String KEY_TUNED_COST = "tooltip.combat_styles.tuned_cost";
    private static final String KEY_TUNED_COOLDOWN = "tooltip.combat_styles.tuned_cooldown";
    private static final String KEY_TUNED_CHANCE = "tooltip.combat_styles.tuned_chance";
    private static final int GRANT_LINE_BUDGET = 8;
    private static final String KEY_WINDOW = "tooltip.combat_styles.equip_window";
    private static final String KEY_WINDOW_USES = "tooltip.combat_styles.equip_window_uses";
    private static final String KEY_WINDOW_FULL = "tooltip.combat_styles.equip_window_full";
    private static final String KEY_WINDOW_SPAN = "tooltip.combat_styles.equip_window_span";
    private static final String KEY_WINDOW_REFRESH = "tooltip.combat_styles.equip_window_refresh";
    private static final String KEY_SET_CHARGED = "tooltip.combat_styles.equip_set_charged";
    private static final String KEY_SET_RECHARGES = "tooltip.combat_styles.equip_set_recharges";
    private static final String KEY_REPLACE_HEADER = "tooltip.combat_styles.replace_header";
    private static final String KEY_REPLACE_ALL = "tooltip.combat_styles.replace_all";
    private static final String KEY_REPLACE_MAX = "tooltip.combat_styles.replace_max";
    private static final String KEY_REPLACE_DAMAGE = "tooltip.combat_styles.replace_damage";
    private static final String KEY_REPLACE_LIFE = "tooltip.combat_styles.replace_life";
    private static final String KEY_SUMMON_RECAST = "tooltip.combat_styles.summon_recast";
    private static final String KEY_SUMMON_PICK = "tooltip.combat_styles.summon_pick.";
    private static final String KEY_SUMMON_AIM = "tooltip.combat_styles.summon_aim";
    private static final String KEY_SUMMON_PASSIVE_PREFIX = "tooltip.combat_styles.summon_passive.";
    private static final String KEY_SUMMON_PASSIVE_COOLDOWN = "tooltip.combat_styles.summon_passive_cooldown";
    private static final String KEY_GRANT_PREFIX = "tooltip.combat_styles.grant_abilities.";
    private static final String KEY_GRANT_ACTIVES = "tooltip.combat_styles.grant_abilities_actives";
    private static final String KEY_SUMMON_AIM_PREFIX = "tooltip.combat_styles.summon_aim.";
    private static final String KEY_SUMMON_DIVE = "tooltip.combat_styles.summon_dive";
    private static final String KEY_GROUP_ALL = "tooltip.combat_styles.group_all";
    private static final String KEY_GROUP_PICKS = "tooltip.combat_styles.group_picks";
    private static final String KEY_GROUP_RANGE = "tooltip.combat_styles.group_range";
    private static final String KEY_GROUP_CHANCE = "tooltip.combat_styles.group_chance";
    private static final String KEY_GROUP_NONE = "tooltip.combat_styles.group_none";
    private static final String KEY_GROUP_WINDOW = "tooltip.combat_styles.group_window";
    private static final String KEY_GROUP_COOLDOWN = "tooltip.combat_styles.group_cooldown";
    private static final String KEY_GROUP_WINDOW_USES =
            "tooltip.combat_styles.group_window_uses";
    private static final String KEY_GROUP_ENTRY_CHANCE = "tooltip.combat_styles.group_entry_chance";

    private static final String KEY_ON_MISS = "tooltip.combat_styles.when_miss";
    private static final String KEY_ON_KILL = "tooltip.combat_styles.when_kill";
    private static final String KEY_ON_HURT = "tooltip.combat_styles.on_hurt";
    private static final String KEY_ON_SHOOT = "tooltip.combat_styles.on_shoot";
    private static final String KEY_ON_SHOOT_TYPED = "tooltip.combat_styles.on_shoot_typed";
    private static final String KEY_ON_HURT_TYPED = "tooltip.combat_styles.on_hurt_typed";
    private static final String KEY_ON_BLOCK_TYPED = "tooltip.combat_styles.on_shield_block_typed";
    private static final String KEY_ON_BLOCK = "tooltip.combat_styles.on_shield_block";
    private static final String KEY_ON_USE = "tooltip.combat_styles.on_use";
    private static final String KEY_SELF_EFFECT = "affix.combat_styles.effect.self";
    private static final String KEY_ON_USE_START = "tooltip.combat_styles.on_use_start";
    private static final String KEY_ON_USE_CHANNEL = "tooltip.combat_styles.on_use_channel";
    private static final String KEY_ON_USE_RELEASE = "tooltip.combat_styles.on_use_release";
    private static final String KEY_ON_PROJECTILE_HIT =
            "tooltip.combat_styles.on_projectile_hit";
    private static final String KEY_ON_PROJECTILE_HIT_TYPED =
            "tooltip.combat_styles.on_projectile_hit_typed";
    private static final String KEY_ON_PLAYER_HIT = "tooltip.combat_styles.on_player_hit";
    private static final String KEY_ON_SWING = "tooltip.combat_styles.on_swing";
    private static final String KEY_SPRINTING = "tooltip.combat_styles.while_sprinting";
    private static final String KEY_CROUCHING = "tooltip.combat_styles.while_crouching";
    private static final String KEY_STANDING = "tooltip.combat_styles.while_standing";
    private static final String KEY_AIRBORNE = "tooltip.combat_styles.while_airborne";
    private static final String KEY_GROUNDED = "tooltip.combat_styles.while_grounded";
    private static final String KEY_BEHIND = "tooltip.combat_styles.while_behind";
    private static final String KEY_NIGHT = "tooltip.combat_styles.while_night";
    private static final String KEY_DAY = "tooltip.combat_styles.while_day";
    private static final String KEY_DAMAGED_WITHIN = "tooltip.combat_styles.damaged_within";
    private static final String KEY_HAS_SPELL = "tooltip.combat_styles.has_spell";
    private static final String KEY_HAS_SPELL_SCHOOL = "tooltip.combat_styles.has_spell_school";
    private static final String KEY_SCHOOL_JOIN = "tooltip.combat_styles.school_join";
    private static final String KEY_CASTING_SPELL = "tooltip.combat_styles.casting_spell";
    private static final String KEY_CASTING_NAMED = "tooltip.combat_styles.casting_named";
    private static final String KEY_CAST_SPELL = "tooltip.combat_styles.cast_spell";
    private static final String KEY_CAST_NAMED = "tooltip.combat_styles.cast_named";
    private static final String KEY_CHANNEL_SPELL = "tooltip.combat_styles.channel_spell";
    private static final String KEY_CHANNEL_NAMED = "tooltip.combat_styles.channel_named";
    private static final String KEY_DAMAGE_ABOVE = "tooltip.combat_styles.damage_above";
    private static final String KEY_DAMAGE_BELOW = "tooltip.combat_styles.damage_below";
    private static final String KEY_DAMAGE_DEALT_TYPE =
            "tooltip.combat_styles.damage_dealt_type";
    private static final String KEY_AGAINST = "tooltip.combat_styles.against";
    private static final String KEY_AGAINST_AFFLICTED = "tooltip.combat_styles.against_afflicted";
    private static final String KEY_LAST_SLOT = "tooltip.combat_styles.when_last_slot";
    private static final String KEY_NOT_LAST_SLOT = "tooltip.combat_styles.when_not_last_slot";
    private static final String KEY_SLOT_FROM_LAST = "tooltip.combat_styles.slot_from_last";
    private static final String KEY_SLOT_EXCEPT = "tooltip.combat_styles.slot_except";
    private static final String KEY_SLOTS_EVERY = "tooltip.combat_styles.equip_slots_every";
    private static final String KEY_SLOTS_EVERY_EXCEPT = "tooltip.combat_styles.equip_slots_every_except";
    private static final String KEY_SLOTS = "tooltip.combat_styles.equip_slots";
    private static final String KEY_SLOT_RANGE = "tooltip.combat_styles.slot_range";
    private static final String KEY_SLOT_AND = "tooltip.combat_styles.slot_list_and";
    private static final String KEY_SLOT_LAST = "tooltip.combat_styles.slot_last";
    private static final String KEY_CHANGED = "tooltip.combat_styles.affix_changed";
    private static final String KEY_ORDINAL_1 = "tooltip.combat_styles.slot_ordinal_1";
    private static final String KEY_ORDINAL_2 = "tooltip.combat_styles.slot_ordinal_2";
    private static final String KEY_ORDINAL_3 = "tooltip.combat_styles.slot_ordinal_3";
    private static final String KEY_ORDINAL_N = "tooltip.combat_styles.slot_ordinal_n";
    private static final String KEY_NTH = "tooltip.combat_styles.when_nth";
    private static final String KEY_IDLE = "tooltip.combat_styles.when_idle";
    private static final String KEY_NEEDS_AFFIX = "tooltip.combat_styles.when_affix";
    private static final String KEY_ON_SUMMON_HIT = "tooltip.combat_styles.when_summon_hit";
    private static final String KEY_HITBOX_TRIGGER = "tooltip.combat_styles.when_hitbox";
    private static final String KEY_STYLE_TRIGGER = "tooltip.combat_styles.when_style";
    private static final String KEY_AFFIX_TRIGGER = "tooltip.combat_styles.when_affix_id";
    private static final String KEY_COND_AND = "tooltip.combat_styles.cond_and";
    private static final String KEY_COND_OR = "tooltip.combat_styles.cond_or";
    private static final String KEY_COND_GROUP = "tooltip.combat_styles.cond_group";
    private static final String KEY_WHEN_PREFIX = "tooltip.combat_styles.when_prefix";
    private static final String KEY_SELF_ABOVE = "tooltip.combat_styles.when_self_above";
    private static final String KEY_SELF_BELOW = "tooltip.combat_styles.when_self_below";
    private static final String KEY_TARGET_ABOVE = "tooltip.combat_styles.when_target_above";
    private static final String KEY_TARGET_BELOW = "tooltip.combat_styles.when_target_below";

    private static final String KEY_ON_SUMMON_SPAWN = "tooltip.combat_styles.on_summon_spawn";
    private static final String KEY_ON_SUMMON_SPAWN_TYPED = "tooltip.combat_styles.on_summon_spawn_typed";
    private static final String KEY_ON_SUMMON_DEATH = "tooltip.combat_styles.on_summon_death";
    private static final String KEY_ON_SUMMON_DEATH_TYPED = "tooltip.combat_styles.on_summon_death_typed";
    private static final String KEY_ON_SUMMON_KILL = "tooltip.combat_styles.on_summon_kill";
    private static final String KEY_ON_SUMMON_KILL_TYPED = "tooltip.combat_styles.on_summon_kill_typed";
    private static final String KEY_NOT_SPRINTING = "tooltip.combat_styles.while_not_sprinting";
    private static final String KEY_NOT_BEHIND = "tooltip.combat_styles.while_not_behind";
    private static final String KEY_NO_SPELL = "tooltip.combat_styles.no_spell";
    private static final String KEY_NOT_CASTING = "tooltip.combat_styles.not_casting";
    private static final String KEY_SUMMONS_ACTIVE = "tooltip.combat_styles.summons_active";
    private static final String KEY_HURT_ABOVE = "tooltip.combat_styles.hurt_above";
    private static final String KEY_HURT_BELOW = "tooltip.combat_styles.hurt_below";
    private static final String KEY_HURT_TOTAL = "tooltip.combat_styles.hurt_total";
    private static final String KEY_HURT_BY = "tooltip.combat_styles.hurt_by";
    private static final String KEY_NOT_FROM_SUMMON = "tooltip.combat_styles.not_from_summon";
    private static final String KEY_FULLY_BLOCKED = "tooltip.combat_styles.fully_blocked";
    private static final String KEY_PARTLY_BLOCKED = "tooltip.combat_styles.partly_blocked";
    private static final String KEY_USE_HELD_ABOVE = "tooltip.combat_styles.use_held_above";
    private static final String KEY_USE_HELD_BELOW = "tooltip.combat_styles.use_held_below";
    private static final String KEY_CAST_SCHOOL = "tooltip.combat_styles.cast_school";
    private static final String KEY_CAST_TYPE = "tooltip.combat_styles.cast_type";
    private static final String KEY_CAST_TRIGGERED = "tooltip.combat_styles.cast_triggered";
    private static final String KEY_PROJECTILE_FAR = "tooltip.combat_styles.projectile_far";
    private static final String KEY_PROJECTILE_NEAR = "tooltip.combat_styles.projectile_near";
    private static final String KEY_SPELL_PROJECTILE = "tooltip.combat_styles.spell_projectile";
    private static final String KEY_SPELL_PROJECTILE_NAMED =
            "tooltip.combat_styles.spell_projectile_named";
    private static final String KEY_DELAY = "tooltip.combat_styles.equip_delay";

    private static final String SECTION_JOIN = "/";
    private static final String SECTION_WORN = "worn";
    private static final String SECTION_HEADER = TooltipOverride.HEADER_SECTION;
    private static final String SECTION_ATTRIBUTES = "attributes";
    private static final String SECTION_TUNED = "tuned";
    private static final String SECTION_SUBSTITUTE = "substitute";
    private static final String SECTION_SUBSTITUTION = "substitution";
    private static final String SECTION_GRANTS = "grants";
    private static final String SECTION_SPELLS = "spells";
    private static final String SECTION_DAMAGE_BONUS = "damage_bonus";
    private static final String SECTION_SELF = "self_effects";
    private static final String SECTION_OWN = "own";
    private static final String SECTION_EVENTS = "events";
    private static final String SECTION_CUSTOM = "custom";
    private static final String SECTION_AUGMENT = "augment";
    private static final String SECTION_ECHO = "echo";
    private static final String SECTION_REPEAT = "repeat";
    private static final String SECTION_DIVE = "summon_dive";
    private static final String SECTION_GATE = "gate";
    private static final String SECTION_STACKING = "stacking";
    private static final String SECTION_DELAY = "delay";
    private static final String KEY_HOLD_WHILE_ACTIVE = "tooltip.combat_styles.equip_hold_while_active";
    private static final String KEY_SUMMONS_ENTITY = "item.combat_styles.effect.summons";
    private static final String KEY_SUMMON_ANCHORED = "item.combat_styles.effect.summon_anchored";
    private static final String KEY_SUMMON_DAMAGE = "item.combat_styles.effect.summon_damage";
    private static final String KEY_SUMMON_LIFE = "item.combat_styles.effect.summon_life";
    private static final String KEY_SUMMON_ABILITIES = "item.combat_styles.effect.summon_abilities";
    private static final String KEY_SUMMON_FINALE = "item.combat_styles.effect.summon_finale";
    private static final String KEY_SUMMON_DIVE_ABILITY = "tooltip.combat_styles.summon_dive_ability";
    private static final String KEY_SUMMON_DIVE_WHO = "tooltip.combat_styles.summon_dive_who";
    private static final String KEY_SUMMON_DIVE_MAX = "tooltip.combat_styles.summon_dive_max";
    private static final String KEY_SUMMON_DIVE_ONLY = "tooltip.combat_styles.summon_dive_only";
    private static final String KEY_TARGETING_PREFIX = "affix.combat_styles.targeting.";
    private static final String KEY_USE_CHARGE = "tooltip.combat_styles.use_charge";
    private static final String KEY_USE_CHARGE_AFTER = "tooltip.combat_styles.use_charge_after";
    private static final String KEY_USE_CHANNEL = "tooltip.combat_styles.use_channel";
    private static final String KEY_USE_CHANNEL_FOR = "tooltip.combat_styles.use_channel_for";

    private static final String KEY_TUNE_POWER = "tooltip.combat_styles.tune.power";
    private static final String KEY_TUNE_CRIT_CHANCE = "tooltip.combat_styles.tune.crit_chance";
    private static final String KEY_TUNE_CRIT_DAMAGE = "tooltip.combat_styles.tune.crit_damage";
    private static final String KEY_TUNE_RICOCHET = "tooltip.combat_styles.tune.ricochet";
    private static final String KEY_TUNE_RICOCHET_RANGE = "tooltip.combat_styles.tune.ricochet_range";
    private static final String KEY_TUNE_PIERCE = "tooltip.combat_styles.tune.pierce";
    private static final String KEY_TUNE_BOUNCE = "tooltip.combat_styles.tune.bounce";
    private static final String KEY_TUNE_RANGE = "tooltip.combat_styles.tune.range";
    private static final String KEY_TUNE_CHANNEL = "tooltip.combat_styles.tune.channel";
    private static final String KEY_TUNE_EFFECT_DURATION = "tooltip.combat_styles.tune.effect_duration";
    private static final String KEY_TUNE_EFFECT_LEVEL = "tooltip.combat_styles.tune.effect_level";
    private static final String KEY_TUNE_KNOCKBACK = "tooltip.combat_styles.tune.knockback";
    private static final String KEY_TUNE_COOLDOWN = "tooltip.combat_styles.tune.cooldown";
    private static final String KEY_TUNE_CHAIN = "tooltip.combat_styles.tune.chain";
    private static final String KEY_TUNE_LAUNCH = "tooltip.combat_styles.tune.launch";
    private static final String KEY_TUNE_IMPACTS = "tooltip.combat_styles.tune.impacts";
    private static final String KEY_TUNE_TRIGGER_CHANCE = "tooltip.combat_styles.tune.trigger_chance";
    private static final String PLUS = "+";
    private static final double PERCENT = 100.0D;

    private static final String INDENT = "  ";
    private static final String DESCRIPTION_JOIN = ": ";
    private static final String KEY_STACKING = "tooltip.combat_styles.stacking";
    private static final int SECOND = 20;
    private static final double SURE = 0.9995D;
    private static final int MAX_HEADER_ICONS = 3;

    private static final ChatFormatting HEADER = ChatFormatting.GOLD;
    private static final ChatFormatting BODY = ChatFormatting.GRAY;
    private static final ChatFormatting GOOD = ChatFormatting.GREEN;
    private static final ChatFormatting BAD = ChatFormatting.RED;
    private static final ChatFormatting COND = ChatFormatting.AQUA;
    private static final ChatFormatting FRAME = ChatFormatting.DARK_GRAY;
    private static final int PLATE_COLOUR = 0xFF8A6E3C;

    private static ItemStack previewStack;
    private static EquipmentDefinition previewDefinition;

    private EquipmentTooltip() {
    }

    public static <T> T previewing(ItemStack stack, EquipmentDefinition definition,
                                   Supplier<T> body) {
        ItemStack priorStack = previewStack;
        EquipmentDefinition priorDefinition = previewDefinition;
        previewStack = stack;
        previewDefinition = definition;
        try {
            return HeldPreview.showing(stack, body);
        } finally {
            previewStack = priorStack;
            previewDefinition = priorDefinition;
        }
    }

    public static EquipmentDefinition shownFor(ItemStack stack) {
        if (stack != null && stack == previewStack) {
            return previewDefinition;
        }
        return EquipmentProviders.definitionFor(stack);
    }

    public static void append(EquipmentDefinition definition, List<Component> tooltip) {
        if (definition == null) {
            return;
        }
        AffixPlates.beginTooltip();
        Set<EffectGroup> grouped = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean shown = appendDefinitionLines(definition, tooltip);
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (named.getValue().hideTooltip()) {
                continue;
            }
            EffectGroup group = definition.groupOf(named.getKey());
            if (group != null) {
                if (grouped.add(group)) {
                    shown |= appendGroup(definition, group, tooltip);
                }
                continue;
            }
            List<Component> lines = new ArrayList<>();
            appendEffect(definition, named.getValue(),
                    !definition.isPermanent(named.getKey(), named.getValue()),
                    lines,
                    named.getValue().trigger().chance().orElse(EffectGroup.ALWAYS));
            if (lines.isEmpty()) {
                continue;
            }
            tooltip.add(Component.empty());
            tooltip.addAll(lines);
            shown = true;
        }
        if (shown && !TooltipContextAccess.expanded()) {
            tooltip.add(ItemTooltip.equipmentHint());
        }
    }

    private static boolean showsActiveIn(EquipmentDefinition definition) {
        return !definition.hideScopeTooltip() && definition.activeIn() != ActiveIn.ANY;
    }

    private static boolean reachesBothHands(EquipmentDefinition definition) {
        return !definition.hideScopeTooltip() && definition.swings() == SwingHands.ANY
                && definition.activeIn() != ActiveIn.WORN;
    }

    public static boolean describes(EquipmentDefinition definition) {
        return definition != null && (!definition.effects().isEmpty()
                || showsActiveIn(definition)
                || reachesBothHands(definition)
                || definition.use().channels()
                || !definition.use().cooldown().isEmpty());
    }

    private static boolean appendDefinitionLines(EquipmentDefinition definition,
                                                 List<Component> tooltip) {
        List<Component> lines = new ArrayList<>();
        if (showsActiveIn(definition)) {
            lines.add(TooltipGem.NOTE.bullet(Component.translatable(
                    definition.activeIn().tooltipKey()).withStyle(COND)));
        }
        if (reachesBothHands(definition)) {
            lines.add(TooltipGem.NOTE.bullet(Component.translatable(
                    definition.swings().tooltipKey()).withStyle(COND)));
        }
        appendUse(definition.use(), lines);
        if (lines.isEmpty()) {
            return false;
        }
        tooltip.add(Component.empty());
        tooltip.addAll(lines);
        return true;
    }

    private static void appendUse(UseSpec use, List<Component> tooltip) {
        Component head = null;
        if (use.kind() == UseKind.CHARGE) {
            head = TooltipGem.NOTE.bullet((use.thresholdTicks() > 0
                    ? Component.translatable(KEY_USE_CHARGE_AFTER, seconds(use.thresholdTicks()))
                    : Component.translatable(KEY_USE_CHARGE)).withStyle(BODY));
        } else if (use.kind() == UseKind.CHANNEL) {
            Component every = Component.translatable(KEY_USE_CHANNEL,
                    seconds(use.channelTicks()));
            head = TooltipGem.NOTE.bullet((use.infinite() || use.holdTicks() <= 0
                    ? every
                    : Component.translatable(KEY_USE_CHANNEL_FOR, every, seconds(use.holdTicks())))
                    .copy().withStyle(BODY));
        }
        List<Component> gate = new ArrayList<>();
        appendGate(use.cooldown(), gate);
        appendUnder(head, gate, List.of(), tooltip);
    }

    private static boolean appendGroup(EquipmentDefinition definition, EffectGroup group,
                                       List<Component> tooltip) {
        Map<String, Double> odds = group.odds();
        List<Component> members = new ArrayList<>();
        for (EffectGroup.Entry entry : group.entries()) {
            TriggeredEffect member = definition.effects().get(entry.effect());
            if (member == null || member.hideTooltip()) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            boolean headed = appendEffect(definition, member,
                    !definition.isPermanent(entry.effect(), member),
                    lines, odds.getOrDefault(entry.effect(), 0.0D));
            if (lines.isEmpty()) {
                continue;
            }
            members.add(lines.getFirst());
            for (Component line : lines.subList(1, lines.size())) {
                members.add(headed ? TooltipGem.hang(line) : line);
            }
        }
        if (members.isEmpty()) {
            return false;
        }
        tooltip.add(Component.empty());
        Component when = group.tooltipHeader().isBlank() ? triggerOf(group.trigger()) : null;
        if (when != null) {
            tooltip.add(headerLine(group.trigger(), when));
        }
        tooltip.add(groupHeader(group));
        for (Component line : members) {
            tooltip.add(Component.literal(INDENT).append(line));
        }
        return true;
    }

    private static Component withOdds(Component line, double chance) {
        if (chance >= SURE) {
            return line;
        }
        return line.copy().append(Component.literal(INDENT)
                .append(Component.translatable(KEY_GROUP_ENTRY_CHANCE,
                        StyleText.formatCompact(chance * 100.0D)))
                .withStyle(FRAME));
    }

    private static Component groupHeader(EffectGroup group) {
        if (!group.tooltipHeader().isBlank()) {
            return TooltipOverride.text(group.tooltipHeader()).withStyle(COND);
        }
        int total = group.entries().size();
        MutableComponent line;
        if (group.fixed()) {
            line = Component.translatable(KEY_GROUP_PICKS, group.picks().orElse(total), total);
        } else if (group.ranged()) {
            int low = group.lowestPicks();
            int high = group.highestPicks();
            line = low == high
                    ? Component.translatable(KEY_GROUP_PICKS, low, total)
                    : Component.translatable(KEY_GROUP_RANGE, low, high, total);
        } else {
            line = Component.translatable(KEY_GROUP_ALL);
        }
        if (group.noneChance() > 0.0F) {
            line = Component.translatable(KEY_GROUP_NONE, line,
                    StyleText.formatCompact(group.noneChance() * 100.0D));
        }
        if (group.chance() < EffectGroup.ALWAYS) {
            line = Component.translatable(KEY_GROUP_CHANCE,
                    StyleText.formatCompact(group.chance() * 100.0D), line);
        }
        if (group.windowed()) {
            line = group.limited()
                    ? Component.translatable(KEY_GROUP_WINDOW_USES,
                            line, StyleText.formatCompact(group.duration() / (double) SECOND), group.uses())
                    : Component.translatable(KEY_GROUP_WINDOW, line,
                            StyleText.formatCompact(group.duration() / (double) SECOND));
        }
        if (group.onCooldown()) {
            line = Component.translatable(KEY_GROUP_COOLDOWN, line,
                    StyleText.formatCompact(group.cooldown().ticks() / (double) SECOND));
        }
        return line.withStyle(COND);
    }

    private static void appendSummonDive(SummonDive dive, List<Component> tooltip) {
        if (dive == null) {
            return;
        }
        Component head = TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_DIVE,
                StyleText.formatDecimal(dive.damageShare())).withStyle(BODY));
        List<Component> always = new ArrayList<>();
        List<Component> order = new ArrayList<>();
        dive.ability().filter(ability -> !ability.isBlank()).ifPresent(ability -> {
            always.add(TooltipGem.SUMMON.bullet(Component.translatable(
                    KEY_SUMMON_DIVE_ABILITY, abilityName(ability)).withStyle(BODY)));
            appendAbilityDescriptions(List.of(ability), always);
        });
        if (dive.targeting() != SummonTargeting.ALL) {
            order.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_DIVE_WHO,
                    Component.translatable(KEY_TARGETING_PREFIX + dive.targeting().key()))
                    .withStyle(BODY)));
        }
        if (dive.maxDiving() > SummonDive.ALL_DIVING) {
            order.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_DIVE_MAX,
                    dive.maxDiving()).withStyle(BODY)));
        }
        if (!dive.entities().isEmpty()) {
            order.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_DIVE_ONLY,
                    entityNamesOf(dive.entities())).withStyle(BODY)));
        }
        dive.aim().ifPresent(aim -> order.add(TooltipGem.SUMMON.bullet(
                Component.translatable(KEY_SUMMON_AIM,
                        Component.translatable(KEY_SUMMON_AIM_PREFIX + aim.getSerializedName()))
                        .withStyle(BODY))));
        appendUnder(head, always, order, tooltip);
    }

    private static void appendSummonReplacement(SummonProfile replacement,
                                                List<Component> tooltip) {
        if (replacement == null || !replacement.replacesSummons()) {
            return;
        }
        if (!tooltip.isEmpty()) {
            tooltip.add(Component.empty());
        }
        tooltip.add(Component.translatable(KEY_REPLACE_HEADER).withStyle(HEADER));
        tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_REPLACE_ALL,
                StyleText.registryName(replacement.entity().orElse(""))).withStyle(BODY)));

        replacement.maxActive().ifPresent(max -> tooltip.add(TooltipGem.SUMMON.bullet(
                Component.translatable(KEY_REPLACE_MAX, max).withStyle(BODY))));

        replacement.damageMultiplier().ifPresent(multiplier -> {
            if (multiplier != 1.0F) {
                tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_REPLACE_DAMAGE,
                        StyleText.formatDecimal(multiplier)).withStyle(GOOD)));
            }
        });

        replacement.lifespan().ifPresent(ticks -> tooltip.add(TooltipGem.SUMMON.bullet(
                Component.translatable(KEY_REPLACE_LIFE,
                        StyleText.formatDecimal(ticks / (double) SECOND)).withStyle(BODY))));

        appendCadence(replacement.cadence(), replacement.abilities(), tooltip);
        replacement.finale().filter(finale -> !finale.isBlank()).ifPresent(finale -> {
            tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(
                    KEY_SUMMON_FINALE, abilityName(finale)).withStyle(BODY)));
            appendAbilityDescriptions(List.of(finale), tooltip);
        });
    }

    private static void appendCadence(SummonCadence cadence, List<String> abilities,
                                      List<Component> tooltip) {
        int recast = cadence.recastTicks().orElse(SummonCadence.NEVER_RECASTS);
        if (recast > 0 && !abilities.isEmpty()) {
            AbilityPick pick = cadence.abilityPick().orElse(AbilityPick.IN_ORDER);
            tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_RECAST,
                    abilityNames(abilities),
                    StyleText.formatDecimal(recast / (double) SECOND),
                    Component.translatable(KEY_SUMMON_PICK + pick.getSerializedName()))
                    .withStyle(BODY)));
            appendAbilityDescriptions(abilities, tooltip);
        }
        for (SummonPassive passive : cadence.passives()) {
            if (passive.ability().isBlank()) {
                continue;
            }
            MutableComponent line = Component.translatable(
                    KEY_SUMMON_PASSIVE_PREFIX + passive.trigger().getSerializedName(),
                    abilityName(passive.ability()));
            if (passive.odds() < SummonPassive.ALWAYS) {
                line.append(Component.literal(INDENT)).append(Component.translatable(
                        KEY_GROUP_ENTRY_CHANCE,
                        StyleText.formatCompact(passive.odds() * PERCENT)).withStyle(FRAME));
            }
            int cooldown = Math.max(passive.cooldown(),
                    AbilityConfig.get(passive.ability()).cooldown());
            if (cooldown > SummonPassive.NO_COOLDOWN) {
                line.append(Component.literal(INDENT)).append(Component.translatable(
                        KEY_SUMMON_PASSIVE_COOLDOWN, seconds(cooldown))
                        .withStyle(FRAME));
            }
            tooltip.add(TooltipGem.SUMMON.bullet(line.withStyle(BODY)));
            appendAbilityDescriptions(List.of(passive.ability()), tooltip);
        }
        cadence.aim().ifPresent(aim -> tooltip.add(TooltipGem.SUMMON.bullet(
                Component.translatable(KEY_SUMMON_AIM,
                        Component.translatable(KEY_SUMMON_AIM_PREFIX + aim.getSerializedName()))
                        .withStyle(BODY))));
    }

    private static final String ABILITY_NAME_PREFIX = "ability.";
    private static final String ABILITY_NAME_SUFFIX = ".name";
    private static final String ABILITY_DESC_SUFFIX = ".desc";

    private static StyleAffix affixAbility(String ability) {
        String key = AbilityConfig.key(ability);
        StyleAffix affix = ability != null && ability.contains(":")
                ? AffixRegistry.get(ability)
                : null;
        if (affix != null) {
            return affix;
        }
        for (StyleAffix candidate : AffixRegistry.all()) {
            if (AbilityConfig.key(candidate.serializedId()).equals(key)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> abilityLangKeys(String ability, String suffix) {
        String key = AbilityConfig.key(ability);
        List<String> keys = new ArrayList<>();
        ResourceLocation id = ability == null ? null : ResourceLocation.tryParse(ability.trim());
        if (id != null && ability.contains(":")) {
            keys.add(ABILITY_NAME_PREFIX + id.getNamespace() + "." + id.getPath() + suffix);
        }
        keys.add(ABILITY_NAME_PREFIX + CombatStyles.MOD_ID + "." + key + suffix);
        return keys;
    }

    private static Optional<String> presentLangKey(List<String> keys) {
        Language language = Language.getInstance();
        return keys.stream().filter(language::has).findFirst();
    }

    private static Component abilityName(String ability) {
        StyleAffix affix = affixAbility(ability);
        if (affix != null) {
            return Component.translatableWithFallback(affix.nameKey(), affix.fallbackName());
        }
        List<String> keys = abilityLangKeys(ability, ABILITY_NAME_SUFFIX);
        return Component.translatableWithFallback(
                presentLangKey(keys).orElse(keys.get(keys.size() - 1)),
                StyleText.prettify(AbilityConfig.key(ability)));
    }

    private static Optional<Component> abilityDescription(String ability) {
        if (ability == null || ability.isBlank()) {
            return Optional.empty();
        }
        ScriptedAbility scripted = ScriptedAbilities.get(ability);
        if (scripted != null && scripted.description().isPresent()) {
            return Optional.of(TooltipOverride.text(scripted.description().get()));
        }
        StyleAffix affix = affixAbility(ability);
        List<String> keys = affix == null
                ? abilityLangKeys(ability, ABILITY_DESC_SUFFIX)
                : List.of(affix.descriptionKey());
        return presentLangKey(keys).map(Component::translatable);
    }

    private static void appendAbilityDescriptions(List<String> abilities, List<Component> tooltip) {
        List<Component> described = new ArrayList<>();
        for (String ability : abilities) {
            abilityDescription(ability).ifPresent(text -> described.add(TooltipGem.detail(
                    Component.empty().append(abilityName(ability).copy().withStyle(BODY))
                            .append(Component.literal(DESCRIPTION_JOIN).withStyle(FRAME))
                            .append(text.copy().withStyle(FRAME)))));
        }
        if (described.isEmpty()) {
            return;
        }
        if (!ItemTooltip.detailed()) {
            int last = tooltip.size() - 1;
            if (last >= 0) {
                tooltip.set(last, ItemTooltip.marked(tooltip.get(last)));
            }
            return;
        }
        tooltip.addAll(described);
    }

    private static MutableComponent abilityNames(List<String> abilities) {
        MutableComponent line = Component.empty();
        for (String ability : abilities) {
            if (!line.getSiblings().isEmpty()) {
                line.append(Component.literal(" · "));
            }
            line.append(abilityName(ability));
        }
        return line;
    }

    private static AffixTuning tuningFor(EquipmentDefinition definition, String affixId) {
        AffixTuning merged = null;
        for (TriggeredEffect entry : definition.triggered()) {
            AffixTuning tuning = entry.effect().tuneAffixes().get(affixId);
            if (tuning == null) {
                continue;
            }
            merged = merged == null ? tuning : tuning.mergedUnder(merged);
        }
        return merged;
    }

    private static boolean rewrites(AffixTuning tuning) {
        return tuning != null && (tuning.onHit().isPresent() || tuning.onSwing().isPresent()
                || tuning.appliesTo().isPresent() || tuning.cooldown().isPresent()
                || tuning.chance().isPresent() || tuning.damageMultiplier().isPresent()
                || tuning.costModifier().isPresent());
    }

    public record EditableLine(String key, Component line) {
    }

    public static List<EditableLine> editableLines(EquipmentDefinition definition, String name,
                                                   TriggeredEffect entry) {
        EffectLines lines = new EffectLines();
        generateEffect(definition, entry, !definition.isPermanent(name, entry), lines);
        List<String> keys = lines.keys();
        List<EditableLine> editable = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            editable.add(new EditableLine(keys.get(index), lines.get(index)));
        }
        return editable;
    }

    public static List<Component> shownLines(EquipmentDefinition definition, String name,
                                             TriggeredEffect entry) {
        if (entry.hideTooltip()) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        appendEffect(definition, entry, !definition.isPermanent(name, entry), lines,
                entry.trigger().chance().orElse(EffectGroup.ALWAYS));
        return lines;
    }

    public static Component rewritten(String raw) {
        return rewritten(raw, null, List.of(), null);
    }

    public static Component rewritten(String raw, Component original, List<Component> generated) {
        return rewritten(raw, original, generated, null);
    }

    public static Component rewritten(String raw, Component original, List<Component> generated,
                                      String icon) {
        MutableComponent body = TooltipOverride.filled(raw, original, generated);
        if (icon != null) {
            Component styled = original == null
                    ? body.withStyle(BODY)
                    : Component.empty().withStyle(TooltipOverride.bodyOf(original).getStyle()).append(body);
            return TooltipGem.iconLine(icon, styled);
        }
        Component kept = original == null ? null : TooltipOverride.keepingStructure(original, body);
        return kept != null ? kept : TooltipGem.CUSTOM.bullet(body.withStyle(BODY));
    }
    private static boolean appendEffect(EquipmentDefinition definition, TriggeredEffect entry,
                                        boolean conditional,
                                        List<Component> tooltip, double odds) {
        EffectLines lines = new EffectLines();
        boolean headed = generateEffect(definition, entry, conditional, lines);
        TooltipOverride override = entry.tooltip();
        int headerAt = lines.keys().indexOf(TooltipOverride.HEADER_KEY);
        int oddsAt = headerAt < 0 ? 0 : shownIndex(lines, override, headerAt);
        List<Component> shown = override.apply(lines, EquipmentTooltip::rewritten);
        int start = tooltip.size();
        tooltip.addAll(shown);
        markOdds(tooltip, start + oddsAt, odds);
        return headed && !(headerAt >= 0 && override.hides(TooltipOverride.HEADER_KEY));
    }

    private static int shownIndex(EffectLines lines, TooltipOverride override, int generated) {
        List<String> keys = lines.keys();
        int shown = 0;
        for (int index = 0; index < generated; index++) {
            if (!override.hides(keys.get(index))) {
                shown++;
            }
        }
        return shown;
    }

    private static boolean generateEffect(EquipmentDefinition definition, TriggeredEffect entry,
                                          boolean conditional, EffectLines tooltip) {
        EquipmentEffect effect = entry.effect();
        Component when = entry.tooltipHeader().isBlank()
                ? triggerOf(entry.trigger())
                : TooltipOverride.text(entry.tooltipHeader());
        List<Component> tuned = tunedAffixes(effect);
        tooltip.section(SECTION_WORN);
        appendWornAttributes(effect, entry.cooldownTicks(), !conditional,
                conditional ? List.of() : tuned, tooltip);
        boolean headed = !tooltip.isEmpty() || when != null;
        if (when != null) {
            tooltip.section(SECTION_HEADER);
            tooltip.add(headerLine(entry.trigger(), when));
        }
        if (conditional) {
            tooltip.section(SECTION_ATTRIBUTES);
            appendConditionalAttributes(effect, entry.cooldownTicks(), tooltip);
            tooltip.section(SECTION_TUNED);
            tooltip.addAll(tuned);
        }
        tooltip.section(SECTION_SUBSTITUTE);
        appendSubstitute(effect, tooltip);
        tooltip.section(SECTION_SUBSTITUTION);
        appendSubstitution(effect, tooltip);
        tooltip.section(SECTION_GRANTS);
        appendGrants(definition, effect, tooltip);
        tooltip.section(SECTION_SPELLS);
        appendSpellLocks(effect.spells(), tooltip);
        tooltip.section(SECTION_DAMAGE_BONUS);
        for (DamageBonus bonus : effect.damageBonus()) {
            tooltip.add(TooltipGem.DAMAGE.bullet(bonus.describe().withStyle(BODY)));
        }
        tooltip.section(SECTION_SELF);
        appendSelfEffects(entry.selfEffects(), tooltip);
        tooltip.section(SECTION_OWN);
        appendOwnEffects(definition, effect, tooltip);
        tooltip.section(SECTION_EVENTS);
        appendEventEffects(definition, effect, entry.trigger(), tooltip);
        tooltip.section(SECTION_CUSTOM);
        for (Component line : Customs.tooltip(CustomScopes.EQUIPMENT_EFFECT, effect)) {
            tooltip.add(TooltipGem.EFFECT.bullet(line.copy().withStyle(BODY)));
        }
        if (TooltipContextAccess.expanded()) {
            tooltip.section(SECTION_AUGMENT);
            appendAugment(effect.augment().orElse(null), tooltip);
            tooltip.section(SECTION_ECHO);
            appendEcho(echoOf(effect), tooltip);
            tooltip.section(SECTION_REPEAT);
            appendRepeat(effect, tooltip);
            tooltip.section(SECTION_DIVE);
            appendSummonDive(effect.summonDive().orElse(null), tooltip);
        }
        tooltip.section(SECTION_GATE);
        appendGate(entry.cooldown(), tooltip);
        tooltip.section(SECTION_STACKING);
        entry.stacking().ifPresent(stacking -> {
            Player viewer = TooltipContextAccess.viewer();
            int now = viewer == null ? stacking.minStacks() : stacking.count(viewer);
            tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_STACKING, now,
                    StyleText.formatCompact(stacking.per()),
                    StyleText.scalingSourceName(stacking.source()), stacking.maxStacks())
                    .withStyle(FRAME)));
        });
        tooltip.section(SECTION_DELAY);
        if (entry.delayTicks() > 0) {
            tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_DELAY,
                    seconds(entry.delayTicks())).withStyle(FRAME)));
        }
        return headed;
    }

    private static void markOdds(List<Component> tooltip, int index, double odds) {
        if (index >= 0 && index < tooltip.size()) {
            tooltip.set(index, withOdds(tooltip.get(index), odds));
        }
    }

    private static void appendSpellLocks(SpellLocks locks, List<Component> tooltip) {
        if (!SpellEngineCompat.isLoaded()) {
            return;
        }
        for (String spell : locks.grant()) {
            tooltip.add(TooltipGem.SPELL.bullet(Component.translatable(KEY_SPELL_GRANT, StyleText.spellName(spell))
                    .withStyle(BODY)));
        }
        locks.replace().forEach((replaced, granted) -> tooltip.add(TooltipGem.SPELL.bullet(
                Component.translatable(KEY_SPELL_REPLACE, StyleText.spellName(replaced), StyleText.spellName(granted))
                        .withStyle(BODY))));
        for (SpellTune tuned : locks.tune()) {
            appendUnder(TooltipGem.SPELL.bullet(Component.translatable(KEY_SPELL_TUNE,
                    StyleText.spellName(tuned.spell())).withStyle(BODY)),
                    List.of(), spellTuneDetails(tuned.tuning()), tooltip);
        }
        boolean removes = locks.disableMode() == SpellLocks.Mode.REMOVE;
        for (String spell : locks.disable()) {
            tooltip.add(TooltipGem.SPELL.bullet(Component.translatable(removes ? KEY_SPELL_REMOVE : KEY_SPELL_BLOCK,
                    StyleText.spellName(spell)).withStyle(BODY)));
        }
        for (String school : locks.disableSchools()) {
            tooltip.add(TooltipGem.SPELL.bullet(Component.translatable(removes ? KEY_SCHOOL_REMOVE : KEY_SCHOOL_BLOCK,
                    StyleText.prettify(SpellLockState.schoolKey(school))).withStyle(BODY)));
        }
    }

    private static List<Component> spellTuneDetails(SpellTuning tuning) {
        List<Component> lines = new ArrayList<>();
        if (tuning.powerMultiplier() != SpellTuning.NO_MULTIPLIER) {
            lines.add(tuneLine(KEY_TUNE_POWER, StyleText.formatMultiplier(tuning.powerMultiplier()),
                    tuning.powerMultiplier() >= SpellTuning.NO_MULTIPLIER));
        }
        addTune(lines, KEY_TUNE_CRIT_CHANCE, tuning.criticalChanceBonus() * PERCENT);
        addTune(lines, KEY_TUNE_CRIT_DAMAGE, tuning.criticalDamageBonus() * PERCENT);
        addTune(lines, KEY_TUNE_RICOCHET, tuning.ricochet());
        addTune(lines, KEY_TUNE_RICOCHET_RANGE, tuning.ricochetRange());
        addTune(lines, KEY_TUNE_PIERCE, tuning.pierce());
        addTune(lines, KEY_TUNE_BOUNCE, tuning.bounce());
        addTune(lines, KEY_TUNE_RANGE, tuning.rangeAdd());
        addTune(lines, KEY_TUNE_CHANNEL, tuning.channelTicksAdd() / (double) SECOND);
        addTune(lines, KEY_TUNE_EFFECT_DURATION, tuning.effectDurationAdd());
        addTune(lines, KEY_TUNE_EFFECT_LEVEL, tuning.effectAmplifierAdd());
        addTune(lines, KEY_TUNE_KNOCKBACK, tuning.knockbackMultiply());
        if (tuning.cooldownReduction() != 0.0F) {
            lines.add(tuneLine(KEY_TUNE_COOLDOWN, StyleText.formatDecimal(tuning.cooldownReduction()),
                    tuning.cooldownReduction() > 0.0F));
        }
        addTune(lines, KEY_TUNE_TRIGGER_CHANCE, tuning.triggerChanceAdd() * PERCENT);
        if (!tuning.chainReaction().isEmpty()) {
            lines.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_TUNE_CHAIN).withStyle(GOOD)));
        }
        if (!tuning.launch().isEmpty()) {
            lines.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_TUNE_LAUNCH).withStyle(BODY)));
        }
        if (tuning.filtersImpacts()) {
            List<String> filters = new ArrayList<>(tuning.impactSchools());
            filters.addAll(tuning.impactTypes());
            lines.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_TUNE_IMPACTS,
                    prettyNamesOf(filters)).withStyle(FRAME)));
        }
        return lines;
    }

    private static void addTune(List<Component> lines, String key, double amount) {
        if (amount == 0.0D) {
            return;
        }
        String shown = (amount > 0.0D ? PLUS : "") + StyleText.formatDecimal(amount);
        lines.add(tuneLine(key, shown, amount > 0.0D));
    }

    private static Component tuneLine(String key, String amount, boolean better) {
        return TooltipGem.NOTE.bullet(Component.translatable(key,
                Component.literal(amount).withStyle(better ? GOOD : BAD)).withStyle(BODY));
    }

    private static void appendSelfEffects(List<AffixEffects.SelfEffect> effects,
                                          List<Component> tooltip) {
        for (AffixEffects.SelfEffect effect : effects) {
            tooltip.add(TooltipGem.EFFECT.bullet(Component.translatable(KEY_SELF_EFFECT,
                            StyleText.registryName(effect.effect()),
                            effect.amplifier() + 1,
                            effect.duration() / SECOND)
                    .withStyle(BODY)));
        }
    }

    private static void appendGate(EffectCooldown gate, List<Component> tooltip) {
        if (gate == null || gate.isEmpty()) {
            return;
        }
        if (gate.holdWhileActive() && !gate.windowed()) {
            tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_HOLD_WHILE_ACTIVE,
                    seconds(gate.ticks())).withStyle(FRAME)));
            return;
        }
        if (!gate.windowed()) {
            tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_COOLDOWN,
                            StyleText.formatDecimal(gate.ticks() / (double) SECOND))
                    .withStyle(FRAME)));
            return;
        }
        String span = StyleText.formatDecimal(gate.span() / (double) SECOND);
        Component summary = gate.limited()
                ? Component.translatable(gate.uses() == 1 ? KEY_WINDOW : KEY_WINDOW_USES,
                        gate.uses(), span)
                : Component.translatable(KEY_WINDOW_SPAN, span);
        if (!ItemTooltip.detailed()) {
            tooltip.add(TooltipGem.NOTE.bullet(ItemTooltip.marked(summary.copy().withStyle(FRAME))));
            return;
        }
        tooltip.add(TooltipGem.NOTE.bullet((gate.ticks() > 0
                        ? Component.translatable(KEY_WINDOW_FULL, summary,
                                StyleText.formatDecimal(gate.ticks() / (double) SECOND))
                        : summary.copy())
                .withStyle(FRAME)));
        if (gate.refreshOnUse()) {
            tooltip.add(TooltipGem.hang(TooltipGem.NOTE.bullet(
                    Component.translatable(KEY_WINDOW_REFRESH).withStyle(FRAME))));
        }
    }

    private static void appendGrantedStyle(StylePage page, String styleId,
                                           EquipmentEffect effect, List<Component> tooltip) {
        if (page == null) {
            return;
        }
        StylePage shown = effect.augment()
                .map(augment -> AttackOverrides.scaleWith(page, augment))
                .orElse(page);

        List<StyleAffix> affixes = new ArrayList<>();
        for (String affixId : styleAffixesOf(styleId)) {
            StyleAffix affix = AffixRegistry.get(affixId);
            if (affix != null && affix.canApplyTo(shown)) {
                affixes.add(affix);
            }
        }
        if (!affixes.isEmpty()) {
            StyleTooltip.appendAffixes(affixes, tooltip, false);
        }
    }

    private static List<String> styleAffixesOf(String styleId) {
        CustomStyle custom = CustomStyles.get(styleId);
        return custom == null ? List.of() : custom.entry().affixIds();
    }

    private static StylePage styleNamed(String styleId) {
        StylePage direct = StyleRegistry.get(styleId);
        if (direct != null) {
            return direct;
        }
        String resolved = CustomStyles.styleIdOf(styleId);
        return resolved == null || resolved.isBlank() ? null : StyleRegistry.get(resolved);
    }

    private static Component castLine(CastGate gate) {
        Component names = spellNamesOf(gate.spells());
        return switch (gate.phase()) {
            case CAST -> detailed(KEY_CAST_SPELL, KEY_CAST_NAMED, names);
            case CHANNEL -> detailed(KEY_CHANNEL_SPELL, KEY_CHANNEL_NAMED, names);
            default -> detailed(KEY_CASTING_SPELL, KEY_CASTING_NAMED, names);
        };
    }

    private static Component spellNamesOf(List<String> spells) {
        if (spells.isEmpty()) {
            return null;
        }
        MutableComponent joined = Component.empty();
        for (int index = 0; index < spells.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", "));
            }
            joined.append(StyleText.spellName(spells.get(index)));
        }
        return joined;
    }

    private static Component useLine(UseGate gate) {
        return switch (gate.phase()) {
            case START -> Component.translatable(KEY_ON_USE_START);
            case CHANNEL -> Component.translatable(KEY_ON_USE_CHANNEL);
            case RELEASE -> Component.translatable(KEY_ON_USE_RELEASE);
            default -> Component.translatable(KEY_ON_USE);
        };
    }

    private static void stateParts(StateGate state, List<Component> parts) {
        if (state == null || state.isEmpty()) {
            return;
        }
        state.sprinting().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_SPRINTING : KEY_NOT_SPRINTING)));
        state.airborne().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_AIRBORNE : KEY_GROUNDED)));
        state.crouching().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_CROUCHING : KEY_STANDING)));
        state.behindTarget().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_BEHIND : KEY_NOT_BEHIND)));
        state.night().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_NIGHT : KEY_DAY)));
        state.damagedWithin().ifPresent(ticks ->
                parts.add(Component.translatable(KEY_DAMAGED_WITHIN, seconds(ticks))));
        state.hasSpell().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_HAS_SPELL : KEY_NO_SPELL)));
        if (!state.heldSpellSchools().isEmpty()) {
            MutableComponent schools = Component.empty();
            for (String school : state.heldSpellSchools()) {
                if (!schools.getSiblings().isEmpty()) {
                    schools.append(Component.translatable(KEY_SCHOOL_JOIN));
                }
                schools.append(StyleText.schoolName(school));
            }
            parts.add(Component.translatable(KEY_HAS_SPELL_SCHOOL, schools));
        }
        state.castingSpell().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_CASTING_SPELL : KEY_NOT_CASTING)));
        state.summonsActive().ifPresent(count ->
                parts.add(Component.translatable(KEY_SUMMONS_ACTIVE, count)));
        parts.addAll(ItemGateText.parts(state));
        parts.addAll(ThresholdText.parts(state));
    }

    private static void dealtParts(DealtGate dealt, List<Component> parts) {
        if (dealt == null || dealt.isEmpty()) {
            return;
        }
        dealt.above().ifPresent(value ->
                parts.add(Component.translatable(KEY_DAMAGE_ABOVE,
                        StyleText.formatDecimal(value))));
        dealt.below().ifPresent(value ->
                parts.add(Component.translatable(KEY_DAMAGE_BELOW,
                        StyleText.formatDecimal(value))));
        if (!dealt.damageTypes().isEmpty()) {
            parts.add(Component.translatable(KEY_DAMAGE_DEALT_TYPE,
                    namesOf(dealt.damageTypes())));
        }
        if (!dealt.targets().isEmpty()) {
            parts.add(Component.translatable(KEY_AGAINST, entityNamesOf(dealt.targets())));
        }
        if (!dealt.targetEffects().isEmpty()) {
            parts.add(Component.translatable(KEY_AGAINST_AFFLICTED,
                    entityNamesOf(dealt.targetEffects())));
        }
    }

    private static void summonParts(SummonGate summon, List<Component> parts) {
        if (summon == null || summon.isEmpty()) {
            return;
        }
        Component named = entityNamesOf(summon.entities());
        if (summon.onSpawn()) {
            parts.add(detailed(KEY_ON_SUMMON_SPAWN, KEY_ON_SUMMON_SPAWN_TYPED, named));
        }
        if (summon.onDeath()) {
            parts.add(detailed(KEY_ON_SUMMON_DEATH, KEY_ON_SUMMON_DEATH_TYPED, named));
        }
        if (summon.onKill()) {
            parts.add(detailed(KEY_ON_SUMMON_KILL, KEY_ON_SUMMON_KILL_TYPED, named));
        }
    }

    private static void hurtParts(HurtGate hurt, List<Component> parts) {
        hurt.above().ifPresent(value -> parts.add(Component.translatable(KEY_HURT_ABOVE,
                StyleText.formatDecimal(value))));
        hurt.below().ifPresent(value -> parts.add(Component.translatable(KEY_HURT_BELOW,
                StyleText.formatDecimal(value))));
        hurt.totalAbove().ifPresent(value -> parts.add(Component.translatable(KEY_HURT_TOTAL,
                StyleText.formatDecimal(value), seconds(EquipmentHurt.TOTAL_WINDOW_TICKS))));
        if (!hurt.attackers().isEmpty()) {
            parts.add(Component.translatable(KEY_HURT_BY, entityNamesOf(hurt.attackers())));
        }
    }

    private static void projectileParts(ProjectileHitGate hit, List<Component> parts) {
        if (!hit.fromSummon()) {
            parts.add(Component.translatable(KEY_NOT_FROM_SUMMON));
        }
        hit.distanceAbove().ifPresent(blocks -> parts.add(Component.translatable(
                KEY_PROJECTILE_FAR, StyleText.formatDecimal(blocks))));
        hit.distanceBelow().ifPresent(blocks -> parts.add(Component.translatable(
                KEY_PROJECTILE_NEAR, StyleText.formatDecimal(blocks))));
        dealtParts(hit.dealt(), parts);
        SpellProjectileGate spell = hit.spellProjectile();
        if (spell != null && !spell.isEmpty()) {
            parts.add(detailed(KEY_SPELL_PROJECTILE, KEY_SPELL_PROJECTILE_NAMED,
                    spellNamesOf(spell.spells())));
            if (!spell.schools().isEmpty()) {
                parts.add(Component.translatable(KEY_CAST_SCHOOL, namesOf(spell.schools())));
            }
        }
    }

    private static Component prettyNamesOf(List<String> ids) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", "));
            }
            joined.append(Component.literal(StyleText.prettify(ids.get(index))));
        }
        return joined;
    }

    private static String seconds(int ticks) {
        return StyleText.formatDecimal(ticks / (double) SECOND);
    }

    private static Component detailed(String plainKey, String typedKey, Component names) {
        return names == null
                ? Component.translatable(plainKey)
                : Component.translatable(typedKey, names);
    }

    private static Component namesOf(List<String> ids) {
        return joinedNames(ids, true);
    }

    private static Component entityNamesOf(List<String> ids) {
        return joinedNames(ids, false);
    }

    private static Component joinedNames(List<String> ids, boolean schools) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        MutableComponent joined = Component.empty();
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", "));
            }
            String id = ids.get(index);
            joined.append(schools ? StyleText.schoolName(id) : StyleText.registryName(id));
        }
        return joined;
    }

    static Component triggerOf(EquipmentTrigger trigger) {
        List<Component> parts = new ArrayList<>();
        collectConditions(trigger, parts);
        for (EquipmentTrigger required : trigger.allOf()) {
            List<Component> inner = new ArrayList<>();
            collectConditions(required, inner);
            Component joined = triggerOfInner(required, inner);
            if (joined != null) {
                parts.add(inner.size() > 1 || !required.anyOf().isEmpty()
                        ? Component.translatable(KEY_COND_GROUP, joined)
                        : joined);
            }
        }
        if (!trigger.anyOf().isEmpty()) {
            MutableComponent options = null;
            for (EquipmentTrigger option : trigger.anyOf()) {
                List<Component> inner = new ArrayList<>();
                collectConditions(option, inner);
                Component joined = triggerOfInner(option, inner);
                if (joined == null) {
                    continue;
                }
                options = options == null
                        ? joined.copy()
                        : Component.translatable(KEY_COND_OR, options, joined);
            }
            if (options != null) {
                parts.add(parts.isEmpty()
                        ? options
                        : Component.translatable(KEY_COND_GROUP, options));
            }
        }
        Component all = joinAll(parts, KEY_COND_AND);
        return all == null
                ? null
                : Component.translatable(KEY_WHEN_PREFIX,
                        StyleText.sentenceCase(all.getString()));
    }

    private static Component triggerOfInner(EquipmentTrigger trigger, List<Component> parts) {
        Component nested = triggerBody(trigger);
        List<Component> all = new ArrayList<>(parts);
        if (nested != null) {
            all.add(nested);
        }
        return joinAll(all, KEY_COND_AND);
    }

    private static Component triggerBody(EquipmentTrigger trigger) {
        if (trigger.anyOf().isEmpty() && trigger.allOf().isEmpty()) {
            return null;
        }
        List<Component> parts = new ArrayList<>();
        for (EquipmentTrigger required : trigger.allOf()) {
            List<Component> inner = new ArrayList<>();
            collectConditions(required, inner);
            Component joined = triggerOfInner(required, inner);
            if (joined != null) {
                parts.add(joined);
            }
        }
        MutableComponent options = null;
        for (EquipmentTrigger option : trigger.anyOf()) {
            List<Component> inner = new ArrayList<>();
            collectConditions(option, inner);
            Component joined = triggerOfInner(option, inner);
            if (joined == null) {
                continue;
            }
            options = options == null
                    ? joined.copy()
                    : Component.translatable(KEY_COND_OR, options, joined);
        }
        if (options != null) {
            parts.add(Component.translatable(KEY_COND_GROUP, options));
        }
        return joinAll(parts, KEY_COND_AND);
    }

    private static Component joinAll(List<Component> parts, String joinKey) {
        MutableComponent joined = null;
        for (Component part : parts) {
            joined = joined == null
                    ? part.copy()
                    : Component.translatable(joinKey, joined, part);
        }
        return joined;
    }

    private static void collectConditions(EquipmentTrigger trigger, List<Component> parts) {
        if (trigger.onMiss().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_MISS));
        }
        if (trigger.onKill().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_KILL));
        }
        if (trigger.onSummonHit().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_SUMMON_HIT));
        }
        summonParts(trigger.activation().summon(), parts);
        if (trigger.reactsToHurt()) {
            parts.add(detailed(KEY_ON_HURT, KEY_ON_HURT_TYPED,
                    namesOf(trigger.hurt().damageTypes())));
            hurtParts(trigger.hurt(), parts);
        }
        if (trigger.activation().reactsToShoot()) {
            parts.add(detailed(KEY_ON_SHOOT, KEY_ON_SHOOT_TYPED,
                    entityNamesOf(trigger.activation().shoot().entities())));
            if (!trigger.activation().shoot().fromSummon()) {
                parts.add(Component.translatable(KEY_NOT_FROM_SUMMON));
            }
        }
        if (trigger.activation().reactsToBlock()) {
            parts.add(detailed(KEY_ON_BLOCK, KEY_ON_BLOCK_TYPED,
                    namesOf(trigger.activation().block().damageTypes())));
            trigger.activation().block().fullyBlocked().ifPresent(full ->
                    parts.add(Component.translatable(full ? KEY_FULLY_BLOCKED : KEY_PARTLY_BLOCKED)));
        }
        if (trigger.activation().usesItem()) {
            UseGate use = trigger.activation().onUse();
            parts.add(useLine(use));
            use.heldAbove().ifPresent(ticks ->
                    parts.add(Component.translatable(KEY_USE_HELD_ABOVE, seconds(ticks))));
            use.heldBelow().ifPresent(ticks ->
                    parts.add(Component.translatable(KEY_USE_HELD_BELOW, seconds(ticks))));
        }
        if (!trigger.activation().cast().isEmpty()) {
            CastGate cast = trigger.activation().cast();
            parts.add(castLine(cast));
            if (!cast.schools().isEmpty()) {
                parts.add(Component.translatable(KEY_CAST_SCHOOL, namesOf(cast.schools())));
            }
            if (!cast.castTypes().isEmpty()) {
                parts.add(Component.translatable(KEY_CAST_TYPE, prettyNamesOf(cast.castTypes())));
            }
            if (cast.includeTriggered()) {
                parts.add(Component.translatable(KEY_CAST_TRIGGERED));
            }
        }
        if (trigger.activation().reactsToProjectileHit()) {
            ProjectileHitGate hit = trigger.activation().projectileHit();
            parts.add(detailed(KEY_ON_PROJECTILE_HIT, KEY_ON_PROJECTILE_HIT_TYPED,
                    entityNamesOf(hit.entities())));
            projectileParts(hit, parts);
        }
        if (trigger.phase().onPlayerHit().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_PLAYER_HIT));
        }
        if (trigger.phase().onSwing().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_SWING));
        }
        stateParts(trigger.phase().state(), parts);
        dealtParts(trigger.activation().dealt(), parts);
        trigger.lastSlot().ifPresent(last ->
                parts.add(Component.translatable(last ? KEY_LAST_SLOT : KEY_NOT_LAST_SLOT)));
        trigger.slotBand().ifPresent(band ->
                parts.add(Component.translatable(band.translationKey())));
        trigger.slots().ifPresent(selection -> parts.add(slotsPart(selection)));
        trigger.slotIndex().ifPresent(index ->
                parts.add(Component.translatable(KEY_SLOTS, slotName(index))));
        trigger.everyNthSwing().ifPresent(every ->
                parts.add(Component.translatable(KEY_NTH, every)));
        trigger.idleTicks().ifPresent(ticks ->
                parts.add(Component.translatable(KEY_IDLE,
                        StyleText.formatDecimal(ticks / (double) SECOND))));
        HealthGate health = trigger.health();
        health.selfAbove().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_SELF_ABOVE,
                        StyleText.formatCompact(ratio * 100.0D))));
        health.selfBelow().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_SELF_BELOW,
                        StyleText.formatCompact(ratio * 100.0D))));
        health.targetAbove().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_TARGET_ABOVE,
                        StyleText.formatCompact(ratio * 100.0D))));
        health.targetBelow().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_TARGET_BELOW,
                        StyleText.formatCompact(ratio * 100.0D))));
        if (trigger.requiresAffix().orElse(false)) {
            parts.add(Component.translatable(KEY_NEEDS_AFFIX));
        }
        trigger.hitbox().ifPresent(shape ->
                parts.add(Component.translatable(KEY_HITBOX_TRIGGER, hitboxName(shape))));
        trigger.styleId().ifPresent(id -> {
            StylePage page = styleNamed(id);
            parts.add(Component.translatable(KEY_STYLE_TRIGGER, page == null
                    ? Component.literal(StyleText.prettify(id))
                    : StyleText.name(page)));
        });
        trigger.affixId().ifPresent(id -> {
            StyleAffix affix = AffixRegistry.get(id);
            parts.add(Component.translatable(KEY_AFFIX_TRIGGER, affix == null
                    ? Component.literal(StyleText.prettify(id))
                    : Component.translatableWithFallback(affix.nameKey(), affix.fallbackName())));
        });
    }

    private static Component headerLine(EquipmentTrigger trigger, Component when) {
        Set<TooltipIcon> icons = new LinkedHashSet<>();
        collectIcons(trigger, icons);
        List<TooltipIcon> shown = new ArrayList<>(icons);
        return TooltipIcon.leading(shown.subList(0, Math.min(MAX_HEADER_ICONS, shown.size())),
                when.copy().withStyle(HEADER));
    }

    private static void collectIcons(EquipmentTrigger trigger, Set<TooltipIcon> icons) {
        PhaseGate phase = trigger.phase();
        ActivationGate activation = trigger.activation();
        if (phase.onMiss().orElse(false)) {
            icons.add(TooltipIcon.MISS);
        }
        if (phase.onKill().orElse(false)) {
            icons.add(TooltipIcon.KILL);
        }
        if (phase.onSummonHit().orElse(false) || !activation.summon().isEmpty()) {
            icons.add(TooltipIcon.SUMMON_HIT);
        }
        if (trigger.reactsToHurt()) {
            icons.add(TooltipIcon.HURT);
        }
        if (activation.reactsToShoot()) {
            icons.add(TooltipIcon.SHOOT);
        }
        if (activation.reactsToBlock()) {
            icons.add(TooltipIcon.BLOCK);
        }
        if (activation.usesItem()) {
            icons.add(TooltipIcon.USE);
        }
        if (activation.reactsToProjectileHit()) {
            icons.add(TooltipIcon.PROJECTILE_HIT);
        }
        if (!activation.cast().isEmpty()) {
            icons.add(TooltipIcon.CASTING);
        }
        if (phase.onPlayerHit().orElse(false)) {
            icons.add(TooltipIcon.HIT);
        }
        if (phase.onSwing().orElse(false)) {
            icons.add(TooltipIcon.SWING);
        }
        StateGate state = phase.state();
        if (state != null) {
            if (state.sprinting().orElse(false)) {
                icons.add(TooltipIcon.SPRINTING);
            }
            if (state.airborne().orElse(false)) {
                icons.add(TooltipIcon.AIRBORNE);
            }
            if (state.crouching().orElse(false)) {
                icons.add(TooltipIcon.CROUCHING);
            }
            if (state.behindTarget().orElse(false)) {
                icons.add(TooltipIcon.BEHIND);
            }
            state.night().ifPresent(night ->
                    icons.add(night ? TooltipIcon.NIGHT : TooltipIcon.DAY));
            if (state.damagedWithin().isPresent()) {
                icons.add(TooltipIcon.DAMAGED);
            }
            if (state.hasSpell().orElse(false) || !state.heldSpellSchools().isEmpty()) {
                icons.add(TooltipIcon.HAS_SPELL);
            }
            if (state.castingSpell().orElse(false)) {
                icons.add(TooltipIcon.CASTING);
            }
        }
        if (trigger.idleTicks().isPresent()) {
            icons.add(TooltipIcon.IDLE);
        }
        if (!trigger.health().isEmpty()) {
            icons.add(TooltipIcon.HEALTH);
        }
        for (EquipmentTrigger required : trigger.allOf()) {
            collectIcons(required, icons);
        }
        for (EquipmentTrigger option : trigger.anyOf()) {
            collectIcons(option, icons);
        }
    }

    private static Component slotsPart(SlotSelection selection) {
        if (selection.range().isPresent()) {
            SlotSelection.Range range = selection.range().get();
            if (range.from() == SlotSelection.FIRST && range.to() == SlotSelection.LAST) {
                return range.except().isEmpty()
                        ? Component.translatable(KEY_SLOTS_EVERY)
                        : Component.translatable(KEY_SLOTS_EVERY_EXCEPT, slotList(range.except()));
            }
        }
        return Component.translatable(KEY_SLOTS, slotsName(selection));
    }

    private static Component slotsName(SlotSelection selection) {
        if (selection.range().isPresent()) {
            SlotSelection.Range range = selection.range().get();
            Component span = Component.translatable(KEY_SLOT_RANGE,
                    slotName(range.from()), slotName(range.to()));
            return range.except().isEmpty()
                    ? span
                    : Component.translatable(KEY_SLOT_EXCEPT, span, slotList(range.except()));
        }
        return slotList(selection.indices());
    }

    private static Component slotList(List<Integer> indices) {
        if (indices.isEmpty()) {
            return Component.empty();
        }
        MutableComponent joined = slotName(indices.getFirst()).copy();
        for (int i = 1; i < indices.size(); i++) {
            joined = Component.translatable(KEY_SLOT_AND, joined, slotName(indices.get(i)));
        }
        return joined;
    }

    private static Component slotName(int index) {
        if (index == SlotSelection.LAST) {
            return Component.translatable(KEY_SLOT_LAST);
        }
        if (index < SlotSelection.LAST) {
            return Component.translatable(KEY_SLOT_FROM_LAST, ordinalName(-index));
        }
        return ordinalName(index + 1);
    }

    private static Component ordinalName(int ordinal) {
        return switch (ordinal) {
            case 1 -> Component.translatable(KEY_ORDINAL_1);
            case 2 -> Component.translatable(KEY_ORDINAL_2);
            case 3 -> Component.translatable(KEY_ORDINAL_3);
            default -> Component.translatable(KEY_ORDINAL_N, ordinal);
        };
    }

    private static Component hitboxName(String raw) {
        try {
            return StyleText.hitboxName(
                    WeaponAttributes.HitBoxShape.valueOf(raw.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Component.literal(StyleText.prettify(raw.toLowerCase(Locale.ROOT)));
        }
    }

    private static void appendOwnEffects(EquipmentDefinition definition, EquipmentEffect effect,
                                         EffectLines tooltip) {
        if (effect.onHit().isEmpty() && effect.onSwing().isEmpty()) {
            return;
        }
        EffectLines lines = new EffectLines();
        AffixText.appendEffects(EquipmentAffix.of(new EquipmentRuntime.Match(
                definition, new TriggeredEffect(EquipmentTrigger.ALWAYS, effect, EffectCooldown.NONE, false),
                definition.id())), lines, false);
        tooltip.addNested(lines.tagged(), TooltipGem.EFFECT::place);
    }

    private static void appendEventEffects(EquipmentDefinition definition, EquipmentEffect effect,
                                           EquipmentTrigger trigger, EffectLines tooltip) {
        for (EventKind kind : EventKind.values()) {
            List<AffixEffects.OnHit> payloads = effect.events().get(kind);
            for (int index = 0; index < payloads.size(); index++) {
                tooltip.section(SECTION_EVENTS + SECTION_JOIN + kind.serializedName()
                        + SECTION_JOIN + index);
                if (!kind.heardBy(trigger)) {
                    tooltip.add(TooltipGem.EFFECT.bullet(Component.translatable(kind.labelKey())
                            .withStyle(BODY)));
                }
                EffectLines lines = new EffectLines();
                EquipmentEffect alone = EquipmentEffects.withOnHit(EquipmentEffect.EMPTY,
                        Optional.of(payloads.get(index)));
                AffixText.appendEffects(EquipmentAffix.of(new EquipmentRuntime.Match(definition,
                        new TriggeredEffect(EquipmentTrigger.ALWAYS, alone, EffectCooldown.NONE,
                                false), definition.id())), lines, false);
                tooltip.addNested(lines.tagged(), TooltipGem::hangPlace);
            }
        }
    }

    private static void appendGrants(EquipmentDefinition definition, EquipmentEffect effect,
                                     List<Component> tooltip) {
        if (effect.grantAffixes().isEmpty()) {
            return;
        }
        int budget = TooltipContextAccess.expanded() ? Integer.MAX_VALUE : GRANT_LINE_BUDGET;
        int spent = 0;
        List<Component> withheld = new ArrayList<>();
        for (String entry : effect.grantAffixes()) {
            String id = AffixGrants.affixOf(entry);
            StyleAffix affix = AffixRegistry.get(id);
            if (affix != null && AffixGrants.isRemoval(entry)) {
                tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_GRANT_REMOVED,
                        AffixText.name(affix)).withStyle(BODY)));
                continue;
            }
            if (affix == null) {
                tooltip.add(TooltipGem.NOTE.bullet(
                        Component.translatable(KEY_GRANT_UNKNOWN, id).withStyle(FRAME)));
                continue;
            }
            AffixTuning tuning = tuningFor(definition, id);
            StyleAffix shown = tuning == null ? affix : tuning.applyTo(affix);
            MutableComponent name = TooltipGem.of(shown.rarity()).bullet(
                    AffixText.name(shown).copy().withStyle(shown.rarity().color()));
            List<Component> effects = new ArrayList<>();
            AffixText.appendEffects(shown, effects);
            if (spent > 0 && spent + effects.size() > budget) {
                withheld.add(AffixText.name(shown).copy().withStyle(shown.rarity().color()));
                continue;
            }
            if (!ItemTooltip.detailed()) {
                tooltip.add(effects.isEmpty() ? name : ItemTooltip.marked(name));
                continue;
            }
            tooltip.add(name);
            spent += effects.size();
            List<Component> body = new ArrayList<>();
            if (tuning == null) {
                body.addAll(effects);
            } else {
                appendTunedScalars(affix, tuning, body);
                appendTunedFacts(tuning, body);
                AffixText.appendTuned(affix, shown, body, false);
            }
            for (Component line : body) {
                tooltip.add(TooltipGem.hangPlace(line));
            }
        }
        if (withheld.isEmpty()) {
            return;
        }
        MutableComponent names = Component.empty();
        for (int index = 0; index < withheld.size(); index++) {
            if (index > 0) {
                names.append(Component.literal(", ").withStyle(FRAME));
            }
            names.append(withheld.get(index));
        }
        tooltip.add(ItemTooltip.marked(TooltipGem.MORE.bullet(names)));
    }

    private static void appendAugment(AttackOverride augment, List<Component> tooltip) {
        if (augment == null) {
            return;
        }
        augment.damageMultiplier().ifPresent(value ->
                tooltip.add(TooltipGem.STAT.bullet(stat("tooltip.combat_styles.short_damage", value))));
        augment.rangeMultiplier().ifPresent(value ->
                tooltip.add(TooltipGem.STAT.bullet(stat("tooltip.combat_styles.short_reach", value))));
        augment.movementSpeedMultiplier().ifPresent(value ->
                tooltip.add(TooltipGem.STAT.bullet(stat("tooltip.combat_styles.short_move", value))));
        augment.upswing().ifPresent(value -> tooltip.add(TooltipGem.STAT.bullet(
                Component.translatable("tooltip.combat_styles.short_upswing").withStyle(BODY)
                        .append(Component.literal(" " + StyleText.formatDecimal(value))
                                .withStyle(BODY)))));

        List<Component> timing = new ArrayList<>();
        StyleTooltip.appendTiming(augment, timing);
        for (Component line : timing) {
            tooltip.add(TooltipGem.STAT.bullet(line));
        }

        appendAttributes(augment.attributes(), tooltip);
    }

    private static void appendAttributes(Map<String, AttackAttribute> attributes,
                                         List<Component> tooltip) {
        for (Map.Entry<String, AttackAttribute> entry : attributes.entrySet()) {
            AttackAttribute value = entry.getValue();
            if (value == null || value.amount() == 0.0D) {
                continue;
            }
            tooltip.add(TooltipGem.STAT.bullet(StyleText.attributeLine(entry.getKey(), value, BODY)));
        }
    }

    private static void appendChargedAttributes(Map<String, AttackAttribute> set,
                                                int cooldownTicks, List<Component> tooltip) {
        if (set.isEmpty()) {
            return;
        }
        int last = -1;
        for (Map.Entry<String, AttackAttribute> entry : set.entrySet()) {
            AttackAttribute value = entry.getValue();
            if (value == null || value.amount() == 0.0D) {
                continue;
            }
            tooltip.add(TooltipGem.STAT.bullet(StyleText.attributeLine(entry.getKey(), value, BODY)
                    .append(Component.literal(" ")
                            .append(Component.translatable(KEY_SET_CHARGED)).withStyle(FRAME))));
            last = tooltip.size() - 1;
        }
        if (last < 0) {
            return;
        }
        if (!ItemTooltip.detailed()) {
            tooltip.set(last, ItemTooltip.marked(tooltip.get(last)));
            return;
        }
        tooltip.add(TooltipGem.hang(TooltipGem.NOTE.bullet(Component.translatable(KEY_SET_RECHARGES,
                        StyleText.formatDecimal(cooldownTicks / (double) SECOND))
                .withStyle(FRAME))));
    }

    private static void appendConditionalAttributes(EquipmentEffect effect, int cooldownTicks,
                                                    List<Component> tooltip) {
        Map<String, AttackAttribute> set = new LinkedHashMap<>();
        Map<String, AttackAttribute> plain = new LinkedHashMap<>();
        for (Map.Entry<String, AttributeBonus> entry : effect.attributes().entrySet()) {
            AttributeBonus bonus = entry.getValue();
            if (bonus != null) {
                (bonus.set() ? set : plain).put(entry.getKey(), bonus.asAttackAttribute());
            }
        }
        appendAttributes(plain, tooltip);
        if (cooldownTicks > 0) {
            appendChargedAttributes(set, cooldownTicks, tooltip);
        } else {
            appendAttributes(set, tooltip);
        }
    }

    private static void appendWornAttributes(EquipmentEffect effect, int cooldownTicks,
                                             boolean includeSet, List<Component> rewritten,
                                             List<Component> tooltip) {
        Map<String, AttackAttribute> set = new LinkedHashMap<>();
        for (Map.Entry<String, AttributeBonus> entry : effect.attributes().entrySet()) {
            AttributeBonus bonus = entry.getValue();
            if (includeSet && bonus != null && bonus.set()) {
                set.put(entry.getKey(), bonus.asAttackAttribute());
            }
        }
        if (rewritten.isEmpty() && set.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(KEY_WORN).withStyle(HEADER));
        if (cooldownTicks > 0) {
            appendChargedAttributes(set, cooldownTicks, tooltip);
        } else {
            appendAttributes(set, tooltip);
        }
        tooltip.addAll(rewritten);
    }

    private static List<Component> tunedAffixes(EquipmentEffect effect) {
        List<Component> rewritten = new ArrayList<>();
        for (Map.Entry<String, AffixTuning> tuned : effect.tuneAffixes().entrySet()) {
            StyleAffix affix = AffixRegistry.get(tuned.getKey());
            AffixTuning tuning = tuned.getValue();
            if (affix == null || !rewrites(tuning) || effect.grantAffixes().contains(tuned.getKey())) {
                continue;
            }
            MutableComponent line = TooltipGem.of(affix.rarity()).bullet(
                    AffixText.name(affix).copy().withStyle(affix.rarity().color()));
            AffixPlates.attach(line, Component.translatable(KEY_CHANGED), PLATE_COLOUR);
            List<Component> diff = new ArrayList<>();
            appendTunedScalars(affix, tuning, diff);
            AffixText.appendTuned(affix, tuning.applyTo(affix), diff, true);
            rewritten.add(diff.isEmpty() || ItemTooltip.detailed()
                    ? line
                    : ItemTooltip.marked(line));
            if (!ItemTooltip.detailed()) {
                continue;
            }
            for (Component changed : diff) {
                rewritten.add(TooltipGem.hangPlace(changed));
            }
        }
        return rewritten;
    }

    private static MutableComponent stat(String labelKey, double value) {
        boolean better = value >= 1.0D;
        return Component.translatable(labelKey).withStyle(BODY)
                .append(Component.literal(" " + StyleText.formatMultiplier(value))
                        .withStyle(better ? GOOD : BAD));
    }

    private static AffixEffects.Echo echoOf(EquipmentEffect effect) {
        return effect.onSwing().flatMap(AffixEffects.OnHit::echo)
                .or(() -> effect.onHit().flatMap(AffixEffects.OnHit::echo))
                .or(effect::echo)
                .orElse(null);
    }

    private static void appendEcho(AffixEffects.Echo echo, List<Component> tooltip) {
        if (echo == null || echo.times() <= 0) {
            return;
        }
        tooltip.add(TooltipGem.EFFECT.bullet(Component.translatable(KEY_ECHO, echo.times(),
                        StyleText.formatDecimal(echo.delayTicks() / (double) SECOND),
                        Math.round(echo.ratio() * 100.0F))
                .withStyle(COND)));
    }

    private static void appendRepeat(EquipmentEffect effect, List<Component> tooltip) {
        if (effect.resetCombo().orElse(0) > 0) {
            tooltip.add(TooltipGem.MOVESET.bullet(Component.translatable(KEY_REPEAT).withStyle(GOOD)));
        }
    }

    private static void appendSubstitute(EquipmentEffect effect, List<Component> tooltip) {
        effect.substituteStyle().ifPresent(styleId -> {
            StylePage page = styleNamed(styleId);
            tooltip.add(TooltipGem.MOVESET.bullet(Component.translatable(KEY_REPLACES).withStyle(BODY)
                    .append(Component.literal(" ")
                            .append(page == null
                                    ? Component.literal(styleId)
                                    : StyleText.name(page)))));
        });
    }

    private static void appendSubstitution(EquipmentEffect effect, List<Component> tooltip) {
        effect.substitution().ifPresent(substitution -> {
            List<String> styles = substitution.styles();
            if (styles.isEmpty()) {
                return;
            }
            boolean all = TooltipContextAccess.expanded() || ItemTooltip.detailed();
            int shownStyles = all ? styles.size() : 1;
            MutableComponent named = Component.empty();
            for (int index = 0; index < shownStyles; index++) {
                StylePage each = styleNamed(styles.get(index));
                if (index > 0) {
                    named.append(Component.literal(", ").withStyle(BODY));
                }
                named.append(each == null
                        ? Component.literal(StyleText.prettify(styles.get(index)))
                        : StyleText.name(each));
            }
            int hidden = styles.size() - shownStyles;
            if (hidden > 0) {
                named.append(Component.translatable(KEY_REWRITES_MORE, hidden)
                        .withStyle(FRAME));
            }
            MutableComponent header = TooltipGem.MOVESET.bullet(
                    Component.translatable(KEY_REWRITES, named).withStyle(BODY));
            tooltip.add(hidden > 0 ? ItemTooltip.marked(header) : header);
            if (!all) {
                return;
            }
            List<Component> affixes = new ArrayList<>();
            for (String styleId : styles) {
                appendGrantedStyle(styleNamed(styleId), styleId, effect, affixes);
            }
            for (Component line : affixes) {
                tooltip.add(TooltipGem.hang(line));
            }
        });
        effect.summonProfile().ifPresent(profile -> {
            if (profile.replacesSummons()) {
                appendSummonReplacement(profile, tooltip);
                return;
            }
            List<Component> always = new ArrayList<>();
            List<Component> summon = new ArrayList<>();
            Component head = profile.companion()
                    .filter(kept -> kept.count() > 0)
                    .map(kept -> {
                        summon.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_RESPAWNS,
                                StyleText.formatDecimal(kept.respawnTicks() / (double) SECOND))
                                .withStyle(BODY)));
                        return (Component) TooltipGem.SUMMON.bullet(Component.translatable(KEY_KEEPS,
                                kept.count(), StyleText.registryName(profile.entity().orElse("")))
                                .withStyle(BODY));
                    })
                    .orElse(null);
            if (head == null && !profile.replacesSummons()) {
                head = profile.entity().filter(entity -> !entity.isBlank())
                        .map(entity -> (Component) TooltipGem.SUMMON.bullet(Component.translatable(
                                KEY_SUMMONS_ENTITY, StyleText.registryName(entity)).withStyle(BODY)))
                        .orElse(null);
            }
            profile.maxActive().ifPresent(max -> summon.add(TooltipGem.SUMMON.bullet(
                    Component.translatable(KEY_AT_MOST, max).withStyle(BODY))));
            profile.anchored().filter(count -> count > 0).ifPresent(count ->
                    summon.add(TooltipGem.SUMMON.bullet(Component.translatable(
                            KEY_SUMMON_ANCHORED, count).withStyle(BODY))));
            if (!profile.replacesSummons()) {
                appendProfileStats(profile, always, summon);
            }
            appendCadence(profile.cadence(), profile.abilities(), always);
            profile.finale().filter(finale -> !finale.isBlank()).ifPresent(finale -> {
                summon.add(TooltipGem.SUMMON.bullet(Component.translatable(
                        KEY_SUMMON_FINALE, abilityName(finale)).withStyle(BODY)));
                appendAbilityDescriptions(List.of(finale), summon);
            });
            appendUnder(head, always, summon, tooltip);
        });
        effect.grantAbilities().ifPresent(grant -> {
            List<Component> granted = new ArrayList<>();
            appendCadence(grant.cadence(), grant.cadence().carriedAbilities(), granted);
            if (!grant.actives().isEmpty() && grant.cadence().recastTicks().isEmpty()) {
                granted.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_GRANT_ACTIVES,
                        abilityNames(grant.cadence().carriedAbilities())).withStyle(BODY)));
                appendAbilityDescriptions(grant.cadence().carriedAbilities(), granted);
            }
            if (granted.isEmpty()) {
                return;
            }
            List<Component> filter = new ArrayList<>();
            AffixText.appendFilter(grant.filter(), filter);
            Component head = TooltipGem.SUMMON.bullet(Component.translatable(
                    KEY_GRANT_PREFIX + grant.appliesTo().getSerializedName()).withStyle(BODY));
            List<Component> shown = new ArrayList<>(granted);
            appendUnder(head, shown, filter, tooltip);
        });
    }

    private static void appendUnder(Component head, List<Component> always,
                                    List<Component> details, List<Component> tooltip) {
        boolean shown = ItemTooltip.detailed();
        if (head == null) {
            if (always.isEmpty()) {
                tooltip.addAll(details);
                return;
            }
            head = always.getFirst();
            always = always.subList(1, always.size());
            if (!shown) {
                tooltip.add(details.isEmpty() ? head : ItemTooltip.marked(head));
                tooltip.addAll(always);
                return;
            }
            tooltip.add(head);
            tooltip.addAll(always);
            tooltip.addAll(details);
            return;
        }
        tooltip.add(!shown && !details.isEmpty() ? ItemTooltip.marked(head) : head);
        for (Component line : always) {
            tooltip.add(TooltipGem.hang(line));
        }
        if (!shown) {
            return;
        }
        for (Component line : details) {
            tooltip.add(TooltipGem.hang(line));
        }
    }

    private static void appendProfileStats(SummonProfile profile, List<Component> always,
                                           List<Component> details) {
        profile.damageMultiplier().filter(multiplier -> multiplier != 1.0F).ifPresent(multiplier ->
                details.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_DAMAGE,
                        StyleText.formatDecimal(multiplier)).withStyle(GOOD))));
        profile.damageScaling().ifPresent(scaling ->
                details.add(TooltipGem.SUMMON.bullet(AffixText.scalingLine(scaling))));
        profile.lifespan().ifPresent(ticks -> details.add(TooltipGem.SUMMON.bullet(
                Component.translatable(KEY_SUMMON_LIFE, seconds(ticks)).withStyle(BODY))));
        int recast = profile.cadence().recastTicks().orElse(SummonCadence.NEVER_RECASTS);
        if (recast <= 0 && !profile.abilities().isEmpty()) {
            always.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_ABILITIES,
                    abilityNames(profile.abilities())).withStyle(BODY)));
        }
    }

    private static void appendTunedScalars(StyleAffix affix, AffixTuning tuning,
                                           List<Component> body) {
        tuning.damageMultiplier()
                .filter(value -> value != affix.damageMultiplier())
                .ifPresent(value -> body.add(scalarChange(
                        KEY_TUNED_DAMAGE, StyleText.formatMultiplier(affix.damageMultiplier()),
                        StyleText.formatMultiplier(value), value >= affix.damageMultiplier())));
        tuning.costModifier()
                .filter(value -> value != affix.costModifier())
                .ifPresent(value -> body.add(scalarChange(
                        KEY_TUNED_COST, StyleText.formatMultiplier(affix.costModifier()),
                        StyleText.formatMultiplier(value), value <= affix.costModifier())));
    }

    private static void appendTunedFacts(AffixTuning tuning, List<Component> body) {
        tuning.cooldown().ifPresent(value -> body.add(Component.translatable(KEY_TUNED_COOLDOWN,
                StyleText.formatDecimal(value / (double) SECOND)).withStyle(FRAME)));
        tuning.chance().ifPresent(value -> body.add(Component.translatable(KEY_TUNED_CHANCE,
                StyleText.formatDecimal(value * 100.0D)).withStyle(FRAME)));
    }

    private static Component scalarChange(String key, String was, String now, boolean better) {
        return Component.translatable(key).withStyle(BODY)
                .append(Component.literal(" " + was).withStyle(FRAME))
                .append(Component.literal(" → ").withStyle(FRAME))
                .append(Component.literal(now).withStyle(better ? GOOD : BAD));
    }
}
