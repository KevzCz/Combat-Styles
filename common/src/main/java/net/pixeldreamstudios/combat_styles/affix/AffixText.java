package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumber;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumbers;
import net.pixeldreamstudios.combat_styles.custom.CustomScopes;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.item.ItemTooltip;
import net.pixeldreamstudios.combat_styles.item.TooltipContextAccess;
import net.pixeldreamstudios.combat_styles.item.TooltipGem;
import net.pixeldreamstudios.combat_styles.entity.MobFilter;
import net.pixeldreamstudios.combat_styles.entity.RaiseRules;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.ItemGateText;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AffixText {

    private static final ChatFormatting HEADER = ChatFormatting.GOLD;
    private static final ChatFormatting BODY = ChatFormatting.GRAY;
    private static final ChatFormatting GOOD = ChatFormatting.GREEN;
    private static final ChatFormatting BAD = ChatFormatting.RED;
    private static final ChatFormatting COND = ChatFormatting.AQUA;
    private static final ChatFormatting FRAME = ChatFormatting.DARK_GRAY;
    private static final String ARROW = "→";
    private static final String WORD_SPLIT = " ";
    private static final int NO_PARTNER = -1;
    private static final String PHASE_SWING = "swing";
    private static final String PHASE_HIT = "hit";
    private static final String KEY_LINE_NEW = "tooltip.combat_styles.line_new";
    private static final String KEY_LINE_REMOVED = "tooltip.combat_styles.line_removed";
    private static final ChatFormatting HIGHLIGHT = ChatFormatting.WHITE;
    private static final String KEY_CONVERTS = "affix.combat_styles.effect.converts";
    private static final String KEY_BONUS = "affix.combat_styles.effect.bonus";
    private static final String KEY_BONUS_SCHOOL = "affix.combat_styles.effect.bonus_school";
    private static final String KEY_AMPLIFIES = "affix.combat_styles.effect.amplifies";
    private static final String KEY_IGNITE = "affix.combat_styles.effect.ignite";
    private static final String KEY_FREEZE = "affix.combat_styles.effect.freeze";
    private static final String KEY_LIFESTEAL = "affix.combat_styles.effect.lifesteal";
    private static final String KEY_KNOCKBACK = "affix.combat_styles.effect.knockback";
    private static final String KEY_PULL = "affix.combat_styles.effect.pull";
    private static final String KEY_SPLASH = "affix.combat_styles.effect.splash";
    private static final String KEY_SLASH_WAVE = "affix.combat_styles.effect.slash_wave";
    private static final String KEY_PROJECTILE = "affix.combat_styles.effect.projectile";
    private static final String KEY_PROJECTILE_COUNT =
            "affix.combat_styles.effect.projectile_count";
    private static final String KEY_PROJECTILE_DAMAGE =
            "affix.combat_styles.effect.projectile_damage";
    private static final String KEY_CAST_SPELL = "affix.combat_styles.effect.cast_spell";
    private static final String KEY_CAST_SPELL_SELF = "affix.combat_styles.effect.cast_spell_self";
    private static final String KEY_CAST_SPELL_DAMAGE =
            "affix.combat_styles.effect.cast_spell_damage";
    private static final String KEY_SPEC_FLAT = "affix.combat_styles.spec.flat";
    private static final String KEY_SPEC_RATIO = "affix.combat_styles.spec.ratio";
    private static final String KEY_ONLY_TARGET_EFFECTS = "affix.combat_styles.effect.only_target_effects";
    private static final String KEY_ONLY_TARGET_BURNING = "affix.combat_styles.effect.only_target_burning";
    private static final String KEY_ONLY_TARGET_NOT_BURNING = "affix.combat_styles.effect.only_target_not_burning";
    private static final String EFFECT_SEPARATOR = ", ";
    private static final String KEY_SPEC_OF_HIT = "affix.combat_styles.spec.of_hit";
    private static final String KEY_SPEC_MAX_HEALTH = "affix.combat_styles.spec.max_health";
    private static final String KEY_SPEC_MISSING_HEALTH =
            "affix.combat_styles.spec.missing_health";
    private static final String KEY_SPEC_SELF_MISSING = "affix.combat_styles.spec.self_missing";
    private static final String KEY_SPEC_SCALED = "affix.combat_styles.spec.scaled";
    private static final String KEY_SPEC_SCALE_PER_POINT = "affix.combat_styles.spec.scale_per_point";
    private static final String KEY_SPEC_SCALE_MULTIPLIER = "affix.combat_styles.spec.scale_multiplier";
    private static final String KEY_SPEC_SCALE_DIVISOR = "affix.combat_styles.spec.scale_divisor";
    private static final String KEY_SPEC_SCALE_CAP = "affix.combat_styles.spec.scale_cap";
    private static final String KEY_SPEC_STRONGEST = "affix.combat_styles.spec.strongest";
    private static final String KEY_SPEC_OR = "affix.combat_styles.spec.or";
    private static final String KEY_SCALES_WITH = "affix.combat_styles.effect.scales_with";
    private static final String KEY_WITH_BONUS = "affix.combat_styles.effect.with_bonus";
    private static final String KEY_WITH_MULTIPLIER = "affix.combat_styles.effect.with_multiplier";
    private static final double PERCENT_SCALE = 100.0D;
    private static final String KEY_DAMAGE_MULTIPLIED = "affix.combat_styles.effect.damage_multiplied";
    private static final String KEY_DAMAGE_FIELD = "damage";
    private static final String KEY_DAMAGES_FIELD = "damages";
    private static final String PATH_JOIN = "/";
    private static final String KEY_SCALING_SCOPE = "scaling";
    private static final String KEY_ANY_SPELL_POWER = "affix.combat_styles.spec.any_spell_power";
    private static final String KEY_STRONGEST_SCHOOL = "affix.combat_styles.effect.strongest_school";
    private static final String KEY_BONUS_SCALED = "affix.combat_styles.effect.bonus_scaled";
    private static final String PERCENT = "%";
    private static final String SPELL_POWER_PREFIX = SpellPowerAccess.MOD_ID + ":";
    private static final int GROUPED_SCHOOLS_MIN = 2;
    private static final String KEY_SPEC_MULTIPLIED = "affix.combat_styles.spec.multiplied";
    private static final String KEY_SPEC_AND = "affix.combat_styles.spec.and";
    private static final String KEY_EFFECT = "affix.combat_styles.effect.status";
    private static final String KEY_SELF = "affix.combat_styles.effect.self";
    private static final String KEY_SCALES = "affix.combat_styles.effect.scales";
    private static final String KEY_SCALES_SCHOOL = "affix.combat_styles.effect.scales_school";
    private static final String KEY_ATTUNED = "affix.combat_styles.effect.attuned";
    private static final String KEY_ARMOUR = "affix.combat_styles.effect.ignores_armour";
    private static final String KEY_CHANCE = "affix.combat_styles.effect.chance";
    private static final String KEY_AS_SCHOOL = "affix.combat_styles.effect.as_school";
    private static final String KEY_DASH = "affix.combat_styles.effect.dash";
    private static final String KEY_DASH_TARGET = "affix.combat_styles.effect.dash_target";
    private static final String KEY_DASH_BACK = "affix.combat_styles.effect.dash_back";
    private static final String KEY_EXECUTE = "affix.combat_styles.effect.execute";
    private static final String KEY_EXECUTE_KILL = "affix.combat_styles.effect.execute_kill";
    private static final String KEY_REPEAT_ON_KILL = "affix.combat_styles.effect.repeat_on_kill";
    private static final String KEY_REFUND_SLOT = "affix.combat_styles.effect.refund_slot";
    private static final String KEY_CHAIN = "affix.combat_styles.effect.chain";
    private static final String KEY_MOMENTUM = "affix.combat_styles.effect.momentum";
    private static final String KEY_HEAL = "affix.combat_styles.effect.heal";
    private static final String KEY_HEAL_ABSORB = "affix.combat_styles.effect.heal_absorb";
    private static final String KEY_DISARM = "affix.combat_styles.effect.disarm";
    private static final String KEY_VAULT = "affix.combat_styles.effect.vault";
    private static final String KEY_BLINK = "affix.combat_styles.effect.blink";
    private static final String KEY_BLINK_BEHIND = "affix.combat_styles.effect.blink_behind";
    private static final String KEY_BLINK_BACK = "affix.combat_styles.effect.blink_back";
    private static final String KEY_SHIELD = "affix.combat_styles.effect.shield";
    private static final String KEY_CLEAVE = "affix.combat_styles.effect.cleave";
    private static final String KEY_CONDITION_SELF_LOW = "affix.combat_styles.effect.condition_self_low";
    private static final String KEY_CONDITION_SELF_HIGH = "affix.combat_styles.effect.condition_self_high";
    private static final String KEY_CONDITION_TARGET_LOW = "affix.combat_styles.effect.condition_target_low";
    private static final String KEY_CONDITION_TARGET_HIGH = "affix.combat_styles.effect.condition_target_high";
    private static final String KEY_ONLY_TARGET_LOW = "affix.combat_styles.effect.only_target_low";
    private static final String KEY_ONLY_TARGET_HIGH = "affix.combat_styles.effect.only_target_high";
    private static final String KEY_ONLY_SELF_LOW = "affix.combat_styles.effect.only_self_low";
    private static final String KEY_ONLY_SELF_HIGH = "affix.combat_styles.effect.only_self_high";
    private static final String KEY_CONDITION_BEHIND = "affix.combat_styles.effect.condition_behind";
    private static final String KEY_CONDITION_AIRBORNE = "affix.combat_styles.effect.condition_airborne";
    private static final String KEY_CONDITION_SPRINTING = "affix.combat_styles.effect.condition_sprinting";
    private static final String KEY_CONDITION_CROUCHING = "affix.combat_styles.effect.condition_crouching";
    private static final String KEY_ONLY_CROUCHING = "affix.combat_styles.effect.only_crouching";
    private static final String KEY_CONDITION_NIGHT = "affix.combat_styles.effect.condition_night";
    private static final String KEY_ONLY_PREFIX = "affix.combat_styles.effect.only_prefix";
    private static final String KEY_ONLY_STATE = "affix.combat_styles.effect.only_state";
    private static final String KEY_ONLY_BEHIND = "affix.combat_styles.effect.only_behind";
    private static final String KEY_ONLY_AIRBORNE = "affix.combat_styles.effect.only_airborne";
    private static final String KEY_ONLY_SPRINTING = "affix.combat_styles.effect.only_sprinting";
    private static final String KEY_ONLY_NIGHT = "affix.combat_styles.effect.only_night";
    private static final String KEY_ONLY_AFTER_HURT = "affix.combat_styles.effect.only_after_hurt";
    private static final String KEY_ONLY_ON_MISS = "affix.combat_styles.effect.only_on_miss";
    private static final String KEY_ONLY_DAMAGE_TYPE =
            "affix.combat_styles.effect.only_damage_type";
    private static final String KEY_ONLY_TAKEN_DAMAGE_TYPE =
            "affix.combat_styles.effect.only_taken_damage_type";
    private static final String TYPE_SEPARATOR = ", ";
    private static final String TAG_MARK = "#";
    private static final String KEY_HASTEN = "affix.combat_styles.effect.hasten";
    private static final String KEY_ORDERS = "affix.combat_styles.effect.orders";
    private static final String KEY_QUICKEN = "affix.combat_styles.effect.quicken";
    private static final String KEY_QUICKEN_CHANCE = "affix.combat_styles.effect.quicken_chance";
    private static final String KEY_ORDERS_TARGETS = "affix.combat_styles.effect.orders_targets";
    private static final String KEY_TARGETING = "affix.combat_styles.targeting.";
    private static final String KEY_SLOT_AT = "affix.combat_styles.effect.slot_at";
    private static final String KEY_SLOT_AT_BONUS = "affix.combat_styles.effect.slot_at_bonus";
    private static final String KEY_SUMMON_COUNT = "affix.combat_styles.effect.summon_count";
    private static final String KEY_SLOT_FIRST = "affix.combat_styles.effect.slot_first";
    private static final String KEY_SLOT_NOT_FIRST = "affix.combat_styles.effect.slot_not_first";
    private static final String KEY_SLOT_LAST = "affix.combat_styles.effect.slot_last";
    private static final String KEY_SLOT_NOT_LAST = "affix.combat_styles.effect.slot_not_last";
    private static final String KEY_SLOT_FROM_END = "affix.combat_styles.effect.slot_from_end";
    private static final String KEY_SLOT_AFTER = "affix.combat_styles.effect.slot_after";
    private static final String KEY_SLOT_FIRST_PLAIN = "affix.combat_styles.effect.slot_first_plain";
    private static final String KEY_SLOT_NOT_FIRST_PLAIN = "affix.combat_styles.effect.slot_not_first_plain";
    private static final String KEY_SLOT_LAST_PLAIN = "affix.combat_styles.effect.slot_last_plain";
    private static final String KEY_SLOT_NOT_LAST_PLAIN = "affix.combat_styles.effect.slot_not_last_plain";
    private static final String KEY_SLOT_AFTER_PLAIN = "affix.combat_styles.effect.slot_after_plain";
    private static final String KEY_SLOT_AFTER_CATEGORY_PLAIN = "affix.combat_styles.effect.slot_after_category_plain";
    private static final String KEY_REQUIRES_ATTACKS = "affix.combat_styles.effect.requires_attacks";
    private static final String KEY_PER_PRECEDING = "affix.combat_styles.effect.per_preceding";
    private static final String KEY_PER_PRECEDING_CAPPED = "affix.combat_styles.effect.per_preceding_capped";
    private static final String KEY_SAME_TARGET = "affix.combat_styles.effect.same_target";
    private static final String KEY_SLOT_AFTER_CATEGORY = "affix.combat_styles.effect.slot_after_category";
    private static final String KEY_ATTRIBUTE_SELF = "affix.combat_styles.effect.attribute_self";
    private static final String KEY_ATTRIBUTE_TARGET = "affix.combat_styles.effect.attribute_target";
    private static final String KEY_ATTRIBUTE_STACKS = "affix.combat_styles.effect.attribute_stacks";
    private static final String KEY_ATTACK_ATTRIBUTE = "affix.combat_styles.effect.attack_attribute";
    private static final String KEY_ATTACK_ATTRIBUTE_ONCE = "affix.combat_styles.effect.attack_attribute_once";
    private static final String KEY_ECHO = "affix.combat_styles.effect.echo";
    private static final String KEY_COMMANDS = "affix.combat_styles.effect.commands";
    private static final String KEY_COOLDOWN = "affix.combat_styles.effect.cooldown";
    private static final String KEY_TARGET_CAP = "affix.combat_styles.effect.target_cap";
    private static final String KEY_COST_HEALTH = "affix.combat_styles.effect.cost_health";
    private static final String KEY_COST_HUNGER = "affix.combat_styles.effect.cost_hunger";
    private static final String KEY_COST_LEVELS = "affix.combat_styles.effect.cost_levels";
    private static final String KEY_COST_CANCELS = "affix.combat_styles.effect.cost_cancels";
    private static final String KEY_COST_ANYWAY = "affix.combat_styles.effect.cost_anyway";
    private static final String KEY_SUMMON_DAMAGE = "affix.combat_styles.effect.summon_damage";
    private static final String KEY_RAISE = "affix.combat_styles.effect.raise_dead";
    private static final String KEY_RAISE_ONLY = "affix.combat_styles.effect.raise_dead_only";
    private static final String KEY_RAISE_NEVER = "affix.combat_styles.effect.raise_dead_never";
    private static final String KEY_RAISE_PROFILE = "tooltip.combat_styles.raise_dead_profile.";
    private static final String KEY_SUMMON = "affix.combat_styles.effect.summon";
    private static final int TICKS_PER_SECOND = 20;
    private static final String KEY_ON_SWING = "affix.combat_styles.trigger.on_swing";
    private static final String KEY_ON_HIT = "affix.combat_styles.trigger.on_hit";
    private static final String KEY_ON_BOTH = "affix.combat_styles.trigger.on_both";
    private static final String KEY_ON_SWING_ONLY = "affix.combat_styles.trigger.on_swing_only";
    private static final String KEY_ON_HIT_ONLY = "affix.combat_styles.trigger.on_hit_only";

    private AffixText() {
    }

    public static Component name(StyleAffix affix) {
        return Component.translatableWithFallback(affix.nameKey(), affix.fallbackName())
                .withStyle(affix.rarity().color());
    }

    public static void appendEffects(StyleAffix affix, List<Component> lines) {
        appendEffects(affix, lines, true);
    }

    public static Component compared(Component before, Component after) {
        String was = before.getString();
        String now = after.getString();
        if (was.equals(now)) {
            return after;
        }
        Component inline = inlineDiff(was, now);
        return inline != null ? inline : wholeLineDiff(was, after);
    }

    private static Component wholeLineDiff(String was, Component after) {
        return Component.literal(was).withStyle(FRAME)
                .append(Component.literal(" " + ARROW + " ").withStyle(FRAME))
                .append(after);
    }

    private static Component inlineDiff(String was, String now) {
        List<String> oldWords = List.of(was.split(WORD_SPLIT, -1));
        List<String> newWords = List.of(now.split(WORD_SPLIT, -1));
        MutableComponent line = Component.empty();
        int changed = 0;
        int oldAt = 0;
        int newAt = 0;
        while (oldAt < oldWords.size() || newAt < newWords.size()) {
            if (oldAt < oldWords.size() && newAt < newWords.size()
                    && oldWords.get(oldAt).equals(newWords.get(newAt))) {
                line.append(Component.literal(oldWords.get(oldAt)).withStyle(BODY))
                        .append(Component.literal(" "));
                oldAt++;
                newAt++;
                continue;
            }
            int[] resync = resyncAt(oldWords, newWords, oldAt, newAt);
            int oldRun = resync[0];
            int newRun = resync[1];
            String removed = String.join(" ", oldWords.subList(oldAt, oldRun));
            String added = String.join(" ", newWords.subList(newAt, newRun));
            changed += Math.max(oldRun - oldAt, newRun - newAt);
            if (!removed.isEmpty()) {
                line.append(Component.literal(removed).withStyle(FRAME))
                        .append(Component.literal(" " + ARROW + " ").withStyle(FRAME));
            }
            if (!added.isEmpty()) {
                line.append(Component.literal(added).withStyle(HIGHLIGHT))
                        .append(Component.literal(" "));
            }
            oldAt = oldRun;
            newAt = newRun;
        }
        return changed * 2 > Math.max(oldWords.size(), newWords.size()) ? null : line;
    }

    private static int[] resyncAt(List<String> oldWords, List<String> newWords,
                                  int oldAt, int newAt) {
        int reach = Math.max(oldWords.size() - oldAt, newWords.size() - newAt);
        for (int span = 1; span <= reach; span++) {
            for (int step = 0; step <= span; step++) {
                int oldProbe = oldAt + step;
                int newProbe = newAt + span - step;
                if (oldProbe < oldWords.size() && newProbe < newWords.size()
                        && oldWords.get(oldProbe).equals(newWords.get(newProbe))) {
                    return new int[] {oldProbe, newProbe};
                }
                oldProbe = oldAt + span - step;
                newProbe = newAt + step;
                if (oldProbe < oldWords.size() && newProbe < newWords.size()
                        && oldWords.get(oldProbe).equals(newWords.get(newProbe))) {
                    return new int[] {oldProbe, newProbe};
                }
            }
        }
        return new int[] {oldWords.size(), newWords.size()};
    }

    public static void appendTuned(StyleAffix base, StyleAffix tuned, List<Component> lines) {
        appendTuned(base, tuned, lines, false);
    }

    public static void appendTuned(StyleAffix base, StyleAffix tuned, List<Component> lines,
                                   boolean changedOnly) {
        List<EffectLines.Tagged> after = EffectLines.of(tuned).tagged();
        List<EffectLines.Tagged> before = EffectLines.of(base).tagged();

        boolean[] takenOld = new boolean[before.size()];
        int[] partnerOf = new int[after.size()];
        Arrays.fill(partnerOf, NO_PARTNER);

        for (int index = 0; index < after.size(); index++) {
            partnerOf[index] = partnerFor(before, takenOld, after, index, true);
        }
        for (int index = 0; index < after.size(); index++) {
            if (partnerOf[index] == NO_PARTNER) {
                partnerOf[index] = partnerFor(before, takenOld, after, index, false);
            }
        }

        int emittedOld = 0;
        for (int index = 0; index < after.size(); index++) {
            int partner = partnerOf[index];
            Component line = after.get(index).line();
            if (partner == NO_PARTNER) {
                lines.add(plated(line, KEY_LINE_NEW, GOOD));
                continue;
            }
            emittedOld = appendDropped(before, takenOld, emittedOld, partner, lines);
            Component was = before.get(partner).line();
            if (was.getString().equals(line.getString())) {
                if (!changedOnly) {
                    lines.add(line);
                }
            } else {
                lines.add(compared(was, line));
            }
        }
        appendDropped(before, takenOld, emittedOld, before.size(), lines);
    }

    private static int partnerFor(List<EffectLines.Tagged> before, boolean[] takenOld,
                                  List<EffectLines.Tagged> after, int index, boolean exact) {
        EffectLines.Tagged want = after.get(index);
        int ordinal = ordinalOf(after, index);
        for (int probe = 0; probe < before.size(); probe++) {
            if (takenOld[probe] || !before.get(probe).tag().equals(want.tag())) {
                continue;
            }
            if (ordinalOf(before, probe) != ordinal) {
                continue;
            }
            if (exact && !before.get(probe).line().getString()
                    .equals(want.line().getString())) {
                continue;
            }
            takenOld[probe] = true;
            return probe;
        }
        return NO_PARTNER;
    }

    private static int ordinalOf(List<EffectLines.Tagged> lines, int index) {
        String tag = lines.get(index).tag();
        int ordinal = 0;
        for (int probe = 0; probe < index; probe++) {
            if (lines.get(probe).tag().equals(tag)) {
                ordinal++;
            }
        }
        return ordinal;
    }

    private static int appendDropped(List<EffectLines.Tagged> before, boolean[] takenOld,
                                     int from, int until, List<Component> lines) {
        for (int index = from; index < until; index++) {
            if (!takenOld[index]) {
                lines.add(plated(before.get(index).line().copy().withStyle(FRAME),
                        KEY_LINE_REMOVED, BAD));
            }
        }
        return Math.max(from, until);
    }

    static void phaseOf(List<Component> lines, String name) {
        if (lines instanceof EffectLines tagged) {
            tagged.phase(name);
        }
    }

    static void emit(List<Component> lines, List<EffectLines.Tagged> tagged) {
        if (lines instanceof EffectLines target) {
            target.addTagged(tagged);
            return;
        }
        for (EffectLines.Tagged line : tagged) {
            lines.add(line.line());
        }
    }

    static void scopeOf(List<Component> lines, String name) {
        if (lines instanceof EffectLines tagged) {
            tagged.scope(name);
        }
    }

    static void scopeOf(List<Component> lines, String name, int index) {
        if (lines instanceof EffectLines tagged) {
            tagged.scope(name, index);
        }
    }

    private static Component firedName(AffixEffects.Projectile projectile) {
        if (!projectile.name().isBlank()) {
            return Component.literal(projectile.name());
        }
        if (!projectile.spellProjectile().isEmpty()) {
            return StyleText.spellName(projectile.spellProjectile().spell());
        }
        return StyleText.registryName(projectile.entity());
    }

    private static Component plated(Component line, String key, ChatFormatting colour) {
        return Component.translatable(key).withStyle(colour)
                .append(Component.literal(" "))
                .append(line);
    }

    public static void appendEffects(StyleAffix affix, List<Component> lines, boolean headings) {
        StyleAffix shown = liveFor(affix);
        for (DamageBonus bonus : shown.damageBonus()) {
            lines.add(bonus.describe().withStyle(BODY));
        }
        boolean both = !shown.onSwing().isEmpty() && !shown.onHit().isEmpty();
        if (both) {
            appendBothPhases(shown, lines, headings);
        } else {
            if (!shown.onSwing().isEmpty()) {
                phaseOf(lines, PHASE_SWING);
                appendTrigger(shown, shown.onSwing(), lines);
            }
            if (!shown.onHit().isEmpty()) {
                phaseOf(lines, PHASE_HIT);
                appendTrigger(shown, shown.onHit(), lines);
            }
        }
        appendScaling(affix, lines);
    }

    private static StyleAffix liveFor(StyleAffix affix) {
        Player viewer = TooltipContextAccess.viewer();
        if (ItemTooltip.detailed() || viewer == null
                || (!ScaledNumbers.has(affix.onHit()) && !ScaledNumbers.has(affix.onSwing()))) {
            return affix;
        }
        return affix.withPayloads(ScaledNumbers.resolved(affix.onHit(), viewer),
                ScaledNumbers.resolved(affix.onSwing(), viewer));
    }

    private static String percentOf(float ratio) {
        return StyleText.formatDecimal(ratio * PERCENT_SCALE) + PERCENT;
    }

    private static String scaledPercent(AffixEffects.Damage damage) {
        Player viewer = TooltipContextAccess.viewer();
        float base = Math.max(0.0F, damage.ratio());
        if (damage.scaling().isEmpty() || viewer == null) {
            return percentOf(base);
        }
        float now = AffixImpacts.ratioOf(viewer, damage);
        if (!ItemTooltip.detailed() || now == base) {
            return percentOf(now);
        }
        return Component.translatable(KEY_WITH_BONUS, percentOf(base), percentOf(now - base)).getString();
    }

    private static void appendScaling(StyleAffix affix, List<Component> lines) {
        List<String> sources = new ArrayList<>(ScaledNumbers.sourcesOf(affix.onSwing()));
        for (String source : ScaledNumbers.sourcesOf(affix.onHit())) {
            if (!sources.contains(source)) {
                sources.add(source);
            }
        }
        if (sources.isEmpty()) {
            return;
        }
        scopeOf(lines, KEY_SCALING_SCOPE);
        MutableComponent names = Component.empty();
        for (int index = 0; index < sources.size(); index++) {
            if (index > 0) {
                names.append(Component.literal(", "));
            }
            names.append(StyleText.scalingSourceName(sources.get(index)));
        }
        lines.add(TooltipGem.detail(Component.translatable(KEY_SCALES_WITH, names).withStyle(FRAME)));
    }

    private static void appendBothPhases(StyleAffix affix, List<Component> lines,
                                         boolean headings) {
        EffectLines swing = new EffectLines();
        swing.phase(PHASE_SWING);
        appendTrigger(affix, affix.onSwing(), swing);
        EffectLines hit = new EffectLines();
        hit.phase(PHASE_HIT);
        appendTrigger(affix, affix.onHit(), hit);

        Set<String> hitText = new LinkedHashSet<>();
        for (EffectLines.Tagged line : hit.tagged()) {
            hitText.add(line.line().getString());
        }
        List<EffectLines.Tagged> shared = new ArrayList<>();
        List<EffectLines.Tagged> swingOnly = new ArrayList<>();
        for (EffectLines.Tagged line : swing.tagged()) {
            if (hitText.remove(line.line().getString())) {
                shared.add(line);
            } else {
                swingOnly.add(line);
            }
        }
        List<EffectLines.Tagged> hitOnly = new ArrayList<>();
        for (EffectLines.Tagged line : hit.tagged()) {
            if (hitText.contains(line.line().getString())) {
                hitOnly.add(line);
            }
        }

        if (!shared.isEmpty()) {
            if (headings) {
                lines.add(Component.translatable(KEY_ON_BOTH).withStyle(BODY));
            }
            emit(lines, shared);
        }
        if (!swingOnly.isEmpty()) {
            if (headings) {
                lines.add(Component.translatable(
                        shared.isEmpty() ? KEY_ON_SWING : KEY_ON_SWING_ONLY).withStyle(BODY));
            }
            emit(lines, swingOnly);
        }
        if (!hitOnly.isEmpty()) {
            if (headings) {
                lines.add(Component.translatable(
                        shared.isEmpty() ? KEY_ON_HIT : KEY_ON_HIT_ONLY).withStyle(BODY));
            }
            emit(lines, hitOnly);
        }
    }

    private static void appendMechanics(AffixEffects.OnHit onHit, List<Component> lines) {
        scopeOf(lines, "dash");
        onHit.dash().ifPresent(dash -> lines.add(Component
                .translatable(dash.towardTarget()
                        ? KEY_DASH_TARGET
                        : (dash.forward() < 0.0F ? KEY_DASH_BACK : KEY_DASH))
                .withStyle(COND)));

        scopeOf(lines, "execute");
        onHit.execute().ifPresent(execute -> lines.add(execute.instantKill()
                ? Component.translatable(KEY_EXECUTE_KILL,
                        StyleText.formatDecimal(execute.healthThreshold() * 100.0D))
                        .withStyle(BAD)
                : Component.translatable(KEY_EXECUTE,
                        StyleText.formatDecimal(execute.bonusRatio() * 100.0D),
                        StyleText.formatDecimal(execute.healthThreshold() * 100.0D))
                        .withStyle(BAD)));

        onHit.execute().filter(AffixEffects.Execute::repeatOnKill).ifPresent(execute ->
                lines.add(Component.translatable(KEY_REPEAT_ON_KILL)
                        .withStyle(HEADER)));

        scopeOf(lines, "refundSlot");
        if (onHit.condition().refundSlot()) {
            lines.add(Component.translatable(KEY_REFUND_SLOT).withStyle(HEADER));
        }

        scopeOf(lines, "chain");
        onHit.chain().ifPresent(chain -> lines.add(withSchool(Component.translatable(KEY_CHAIN,
                        chain.maxTargets(), StyleText.formatDecimal(chain.ratio() * 100.0D))
                .withStyle(COND), onHit, chain)));

        scopeOf(lines, "momentum");
        onHit.momentum().ifPresent(momentum -> lines.add(Component.translatable(KEY_MOMENTUM,
                        StyleText.formatDecimal(momentum.perHitRatio()
                                * (momentum.maxStacks() - 1) * 100.0D))
                .withStyle(HEADER)));

        scopeOf(lines, "heal");
        onHit.heal().ifPresent(heal -> lines.add(heal.overheal()
                ? Component.translatable(KEY_HEAL_ABSORB,
                        StyleText.formatDecimal(heal.ratio() * 100.0D))
                        .withStyle(GOOD)
                : Component.translatable(KEY_HEAL,
                        StyleText.formatDecimal(heal.flat() + heal.ratio()))
                        .withStyle(GOOD)));

        scopeOf(lines, "disarm");
        onHit.disarm().ifPresent(disarm -> lines.add(Component.translatable(KEY_DISARM,
                        StyleText.formatDecimal(disarm.chance() * 100.0D))
                .withStyle(BODY)));

        scopeOf(lines, "vault");
        onHit.vault().ifPresent(vault -> lines.add(Component.translatable(KEY_VAULT)
                .withStyle(BODY)));

        scopeOf(lines, "blink");
        onHit.blink().ifPresent(blink -> lines.add(Component.translatable(
                        blink.distance() < 0.0F ? KEY_BLINK_BACK
                                : blink.behindTarget() ? KEY_BLINK_BEHIND : KEY_BLINK,
                        StyleText.formatDecimal(Math.abs(blink.distance())))
                .withStyle(COND)));

        scopeOf(lines, "shield");
        onHit.shield().ifPresent(shield -> lines.add(Component.translatable(KEY_SHIELD,
                        StyleText.formatDecimal(shield.amount()),
                        StyleText.formatDecimal(shield.duration() / (double) TICKS_PER_SECOND))
                .withStyle(GOOD)));

        scopeOf(lines, "cleave");
        onHit.cleave().ifPresent(cleave -> lines.add(withSchool(Component.translatable(KEY_CLEAVE,
                        cleave.maxTargets(),
                        StyleText.formatDecimal(cleave.ratio() * 100.0D) + "%")
                .withStyle(HEADER), onHit, cleave)));

        appendAttributes(onHit, lines);
        double amplifyRatio = onHit.allDamage().stream()
                .filter(AffixEffects.Damage::amplifies)
                .mapToDouble(AffixEffects.Damage::ratio)
                .findFirst()
                .orElse(1.0D);
        appendCondition(onHit.condition(), amplifyRatio, lines);
    }

    private static void appendAttributes(AffixEffects.OnHit onHit, List<Component> lines) {
        int attributesAt = 0;
        for (AffixEffects.AttributeBonus bonus : onHit.attributes()) {
            scopeOf(lines, "attributes", attributesAt++);
            String amount = bonus.operation().startsWith("multiply")
                    ? StyleText.formatDecimal(bonus.amount() * 100.0D) + "%"
                    : StyleText.formatDecimal(bonus.amount());
            if (bonus.amount() > 0.0D) {
                amount = "+" + amount;
            }

            MutableComponent line = Component.translatable(
                    bonus.onTarget() ? KEY_ATTRIBUTE_TARGET : KEY_ATTRIBUTE_SELF,
                    amount,
                    StyleText.registryName(bonus.attribute()),
                    StyleText.formatDecimal(bonus.duration() / (double) TICKS_PER_SECOND));

            if (bonus.maxStacks() > 1) {
                line.append(Component.literal(" "))
                        .append(Component.translatable(KEY_ATTRIBUTE_STACKS, bonus.maxStacks()));
            }
            lines.add(line.withStyle(bonus.amount() >= 0.0D
                    ? GOOD
                    : BAD));
        }

        int attackAttributesAt = 0;
        for (AffixEffects.AttackAttributeBonus bonus : onHit.attackAttributes()) {
            scopeOf(lines, "attackAttributes", attackAttributesAt++);
            if (bonus.amount() == 0.0D) {
                continue;
            }
            String amount = bonus.operation().startsWith("multiply")
                    ? StyleText.formatDecimal(bonus.amount() * 100.0D) + "%"
                    : StyleText.formatDecimal(bonus.amount());
            if (bonus.amount() > 0.0D) {
                amount = "+" + amount;
            }
            lines.add(Component.translatable(
                            bonus.stacks() ? KEY_ATTACK_ATTRIBUTE : KEY_ATTACK_ATTRIBUTE_ONCE,
                            amount, StyleText.registryName(bonus.attribute()))
                    .withStyle(bonus.amount() >= 0.0D
                            ? GOOD
                            : BAD));
        }

        if (!onHit.commands().isEmpty()) {
            lines.add(Component.translatable(KEY_COMMANDS, onHit.commands().size())
                    .withStyle(FRAME));
        }

        scopeOf(lines, "echo");
        onHit.echo().ifPresent(echo -> {
            if (echo.times() > 0) {
                lines.add(Component.translatable(KEY_ECHO, echo.times(),
                                StyleText.formatDecimal(echo.delayTicks() / (double) TICKS_PER_SECOND),
                                StyleText.formatDecimal(echo.ratio() * 100.0D))
                        .withStyle(FRAME));
            }
        });

        scopeOf(lines, "cooldown");
        onHit.cooldown().ifPresent(cooldown -> {
            if (cooldown.ticks() > 0) {
                lines.add(Component.translatable(KEY_COOLDOWN,
                                StyleText.formatDecimal(cooldown.ticks() / (double) TICKS_PER_SECOND))
                        .withStyle(FRAME));
            }
        });
        scopeOf(lines, "capsTargets");
        if (onHit.mechanics().capsTargets()) {
            lines.add(Component.translatable(KEY_TARGET_CAP,
                            onHit.mechanics().maxTargetsPerSwing())
                    .withStyle(FRAME));
        }

        scopeOf(lines, "cost");
        onHit.cost().ifPresent(cost -> {
            List<Component> costs = new ArrayList<>();
            if (cost.health() > 0.0F) {
                costs.add(Component.translatable(KEY_COST_HEALTH,
                        StyleText.formatDecimal(cost.health())).withStyle(BAD));
            }
            if (cost.hunger() > 0) {
                costs.add(Component.translatable(KEY_COST_HUNGER, cost.hunger())
                        .withStyle(BAD));
            }
            if (cost.experienceLevels() > 0) {
                costs.add(Component.translatable(KEY_COST_LEVELS, cost.experienceLevels())
                        .withStyle(BAD));
            }
            Component mergedCosts = mergeGates(costs);
            boolean noted = cost.health() > 0.0F || cost.experienceLevels() > 0;
            if (mergedCosts != null) {
                Component styled = mergedCosts.copy().withStyle(BAD);
                lines.add(noted && !ItemTooltip.detailed() ? ItemTooltip.marked(styled) : styled);
            }

            if (noted && ItemTooltip.detailed()) {
                lines.add(TooltipGem.detail(Component.translatable(cost.cancelIfShort()
                                ? KEY_COST_CANCELS : KEY_COST_ANYWAY)
                        .withStyle(FRAME)));
            }
        });

        int summonsAt = 0;
        for (AffixEffects.Summon summon : onHit.summons()) {
            scopeOf(lines, "summons", summonsAt++);
            lines.add((summon.count() > 1
                            ? Component.translatable(KEY_SUMMON_COUNT, summon.count(),
                                    StyleText.registryName(summon.entity()))
                            : Component.translatable(KEY_SUMMON, summon.count(),
                                    StyleText.registryName(summon.entity())))
                    .withStyle(COND));
            DamageSpec summonSpec = summon.damageSpec().orElse(null);
            if (summonSpec != null || summon.damageRatio() > 0.0F) {
                lines.add(TooltipGem.detail(Component.translatable(KEY_SUMMON_DAMAGE,
                                specAmount(summonSpec, summon.damageRatio()))
                        .withStyle(COND)));
            }
        }

        scopeOf(lines, "raiseDead");
        onHit.raiseDead().ifPresent(raise -> appendRaise(raise, lines));
    }

    private static void appendRaise(RaiseRules raise, List<Component> lines) {
        lines.add(Component.translatable(KEY_RAISE).withStyle(COND));
        appendFilter(raise.filter(), lines);
        if (raise.profile() != RaiseRules.DEFAULT.profile()) {
            lines.add(TooltipGem.detail(Component.translatable(KEY_RAISE_PROFILE
                    + raise.profile().getSerializedName()).withStyle(FRAME)));
        }
    }

    public static void appendFilter(MobFilter filter, List<Component> lines) {
        List<String> only = new ArrayList<>(filter.entities());
        filter.entityTags().forEach(tag -> only.add(TAG_MARK + tag));
        if (!only.isEmpty()) {
            lines.add(TooltipGem.detail(Component.translatable(KEY_RAISE_ONLY, mobNames(only))
                    .withStyle(FRAME)));
        }
        List<String> never = new ArrayList<>(filter.exemptEntities());
        filter.exemptTags().forEach(tag -> never.add(TAG_MARK + tag));
        if (!never.isEmpty()) {
            lines.add(TooltipGem.detail(Component.translatable(KEY_RAISE_NEVER, mobNames(never))
                    .withStyle(FRAME)));
        }
    }

    private static Component mobNames(List<String> ids) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", "));
            }
            String id = ids.get(index);
            joined.append(id.startsWith(TAG_MARK) ? Component.literal(id) : StyleText.registryName(id));
        }
        return joined;
    }

    private static String abilitySchool(AffixEffects.Order order) {
        String overridden = order.tuning()
                .map(AbilityTuning::damage)
                .flatMap(damage -> damage == null ? Optional.empty() : damage.school())
                .orElse(null);
        if (overridden != null) {
            return overridden;
        }
        AbilityConfig.Entry tuned = AbilityConfig.get(order.ability());
        return tuned == null ? null : tuned.school();
    }

    private static void appendSlotRule(AffixEffects.SlotRule rule, String multiplier,
                                       double ratio, List<Component> lines) {
        if (rule == null || rule.isEmpty()) {
            return;
        }

        rule.first().ifPresent(value -> lines.add(slotLine(multiplier,
                value ? KEY_SLOT_FIRST : KEY_SLOT_NOT_FIRST,
                value ? KEY_SLOT_FIRST_PLAIN : KEY_SLOT_NOT_FIRST_PLAIN)));

        rule.last().ifPresent(value -> lines.add(slotLine(multiplier,
                value ? KEY_SLOT_LAST : KEY_SLOT_NOT_LAST,
                value ? KEY_SLOT_LAST_PLAIN : KEY_SLOT_NOT_LAST_PLAIN)));

        if (!rule.slots().isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (int slot : rule.slots()) {
                labels.add(slot < 0
                        ? Component.translatable(KEY_SLOT_FROM_END, -slot).getString()
                        : String.valueOf(slot + 1));
            }
            String slots = String.join(", ", labels);
            lines.add(isPlain(multiplier)
                    ? gold(Component.translatable(KEY_SLOT_AT, slots))
                    : gold(Component.translatable(KEY_SLOT_AT_BONUS, slots, multiplier)));
        }

        rule.afterStyle().ifPresent(styleId -> {
            StylePage page = StyleRegistry.get(styleId);
            String name = page == null ? styleId : StyleText.name(page).getString();
            lines.add(isPlain(multiplier)
                    ? gold(Component.translatable(KEY_SLOT_AFTER_PLAIN, name))
                    : gold(Component.translatable(KEY_SLOT_AFTER, name, multiplier)));
        });

        rule.afterCategory().ifPresent(category -> lines.add(isPlain(multiplier)
                ? gold(Component.translatable(KEY_SLOT_AFTER_CATEGORY_PLAIN, category))
                : gold(Component.translatable(KEY_SLOT_AFTER_CATEGORY, category, multiplier))));

        appendRequires(rule.requires(), ratio, lines);
    }

    private static boolean isPlain(String multiplier) {
        return multiplier.equals(StyleText.formatDecimal(1.0D));
    }

    private static Component healthLine(String multiplier, double value,
                                        String scaledKey, String plainKey) {
        String percent = StyleText.formatDecimal(value * 100.0D);
        return isPlain(multiplier)
                ? Component.translatable(plainKey, percent).withStyle(COND)
                : Component.translatable(scaledKey, percent, multiplier)
                        .withStyle(COND);
    }

    private static Component slotLine(String multiplier, String scaledKey, String plainKey) {
        return isPlain(multiplier)
                ? gold(Component.translatable(plainKey))
                : gold(Component.translatable(scaledKey, multiplier));
    }

    private static Component gold(MutableComponent component) {
        return component.withStyle(HEADER);
    }

    private static void appendRequires(AffixEffects.Requires requires, double ratio,
                                       List<Component> lines) {
        if (requires == null || requires.isEmpty()) {
            return;
        }
        if (requires.minAttacks() > 0) {
            lines.add(Component.translatable(KEY_REQUIRES_ATTACKS, requires.minAttacks())
                    .withStyle(HEADER));
        }
        if (requires.perPreceding() > 0.0F) {
            String per = StyleText.formatDecimal(requires.perPreceding() * ratio * 100.0D) + "%";
            if (requires.perPrecedingCap() > 0.0F) {
                lines.add(Component.translatable(KEY_PER_PRECEDING_CAPPED, per,
                                StyleText.formatDecimal(requires.perPrecedingCap() * ratio * 100.0D) + "%")
                        .withStyle(HEADER));
            } else {
                lines.add(Component.translatable(KEY_PER_PRECEDING, per)
                        .withStyle(HEADER));
            }
        }
        requires.sameTarget().filter(Boolean::booleanValue).ifPresent(value ->
                lines.add(Component.translatable(KEY_SAME_TARGET).withStyle(HEADER)));
    }

    private static void appendCondition(AffixEffects.Condition condition, double amplifyRatio,
                                        List<Component> lines) {
        if (condition == null || condition.isEmpty()) {
            return;
        }
        String multiplier = StyleText.formatDecimal(condition.multiplier());
        appendSlotRule(condition.slot(), multiplier, amplifyRatio, lines);

        List<Component> gates = new ArrayList<>();
        List<Component> scaled = isPlain(multiplier) ? gates : lines;

        condition.selfHealthBelow().ifPresent(value -> scaled.add(healthLine(
                multiplier, value, KEY_CONDITION_SELF_LOW, KEY_ONLY_SELF_LOW)));
        condition.selfHealthAbove().ifPresent(value -> scaled.add(healthLine(
                multiplier, value, KEY_CONDITION_SELF_HIGH, KEY_ONLY_SELF_HIGH)));
        condition.targetHealthBelow().ifPresent(value -> scaled.add(healthLine(
                multiplier, value, KEY_CONDITION_TARGET_LOW, KEY_ONLY_TARGET_LOW)));
        condition.targetHealthAbove().ifPresent(value -> scaled.add(healthLine(
                multiplier, value, KEY_CONDITION_TARGET_HIGH, KEY_ONLY_TARGET_HIGH)));
        condition.state().behindTarget().ifPresent(value -> scaled.add(stateLine(multiplier,
                KEY_CONDITION_BEHIND, KEY_ONLY_BEHIND)));
        condition.state().airborne().ifPresent(value -> scaled.add(stateLine(multiplier,
                KEY_CONDITION_AIRBORNE, KEY_ONLY_AIRBORNE)));
        condition.state().sprinting().ifPresent(value -> scaled.add(stateLine(multiplier,
                KEY_CONDITION_SPRINTING, KEY_ONLY_SPRINTING)));
        condition.state().crouching().ifPresent(value -> scaled.add(stateLine(multiplier,
                KEY_CONDITION_CROUCHING, KEY_ONLY_CROUCHING)));
        condition.state().night().ifPresent(value -> scaled.add(stateLine(multiplier,
                KEY_CONDITION_NIGHT, KEY_ONLY_NIGHT)));
        condition.state().damagedWithin().ifPresent(ticks -> gates.add(Component.translatable(
                        KEY_ONLY_AFTER_HURT, StyleText.formatDecimal(ticks / (double) TICKS_PER_SECOND))
                .withStyle(COND)));
        if (!condition.damageType().isEmpty()) {
            gates.add(Component.translatable(KEY_ONLY_DAMAGE_TYPE,
                            damageTypeNames(condition.damageType()))
                    .withStyle(COND));
        }
        if (!condition.takenDamageType().isEmpty()) {
            gates.add(Component.translatable(KEY_ONLY_TAKEN_DAMAGE_TYPE,
                            damageTypeNames(condition.takenDamageType()))
                    .withStyle(COND));
        }
        condition.onMiss().filter(Boolean::booleanValue).ifPresent(value ->
                gates.add(Component.translatable(KEY_ONLY_ON_MISS)
                        .withStyle(COND)));
        if (!condition.targetEffects().isEmpty()) {
            gates.add(Component.translatable(KEY_ONLY_TARGET_EFFECTS,
                            effectNames(condition.targetEffects()))
                    .withStyle(COND));
        }
        condition.targetBurning().ifPresent(burning -> gates.add(Component.translatable(
                        burning ? KEY_ONLY_TARGET_BURNING : KEY_ONLY_TARGET_NOT_BURNING)
                .withStyle(COND)));
        for (Component part : ItemGateText.parts(condition.state())) {
            gates.add(Component.translatable(KEY_ONLY_STATE, part).withStyle(COND));
        }

        Component merged = mergeGates(gates);
        if (merged != null) {
            lines.add(merged);
        }
    }

    private static Component mergeGates(List<Component> gates) {
        if (gates.isEmpty()) {
            return null;
        }
        if (gates.size() == 1) {
            return gates.getFirst();
        }
        MutableComponent joined = Component.empty();
        for (int index = 0; index < gates.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", ").withStyle(FRAME));
            }
            joined.append(stripOnly(gates.get(index), index > 0));
        }
        return joined.withStyle(COND);
    }

    private static Component stripOnly(Component gate, boolean drop) {
        String text = gate.getString();
        String prefix = Component.translatable(KEY_ONLY_PREFIX).getString();
        if (!drop || prefix.isBlank() || !text.startsWith(prefix)) {
            return gate;
        }
        return Component.literal(text.substring(prefix.length())).withStyle(COND);
    }

    private static Component abilityName(String ability) {
        String key = AbilityConfig.key(ability);
        return Component.translatableWithFallback(
                "ability." + CombatStyles.MOD_ID + "." + key + ".name", StyleText.prettify(key));
    }

    private static Component effectNames(List<String> effects) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < effects.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(EFFECT_SEPARATOR));
            }
            joined.append(StyleText.registryName(effects.get(index)));
        }
        return joined;
    }

    public static String damageTypeNames(List<String> types) {
        Set<String> names = new LinkedHashSet<>();
        for (String type : types) {
            String trimmed = type == null ? "" : type.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean tag = trimmed.startsWith(TAG_MARK);
            String body = tag ? trimmed.substring(TAG_MARK.length()) : trimmed;
            names.add(tag
                    ? StyleText.damageTypeTagName(body).getString()
                    : StyleText.damageTypeName(body).getString());
        }
        return String.join(TYPE_SEPARATOR, names);
    }

    private static Component stateLine(String multiplier, String scaledKey, String gateKey) {
        return isPlain(multiplier)
                ? Component.translatable(gateKey).withStyle(COND)
                : Component.translatable(scaledKey, multiplier).withStyle(COND);
    }

    private static void appendTrigger(StyleAffix affix, AffixEffects.OnHit onHit, List<Component> lines) {
        int allDamageAt = 0;
        for (AffixEffects.Damage damage : onHit.allDamage()) {
            scopeOf(lines, "allDamage", allDamageAt++);
            String percent = scaledPercent(damage);

            String school = attunedSchool(damage);
            boolean typed = school != null || !damage.damageType().isBlank();
            boolean scaledOnly = !damage.converts() && damage.ratio() <= 0.0F
                    && damage.scaling().isPresent() && ItemTooltip.detailed();
            String damageKey = damage.converts()
                    ? KEY_CONVERTS
                    : damage.amplifies()
                            ? KEY_AMPLIFIES
                            : (typed ? KEY_BONUS_SCHOOL : KEY_BONUS);
            Component line = scaledOnly
                    ? scaledBonusLine(damage.scaling().get(), typed
                            ? dealtAs(damage, school)
                            : StyleText.schoolName(null))
                    : damage.converts() || (!damage.amplifies() && typed)
                            ? Component.translatable(damageKey, percent, dealtAs(damage, school))
                            : Component.translatable(damageKey, percent);
            if (!ItemTooltip.detailed() && damage.multiplier() != AffixEffects.Damage.NO_MULTIPLIER) {
                line = Component.translatable(KEY_WITH_MULTIPLIER, line,
                        StyleText.formatDecimal(damage.multiplier()));
            }
            Component styled = line.copy().withStyle(damage.converts() ? COND : BAD);
            boolean hasDetail = damage.ignoresArmour() || damage.scaling().isPresent()
                    || damage.multiplier() != AffixEffects.Damage.NO_MULTIPLIER;
            if (!ItemTooltip.detailed()) {
                lines.add(hasDetail ? ItemTooltip.marked(styled) : styled);
                continue;
            }
            lines.add(styled);
            if (damage.ignoresArmour()) {
                lines.add(TooltipGem.detail(Component.translatable(KEY_ARMOUR).withStyle(FRAME)));
            }
            if (!scaledOnly) {
                damage.scaling().ifPresent(scaling -> lines.add(TooltipGem.detail(scalingLine(scaling))));
            }
            Component multiplied = multiplierLine(onHit, allDamageAt - 1, damage,
                    typed ? dealtAs(damage, school) : StyleText.schoolName(null));
            if (multiplied != null) {
                lines.add(TooltipGem.detail(multiplied.copy().withStyle(COND)));
            }
        }

        scopeOf(lines, "ignite");
        onHit.ignite().ifPresent(ignite -> lines.add(withChance(
                Component.translatable(KEY_IGNITE, ignite.seconds()).withStyle(HEADER),
                ignite.chance())));

        scopeOf(lines, "freezeTicks");
        if (onHit.freezeTicks() > 0.0F) {
            lines.add(Component.translatable(KEY_FREEZE,
                            StyleText.formatDecimal(onHit.freezeTicks() / TICKS_PER_SECOND))
                    .withStyle(COND));
        }

        int statusEffectsAt = 0;
        for (AffixEffects.StatusEffect effect : onHit.statusEffects()) {
            scopeOf(lines, "statusEffects", statusEffectsAt++);
            lines.add(withChance(Component.translatable(KEY_EFFECT,
                            StyleText.registryName(effect.effect()),
                            effect.amplifier() + 1,
                            effect.duration() / TICKS_PER_SECOND)
                    .withStyle(GOOD), effect.chance()));
        }

        scopeOf(lines, "lifesteal");
        onHit.lifesteal().ifPresent(lifesteal -> lines.add(withChance(
                Component.translatable(KEY_LIFESTEAL,
                                StyleText.formatDecimal(lifesteal.ratio() * 100.0D) + "%")
                        .withStyle(BAD), lifesteal.chance())));

        scopeOf(lines, "knockback");
        onHit.knockback().ifPresent(knockback -> lines.add(
                Component.translatable(knockback.pull() ? KEY_PULL : KEY_KNOCKBACK,
                                StyleText.formatDecimal(knockback.strength()))
                        .withStyle(GOOD)));

        onHit.mechanics().control().hasten().ifPresent(hasten -> lines.add(
                Component.translatable(KEY_HASTEN,
                                StyleText.formatDecimal(hasten.ratio() * 100.0D))
                        .withStyle(COND)));

        AffixEffects.Order order = onHit.mechanics().order();
        order.quicken().ifPresent(quicken -> lines.add(Component.translatable(
                        quicken.chance() < 1.0F ? KEY_QUICKEN_CHANCE : KEY_QUICKEN,
                        StyleText.formatDecimal(quicken.ratio() * 100.0D),
                        StyleText.formatDecimal(quicken.chance() * 100.0D))
                .withStyle(COND)));
        if (order.ordersAbility()) {
            lines.add(Component.translatable(KEY_ORDERS,
                            abilityName(order.ability()),
                            StyleText.formatDecimal(order.damageRatio() * 100.0D) + "%",
                            StyleText.schoolName(abilitySchool(order)))
                    .withStyle(COND));
            if (order.targets() != SummonTargeting.ALL) {
                lines.add(TooltipGem.detail(Component.translatable(KEY_ORDERS_TARGETS,
                                Component.translatable(KEY_TARGETING + order.targets().key()))
                        .withStyle(COND)));
            }
        }

        appendMechanics(onHit, lines);

        scopeOf(lines, "slashWave");
        onHit.slashWave().ifPresent(wave -> lines.add(withSchool(
                Component.translatable(KEY_SLASH_WAVE,
                        StyleText.formatDecimal(wave.range()),
                        specAmount(wave.damageSpec().orElse(null), wave.ratio()))
                .withStyle(COND), onHit, wave)));

        int projectilesAt = 0;
        for (AffixEffects.Projectile projectile : onHit.projectiles()) {
            scopeOf(lines, "projectiles", projectilesAt++);
            Component fired = firedName(projectile);
            lines.add((projectile.count() > 1
                            ? Component.translatable(KEY_PROJECTILE_COUNT,
                                    projectile.count(), fired)
                            : Component.translatable(KEY_PROJECTILE, projectile.count(), fired))
                    .withStyle(COND));
            DamageSpec spec = projectile.damageSpec().orElse(null);
            if (spec != null || projectile.damage() > 0.0F) {
                lines.add(TooltipGem.detail(Component.translatable(KEY_PROJECTILE_DAMAGE,
                                spec == null
                                        ? Component.translatable(KEY_SPEC_FLAT,
                                                StyleText.formatDecimal(projectile.damage()))
                                        : specAmount(spec, 0.0F))
                        .withStyle(COND)));
            }
            for (Component custom : Customs.tooltip(CustomScopes.PROJECTILE, projectile)) {
                lines.add(TooltipGem.detail(custom.copy().withStyle(COND)));
            }
        }

        int castsAt = 0;
        for (CastSpell cast : SpellEngineCompat.isLoaded() ? onHit.castSpells() : List.<CastSpell>of()) {
            scopeOf(lines, "castSpells", castsAt++);
            Component spell = cast.name().isBlank()
                    ? StyleText.spellName(cast.spell())
                    : Component.literal(cast.name());
            lines.add(Component.translatable(cast.target() == CastSpell.Target.SELF
                            ? KEY_CAST_SPELL_SELF
                            : KEY_CAST_SPELL, spell)
                    .withStyle(COND));
            cast.damageSpec().ifPresent(spec -> lines.add(TooltipGem.detail(Component.translatable(
                            KEY_CAST_SPELL_DAMAGE, specAmount(spec, 0.0F))
                    .withStyle(COND))));
        }

        scopeOf(lines, "splash");
        onHit.splash().ifPresent(splash -> lines.add(withSchool(Component.translatable(KEY_SPLASH,
                        StyleText.formatDecimal(splash.radius()),
                        StyleText.formatDecimal(splash.ratio() * 100.0D) + "%")
                .withStyle(HEADER), onHit, splash)));

        int selfEffectsAt = 0;
        for (AffixEffects.SelfEffect effect : onHit.selfEffects()) {
            scopeOf(lines, "selfEffects", selfEffectsAt++);
            lines.add(Component.translatable(KEY_SELF,
                            StyleText.registryName(effect.effect()),
                            effect.amplifier() + 1,
                            effect.duration() / TICKS_PER_SECOND)
                    .withStyle(COND));
        }
    }

    public static Component specText(DamageSpec spec) {
        return specAmount(spec, DamageSpec.NO_HIT_RATIO);
    }

    private static Component specAmount(DamageSpec spec, float fallbackRatio) {
        if (ItemTooltip.detailed() || spec == null) {
            return specFormula(spec, fallbackRatio);
        }
        Player viewer = TooltipContextAccess.viewer();
        if (viewer == null) {
            return specFormula(spec, fallbackRatio);
        }
        float resolved = spec.amountFor(viewer, 0.0F, null);
        MutableComponent line = resolved > 0.0F
                ? Component.translatable(KEY_SPEC_FLAT, StyleText.formatDecimal(resolved)).copy()
                : null;
        MutableComponent runtime = runtimeTerms(spec, fallbackRatio);
        MutableComponent shown = runtime == null
                ? line
                : (line == null ? runtime : line.append(Component.literal(" ")).append(runtime));
        if (shown == null) {
            return specFormula(spec, fallbackRatio);
        }
        shown = withSpecSchool(shown, spec);
        return hasHiddenTerms(spec) ? ItemTooltip.marked(shown) : shown;
    }

    private static MutableComponent withSpecSchool(MutableComponent line, DamageSpec spec) {
        Component named = spec.school().isPresent()
                ? StyleText.schoolName(spec.school().get())
                : spec.damageType().filter(type -> !type.isBlank()).map(StyleText::damageTypeName).orElse(null);
        if (named == null) {
            return line;
        }
        return line.append(Component.literal(" "))
                .append(Component.translatable(KEY_AS_SCHOOL, named));
    }

    private static boolean hasHiddenTerms(DamageSpec spec) {
        return !spec.scaling().isEmpty()
                || spec.multiplier() != DamageSpec.FULL_MULTIPLIER
                || spec.ofSelfMissingHealth() != DamageSpec.NO_HEALTH_RATIO
                || (spec.base() != DamageSpec.NO_BASE
                        && spec.ofHit() != DamageSpec.NO_HIT_RATIO);
    }

    private static MutableComponent runtimeTerms(DamageSpec spec, float fallbackRatio) {
        MutableComponent terms = null;
        float ratio = spec.ofHit() != DamageSpec.NO_HIT_RATIO ? spec.ofHit() : fallbackRatio;
        if (ratio != DamageSpec.NO_HIT_RATIO) {
            terms = Component.translatable(KEY_SPEC_OF_HIT,
                    StyleText.formatDecimal(ratio * 100.0D)).copy();
        }
        if (spec.ofTargetMaxHealth() != DamageSpec.NO_HEALTH_RATIO) {
            terms = joinTerms(terms, Component.translatable(KEY_SPEC_MAX_HEALTH,
                    StyleText.formatDecimal(spec.ofTargetMaxHealth() * 100.0D)));
        }
        if (spec.ofTargetMissingHealth() != DamageSpec.NO_HEALTH_RATIO) {
            terms = joinTerms(terms, Component.translatable(KEY_SPEC_MISSING_HEALTH,
                    StyleText.formatDecimal(spec.ofTargetMissingHealth() * 100.0D)));
        }
        return terms;
    }

    private static MutableComponent joinTerms(MutableComponent terms, Component next) {
        return terms == null
                ? next.copy()
                : terms.append(Component.literal(" ")).append(next);
    }

    private static Component specFormula(DamageSpec spec, float fallbackRatio) {
        if (spec == null) {
            return Component.translatable(KEY_SPEC_RATIO,
                    StyleText.formatDecimal(fallbackRatio * 100.0D));
        }
        MutableComponent line = null;
        if (spec.base() != DamageSpec.NO_BASE) {
            line = Component.translatable(KEY_SPEC_FLAT,
                    StyleText.formatDecimal(spec.base()));
        }
        if (spec.ofHit() != DamageSpec.NO_HIT_RATIO) {
            line = join(line, Component.translatable(KEY_SPEC_RATIO,
                    StyleText.formatDecimal(spec.ofHit() * 100.0D)));
        }
        if (spec.ofTargetMaxHealth() != DamageSpec.NO_HEALTH_RATIO) {
            line = join(line, Component.translatable(KEY_SPEC_MAX_HEALTH,
                    StyleText.formatDecimal(spec.ofTargetMaxHealth() * 100.0D)));
        }
        if (spec.ofTargetMissingHealth() != DamageSpec.NO_HEALTH_RATIO) {
            line = join(line, Component.translatable(KEY_SPEC_MISSING_HEALTH,
                    StyleText.formatDecimal(spec.ofTargetMissingHealth() * 100.0D)));
        }
        if (spec.ofSelfMissingHealth() != DamageSpec.NO_HEALTH_RATIO) {
            line = join(line, Component.translatable(KEY_SPEC_SELF_MISSING,
                    StyleText.formatDecimal(spec.ofSelfMissingHealth() * 100.0D)));
        }
        if (line == null && spec.scaling().isEmpty()) {
            line = Component.translatable(KEY_SPEC_RATIO,
                    StyleText.formatDecimal(fallbackRatio * 100.0D));
        }
        if (!spec.scaling().isEmpty()) {
            line = join(line, scalingDetail(spec));
        }
        if (spec.multiplier() != DamageSpec.FULL_MULTIPLIER) {
            line = Component.translatable(KEY_SPEC_MULTIPLIED, line,
                    StyleText.formatMultiplier(spec.multiplier()));
        }
        return withSpecSchool(line, spec);
    }

    private static Component scalingDetail(DamageSpec spec) {
        List<DamageSpec.ScalingEntry> entries = spec.scaling();
        if (entries.size() > 1 && spec.scalingMode() == DamageSpec.ScalingMode.STRONGEST
                && sharesOneRate(entries)) {
            return Component.translatable(KEY_SPEC_STRONGEST, scalingEntry(entries.getFirst(),
                    groupedSources(entries)));
        }
        MutableComponent joined = null;
        for (DamageSpec.ScalingEntry entry : entries) {
            joined = joined == null
                    ? scalingEntry(entry).copy()
                    : Component.translatable(KEY_SPEC_AND, joined, scalingEntry(entry));
        }
        if (joined == null) {
            return Component.translatable(KEY_SPEC_SCALED);
        }
        return entries.size() > 1 && spec.scalingMode() == DamageSpec.ScalingMode.STRONGEST
                ? Component.translatable(KEY_SPEC_STRONGEST, joined)
                : joined;
    }

    private static boolean sharesOneRate(List<DamageSpec.ScalingEntry> entries) {
        DamageSpec.ScalingEntry first = entries.getFirst();
        for (DamageSpec.ScalingEntry entry : entries) {
            if (entry.mode() != first.mode() || entry.perPoint() != first.perPoint()
                    || !entry.cap().equals(first.cap()) || entry.baseline() != first.baseline()) {
                return false;
            }
        }
        return true;
    }

    private static Component groupedSources(List<DamageSpec.ScalingEntry> entries) {
        List<Component> names = new ArrayList<>();
        int schools = 0;
        for (DamageSpec.ScalingEntry entry : entries) {
            if (entry.source().startsWith(SPELL_POWER_PREFIX)) {
                schools++;
            }
        }
        boolean grouped = schools >= GROUPED_SCHOOLS_MIN;
        boolean groupAdded = false;
        for (DamageSpec.ScalingEntry entry : entries) {
            if (grouped && entry.source().startsWith(SPELL_POWER_PREFIX)) {
                if (!groupAdded) {
                    names.add(Component.translatable(KEY_ANY_SPELL_POWER));
                    groupAdded = true;
                }
                continue;
            }
            names.add(StyleText.scalingSourceName(entry.source()));
        }
        MutableComponent joined = names.getFirst().copy();
        for (Component name : names.subList(1, names.size())) {
            joined = Component.translatable(KEY_SPEC_OR, joined, name);
        }
        return joined;
    }

    private static Component scalingEntry(DamageSpec.ScalingEntry entry) {
        return scalingEntry(entry, StyleText.scalingSourceName(entry.source()));
    }

    private static Component scalingEntry(DamageSpec.ScalingEntry entry, Component source) {
        Component detail = switch (entry.mode()) {
            case MULTIPLIER -> Component.translatable(KEY_SPEC_SCALE_MULTIPLIER,
                    StyleText.formatDecimal(entry.perPoint() * 100.0D), source);
            case DIVISOR -> Component.translatable(KEY_SPEC_SCALE_DIVISOR,
                    source, StyleText.formatDecimal(entry.baseline()));
            default -> Component.translatable(KEY_SPEC_SCALE_PER_POINT,
                    StyleText.formatDecimal(entry.perPoint()), source);
        };
        return entry.cap()
                .map(limit -> (Component) Component.translatable(KEY_SPEC_SCALE_CAP,
                        detail, StyleText.formatDecimal(limit)))
                .orElse(detail);
    }

    private static MutableComponent join(MutableComponent line, Component next) {
        return line == null
                ? next.copy()
                : Component.translatable(KEY_SPEC_AND, line, next);
    }

    private static Component withSchool(Component base, AffixEffects.OnHit onHit,
                                        AffixEffects.Typed spread) {
        String school = spread.school().orElse(null);
        if (school == null) {
            school = spread.damageType().orElse(null);
        }
        if (school == null && spread.inherit()) {
            school = inheritedSchool(onHit);
        }
        return base.copy().append(Component.literal(" "))
                .append(Component.translatable(KEY_AS_SCHOOL, StyleText.schoolName(school))
                        .withStyle(FRAME));
    }

    private static String inheritedSchool(AffixEffects.OnHit onHit) {
        for (AffixEffects.Damage damage : onHit.allDamage()) {
            if (damage.amplifies()) {
                continue;
            }
            if (damage.school().isPresent()) {
                return damage.school().get();
            }
            if (!damage.damageType().isEmpty()) {
                return damage.damageType();
            }
        }
        return null;
    }

    public static Component scalingLine(AffixEffects.Scaling scaling) {
        String perPoint = StyleText.formatDecimal(scaling.perPoint() * 100.0D) + "%";
        if (scaling.strongest() && scaling.sources().size() > 1) {
            return Component.translatable(KEY_ATTUNED, perPoint).withStyle(COND);
        }
        if (scaling.sources().isEmpty()) {
            return Component.translatable(KEY_SCALES, perPoint).withStyle(COND);
        }
        MutableComponent schools = Component.empty();
        for (int index = 0; index < scaling.sources().size(); index++) {
            if (index > 0) {
                schools.append(Component.literal(", "));
            }
            schools.append(StyleText.scalingSourceName(scaling.sources().get(index)));
        }
        return Component.translatable(KEY_SCALES_SCHOOL, perPoint, schools).withStyle(COND);
    }

    private static Component multiplierLine(AffixEffects.OnHit onHit, int index,
                                            AffixEffects.Damage damage, Component dealt) {
        boolean single = onHit.damage().isPresent();
        String entry = single && index == 0
                ? KEY_DAMAGE_FIELD
                : KEY_DAMAGES_FIELD + PATH_JOIN + (single ? index - 1 : index);
        Optional<ScaledNumber> scaled = ScaledNumbers.at(onHit,
                entry + PATH_JOIN + AffixEffects.Damage.KEY_MULTIPLIER);
        if (scaled.isPresent()) {
            AffixEffects.Scaling scaling = scalingOf(scaled.get());
            return scaling == null ? null : scaledBonusLine(scaling, dealt);
        }
        if (damage.multiplier() == AffixEffects.Damage.NO_MULTIPLIER) {
            return null;
        }
        return Component.translatable(KEY_DAMAGE_MULTIPLIED, dealt,
                StyleText.formatDecimal(damage.multiplier()));
    }

    private static AffixEffects.Scaling scalingOf(ScaledNumber number) {
        if (number.by().isEmpty()) {
            return null;
        }
        DamageSpec.ScalingEntry first = number.by().getFirst();
        for (DamageSpec.ScalingEntry entry : number.by()) {
            if (entry.perPoint() != first.perPoint() || entry.mode() != DamageSpec.ScalingKind.PER_POINT) {
                return null;
            }
        }
        Optional<Float> cap = number.max().map(max -> max - number.base());
        return new AffixEffects.Scaling(number.sources(), first.perPoint(), cap,
                number.mode() == DamageSpec.ScalingMode.STRONGEST);
    }

    private static Component scaledBonusLine(AffixEffects.Scaling scaling, Component dealt) {
        String perPoint = StyleText.formatDecimal(scaling.perPoint() * 100.0D);
        Component sources;
        if (scaling.strongest() && scaling.sources().size() > 1) {
            sources = Component.translatable(KEY_STRONGEST_SCHOOL);
        } else {
            MutableComponent joined = Component.empty();
            for (int index = 0; index < scaling.sources().size(); index++) {
                if (index > 0) {
                    joined.append(Component.literal(", "));
                }
                joined.append(StyleText.scalingSourceName(scaling.sources().get(index)));
            }
            sources = joined;
        }
        Component line = Component.translatable(KEY_BONUS_SCALED, perPoint, dealt, sources);
        return scaling.cap()
                .map(limit -> (Component) Component.translatable(KEY_SPEC_SCALE_CAP, line,
                        StyleText.formatDecimal(limit * 100.0D) + PERCENT))
                .orElse(line);
    }

    private static Component dealtAs(AffixEffects.Damage damage, String school) {
        return school == null && !damage.damageType().isBlank()
                ? StyleText.damageTypeName(damage.damageType())
                : StyleText.schoolName(school);
    }

    private static String attunedSchool(AffixEffects.Damage damage) {
        String declared = damage.school().orElse(null);
        AffixEffects.Scaling scaling = damage.scaling().orElse(null);
        if (!damage.damageType().isBlank()) {
            return declared;
        }
        if (scaling == null || !scaling.strongest() || scaling.sources().size() < 2) {
            return declared;
        }
        Player viewer = TooltipContextAccess.viewer();
        if (viewer == null) {
            return declared;
        }
        String winner = SpellPowerAccess.strongestOf(viewer, scaling.sources());
        return winner == null ? declared : winner;
    }

    private static Component withChance(Component base, float chance) {
        if (chance >= 1.0F) {
            return base;
        }
        return base.copy().append(Component.literal(" "))
                .append(Component.translatable(KEY_CHANCE,
                                StyleText.formatDecimal(chance * 100.0D) + "%")
                        .withStyle(FRAME));
    }
}
