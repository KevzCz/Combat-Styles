package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.client.tooltip.AffixPlates;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.affix.EquipmentAffix;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
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

public final class EquipmentTooltip {
    private static final String KEY_SPELL_GRANT = "tooltip.combat_styles.equip_spell_grant";
    private static final String KEY_SPELL_BLOCK = "tooltip.combat_styles.equip_spell_block";
    private static final String KEY_SPELL_TUNE = "tooltip.combat_styles.equip_spell_tune";
    private static final String KEY_SPELL_REMOVE = "tooltip.combat_styles.equip_spell_remove";
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
    private static final String KEY_WINDOW_REFRESH = "tooltip.combat_styles.equip_window_refresh";
    private static final String KEY_SET_CHARGED = "tooltip.combat_styles.equip_set_charged";
    private static final String KEY_SET_RECHARGES = "tooltip.combat_styles.equip_set_recharges";
    private static final String KEY_REPLACE_HEADER = "tooltip.combat_styles.replace_header";
    private static final String KEY_REPLACE_ALL = "tooltip.combat_styles.replace_all";
    private static final String KEY_REPLACE_MAX = "tooltip.combat_styles.replace_max";
    private static final String KEY_REPLACE_DAMAGE = "tooltip.combat_styles.replace_damage";
    private static final String KEY_REPLACE_LIFE = "tooltip.combat_styles.replace_life";
    private static final String KEY_REPLACE_ORDER = "tooltip.combat_styles.replace_order";
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
    private static final String KEY_SLOT = "tooltip.combat_styles.when_slot";
    private static final String KEY_LAST_SLOT = "tooltip.combat_styles.when_last_slot";
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

    private static final String INDENT = "  ";
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

    private EquipmentTooltip() {
    }

    public static void append(EquipmentDefinition definition, List<Component> tooltip) {
        if (definition == null) {
            return;
        }
        AffixPlates.beginTooltip();
        Set<EffectGroup> grouped = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean shown = false;
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
            tooltip.add(ItemTooltip.hint());
        }
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
        tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_SUMMON_DIVE,
                StyleText.formatDecimal(dive.damageShare())).withStyle(BODY)));
    }

    private static void appendSummonReplacement(SummonProfile replacement,
                                                List<Component> tooltip) {
        if (replacement == null || !replacement.replacesSummons()) {
            return;
        }
        tooltip.add(Component.empty());
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

        if (!replacement.abilities().isEmpty()) {
            tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_REPLACE_ORDER,
                    abilityNames(replacement.abilities())).withStyle(COND)));
        }
    }

    private static final String ABILITY_NAME_PREFIX = "ability.";

    private static Component abilityName(String ability) {
        String key = AbilityConfig.key(ability);
        StyleAffix affix = AffixRegistry.get(CombatStyles.MOD_ID + ":" + key);
        if (affix != null) {
            return Component.translatableWithFallback(affix.nameKey(), affix.fallbackName());
        }
        return Component.translatableWithFallback(
                ABILITY_NAME_PREFIX + CombatStyles.MOD_ID + "." + key + ".name",
                StyleText.prettify(key));
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

    private static boolean rewrites(EquipmentDefinition definition, String affixId) {
        for (TriggeredEffect entry : definition.triggered()) {
            AffixTuning tuning = entry.effect().tuneAffixes().get(affixId);
            if (tuning != null && (tuning.onHit().isPresent() || tuning.onSwing().isPresent()
                    || tuning.appliesTo().isPresent() || tuning.cooldown().isPresent()
                    || tuning.chance().isPresent() || tuning.damageMultiplier().isPresent()
                    || tuning.costModifier().isPresent())) {
                return true;
            }
        }
        return false;
    }

    private static boolean appendEffect(EquipmentDefinition definition, TriggeredEffect entry,
                                        boolean conditional,
                                        List<Component> tooltip, double odds) {
        EquipmentEffect effect = entry.effect();
        int start = tooltip.size();
        Component when = entry.tooltipHeader().isBlank()
                ? triggerOf(entry.trigger())
                : TooltipOverride.text(entry.tooltipHeader());
        if (!entry.tooltipLines().isEmpty()) {
            if (when != null) {
                tooltip.add(headerLine(entry.trigger(), when));
            }
            for (String line : entry.tooltipLines()) {
                tooltip.add(TooltipGem.CUSTOM.bullet(TooltipOverride.text(line).withStyle(BODY)));
            }
            markOdds(tooltip, start, odds);
            return when != null;
        }
        appendWornAttributes(definition, effect, entry.cooldownTicks(), !conditional, tooltip);
        boolean headed = tooltip.size() > start || when != null;
        int oddsAt = tooltip.size();
        if (when != null) {
            tooltip.add(headerLine(entry.trigger(), when));
        }
        if (conditional) {
            appendConditionalAttributes(effect, entry.cooldownTicks(), tooltip);
        }
        appendSubstitute(effect, tooltip);
        appendSubstitution(effect, tooltip);
        appendGrants(definition, effect, tooltip);
        appendSpellLocks(effect.spells(), tooltip);
        for (DamageBonus bonus : effect.damageBonus()) {
            tooltip.add(TooltipGem.DAMAGE.bullet(bonus.describe().withStyle(BODY)));
        }
        appendSelfEffects(entry.selfEffects(), tooltip);
        appendOwnEffects(definition, effect, tooltip);
        appendEventEffects(definition, effect, tooltip);
        if (TooltipContextAccess.expanded()) {
            appendAugment(effect.augment().orElse(null), tooltip);
            appendEcho(echoOf(effect), tooltip);
            appendRepeat(effect, tooltip);
            appendSummonReplacement(effect.summonProfile().orElse(null), tooltip);
            appendSummonDive(effect.summonDive().orElse(null), tooltip);
        }
        appendGate(entry.cooldown(), tooltip);
        markOdds(tooltip, oddsAt < tooltip.size() ? oddsAt : start, odds);
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
        for (SpellTune tuned : locks.tune()) {
            tooltip.add(TooltipGem.SPELL.bullet(Component.translatable(KEY_SPELL_TUNE,
                    StyleText.spellName(tuned.spell())).withStyle(BODY)));
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
                : Component.translatable(KEY_COOLDOWN, span);
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
            tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_WINDOW_REFRESH).withStyle(FRAME)));
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
        state.sprinting().ifPresent(value -> {
            if (value) {
                parts.add(Component.translatable(KEY_SPRINTING));
            }
        });
        state.airborne().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_AIRBORNE : KEY_GROUNDED)));
        state.crouching().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_CROUCHING : KEY_STANDING)));
        state.behindTarget().ifPresent(value -> {
            if (value) {
                parts.add(Component.translatable(KEY_BEHIND));
            }
        });
        state.night().ifPresent(value ->
                parts.add(Component.translatable(value ? KEY_NIGHT : KEY_DAY)));
        state.damagedWithin().ifPresent(ticks ->
                parts.add(Component.translatable(KEY_DAMAGED_WITHIN, seconds(ticks))));
        state.hasSpell().ifPresent(value -> {
            if (value) {
                parts.add(Component.translatable(KEY_HAS_SPELL));
            }
        });
        state.castingSpell().ifPresent(value -> {
            if (value) {
                parts.add(Component.translatable(KEY_CASTING_SPELL));
            }
        });
        parts.addAll(ItemGateText.parts(state));
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
        if (trigger.reactsToHurt()) {
            parts.add(detailed(KEY_ON_HURT, KEY_ON_HURT_TYPED,
                    namesOf(trigger.hurt().damageTypes())));
        }
        if (trigger.activation().reactsToShoot()) {
            parts.add(detailed(KEY_ON_SHOOT, KEY_ON_SHOOT_TYPED,
                    entityNamesOf(trigger.activation().shoot().entities())));
        }
        if (trigger.activation().reactsToBlock()) {
            parts.add(detailed(KEY_ON_BLOCK, KEY_ON_BLOCK_TYPED,
                    namesOf(trigger.activation().block().damageTypes())));
        }
        if (trigger.activation().usesItem()) {
            parts.add(useLine(trigger.activation().onUse()));
        }
        if (!trigger.activation().cast().isEmpty()) {
            parts.add(castLine(trigger.activation().cast()));
        }
        if (trigger.activation().reactsToProjectileHit()) {
            parts.add(detailed(KEY_ON_PROJECTILE_HIT, KEY_ON_PROJECTILE_HIT_TYPED,
                    entityNamesOf(trigger.activation().projectileHit().entities())));
        }
        if (trigger.phase().onPlayerHit().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_PLAYER_HIT));
        }
        if (trigger.phase().onSwing().orElse(false)) {
            parts.add(Component.translatable(KEY_ON_SWING));
        }
        stateParts(trigger.phase().state(), parts);
        dealtParts(trigger.activation().dealt(), parts);
        if (trigger.lastSlot().orElse(false)) {
            parts.add(Component.translatable(KEY_LAST_SLOT));
        }
        trigger.slotBand().ifPresent(band ->
                parts.add(Component.translatable(band.translationKey())));
        trigger.slots().ifPresent(selection ->
                parts.add(Component.translatable(KEY_SLOTS, slotsName(selection))));
        trigger.slotIndex().ifPresent(index ->
                parts.add(Component.translatable(KEY_SLOT, index + 1)));
        trigger.everyNthSwing().ifPresent(every ->
                parts.add(Component.translatable(KEY_NTH, every)));
        trigger.idleTicks().ifPresent(ticks ->
                parts.add(Component.translatable(KEY_IDLE,
                        StyleText.formatDecimal(ticks / (double) SECOND))));
        HealthGate health = trigger.health();
        health.selfAbove().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_SELF_ABOVE,
                        StyleText.formatDecimal(ratio * 100.0D))));
        health.selfBelow().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_SELF_BELOW,
                        StyleText.formatDecimal(ratio * 100.0D))));
        health.targetAbove().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_TARGET_ABOVE,
                        StyleText.formatDecimal(ratio * 100.0D))));
        health.targetBelow().ifPresent(ratio ->
                parts.add(Component.translatable(KEY_TARGET_BELOW,
                        StyleText.formatDecimal(ratio * 100.0D))));
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
        if (phase.onSummonHit().orElse(false)) {
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
            if (state.hasSpell().orElse(false)) {
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

    private static Component slotsName(SlotSelection selection) {
        if (selection.range().isPresent()) {
            SlotSelection.Range range = selection.range().get();
            return Component.translatable(KEY_SLOT_RANGE,
                    slotName(range.from()), slotName(range.to()));
        }
        List<Integer> indices = selection.indices();
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
        if (index < 0) {
            return Component.translatable(KEY_SLOT_LAST);
        }
        int ordinal = index + 1;
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
                                         List<Component> tooltip) {
        if (effect.onHit().isEmpty() && effect.onSwing().isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        AffixText.appendEffects(EquipmentAffix.of(new EquipmentRuntime.Match(
                definition, new TriggeredEffect(EquipmentTrigger.ALWAYS, effect, EffectCooldown.NONE, false),
                definition.id())), lines, false);
        for (Component line : lines) {
            tooltip.add(TooltipGem.EFFECT.bullet(line));
        }
    }

    private static void appendEventEffects(EquipmentDefinition definition, EquipmentEffect effect,
                                           List<Component> tooltip) {
        for (EventKind kind : EventKind.values()) {
            for (AffixEffects.OnHit payload : effect.events().get(kind)) {
                tooltip.add(TooltipGem.EFFECT.bullet(Component.translatable(kind.labelKey())
                        .withStyle(BODY)));
                List<Component> lines = new ArrayList<>();
                EquipmentEffect alone = EquipmentEffects.withOnHit(EquipmentEffect.EMPTY,
                        Optional.of(payload));
                AffixText.appendEffects(EquipmentAffix.of(new EquipmentRuntime.Match(definition,
                        new TriggeredEffect(EquipmentTrigger.ALWAYS, alone, EffectCooldown.NONE,
                                false), definition.id())), lines, false);
                for (Component line : lines) {
                    tooltip.add(TooltipGem.hang(line));
                }
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
        for (String id : effect.grantAffixes()) {
            StyleAffix affix = AffixRegistry.get(id);
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
                tooltip.add(TooltipGem.hang(line));
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
        for (Map.Entry<String, AttackAttribute> entry : set.entrySet()) {
            AttackAttribute value = entry.getValue();
            if (value == null || value.amount() == 0.0D) {
                continue;
            }
            tooltip.add(TooltipGem.STAT.bullet(StyleText.attributeLine(entry.getKey(), value, BODY)
                    .append(Component.literal(" ")
                            .append(Component.translatable(KEY_SET_CHARGED)).withStyle(FRAME))));
        }
        if (TooltipContextAccess.expanded()) {
            tooltip.add(TooltipGem.NOTE.bullet(Component.translatable(KEY_SET_RECHARGES,
                            StyleText.formatDecimal(cooldownTicks / (double) SECOND))
                    .withStyle(FRAME)));
        }
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

    private static void appendWornAttributes(EquipmentDefinition definition,
                                             EquipmentEffect effect, int cooldownTicks,
                                             boolean includeSet, List<Component> tooltip) {
        List<Component> rewritten = new ArrayList<>();
        for (String id : effect.tuneAffixes().keySet()) {
            StyleAffix affix = AffixRegistry.get(id);
            if (affix == null || !rewrites(definition, id)) {
                continue;
            }
            MutableComponent line = TooltipGem.of(affix.rarity()).bullet(
                    AffixText.name(affix).copy().withStyle(affix.rarity().color()));
            AffixPlates.attach(line, Component.translatable(KEY_CHANGED), PLATE_COLOUR);
            AffixTuning tuning = tuningFor(definition, id);
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
                rewritten.add(TooltipGem.hang(changed));
            }
        }
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
            profile.companion().filter(companion -> companion.count() > 0).ifPresent(companion -> {
                tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_KEEPS,
                        companion.count(), StyleText.registryName(profile.entity().orElse("")))
                        .withStyle(BODY)));
                tooltip.add(TooltipGem.SUMMON.bullet(Component.translatable(KEY_RESPAWNS,
                        StyleText.formatDecimal(companion.respawnTicks() / (double) SECOND))
                        .withStyle(BODY)));
            });
            profile.maxActive().ifPresent(max -> tooltip.add(TooltipGem.SUMMON.bullet(
                    Component.translatable(KEY_AT_MOST, max).withStyle(BODY))));
        });
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
