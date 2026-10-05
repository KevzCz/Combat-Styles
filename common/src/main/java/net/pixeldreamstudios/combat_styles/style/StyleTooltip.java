package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.item.TooltipGem;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StyleTooltip {
    private static final String KEY_DAMAGE = "tooltip.combat_styles.short_damage";
    private static final String KEY_COOLDOWN_SCALE = "tooltip.combat_styles.cooldown_scale";
    private static final String KEY_DAMAGE_TYPE = "tooltip.combat_styles.damage_type";
    private static final String KEY_HASTENS_NEXT = "tooltip.combat_styles.hastens_next";
    private static final String KEY_PLACEMENT_CURSOR = "tooltip.combat_styles.placement_cursor";
    private static final String KEY_DRAG_WINDOW = "tooltip.combat_styles.drag_window";
    private static final String KEY_DRAG_TARGETS = "tooltip.combat_styles.drag_targets";
    private static final String KEY_MOTION_DASH = "tooltip.combat_styles.motion_dash";
    private static final String KEY_MOTION_BLINK = "tooltip.combat_styles.motion_blink";
    private static final String KEY_MOTION_LIFT = "tooltip.combat_styles.motion_lift";
    private static final String KEY_REACH = "tooltip.combat_styles.short_reach";
    private static final String KEY_UPSWING = "tooltip.combat_styles.short_upswing";
    private static final String KEY_MOVE = "tooltip.combat_styles.short_move";
    private static final String KEY_SUBTITLE_COST = "tooltip.combat_styles.subtitle_cost";
    private static final String KEY_AFFIX_TOTAL = "tooltip.combat_styles.affix_cost_total";
    private static final String KEY_CATEGORY = "tooltip.combat_styles.category";
    private static final String KEY_HITBOX = "tooltip.combat_styles.hitbox";
    private static final String KEY_SOUND = "tooltip.combat_styles.sound";
    private static final String KEY_ANGLE = "tooltip.combat_styles.angle";
    private static final String KEY_REQUIRES = "tooltip.combat_styles.requires";
    private static final String KEY_EXPAND = "tooltip.combat_styles.expand_hint";
    private static final String KEY_INNATE = "tooltip.combat_styles.innate_affix";
    private static final String KEY_DEFAULT_AFFIXES = "tooltip.combat_styles.default_affixes";

    private static final String SEPARATOR = " · ";
    private static final int COLUMN_PAD = 2;

    private static final ChatFormatting LABEL = ChatFormatting.GRAY;
    private static final ChatFormatting DIM = ChatFormatting.DARK_GRAY;
    private static final ChatFormatting VALUE = ChatFormatting.WHITE;

    private StyleTooltip() {
    }

    private static final String KEY_ATTRIBUTES = "screen.combat_styles.section_attributes";

    public static Component subtitle(StylePage page, double cost) {
        MutableComponent line = StyleText.categoryName(page.category()).copy()
                .append(SEPARATOR)
                .append(Component.translatable(page.rarity().translationKey()));
        if (cost > 0.0D) {
            line.append(SEPARATOR).append(Component.translatable(KEY_SUBTITLE_COST,
                    StyleText.formatDecimal(cost)));
        }
        WeaponAttributes.Condition[] conditions = page.attack().conditions();
        Component named = StyleText.conditionNames(conditions);
        if (!named.getString().isEmpty()) {
            line.append(SEPARATOR).append(named.copy().withStyle(ChatFormatting.AQUA));
        }
        return line.withStyle(DIM);
    }

    public static void appendAffixes(List<StyleAffix> affixes, List<Component> tooltip) {
        appendAffixes(affixes, tooltip, true);
    }

    public static void appendAffixes(List<StyleAffix> affixes, List<Component> tooltip,
                                     boolean withCost) {
        if (affixes.isEmpty()) {
            return;
        }
        double total = StyleAffix.DEFAULT_COST_MODIFIER;
        for (StyleAffix affix : affixes) {
            appendAffix(affix, tooltip);
            total *= affix.costModifier();
        }
        if (withCost && total != StyleAffix.DEFAULT_COST_MODIFIER) {
            tooltip.add(Component.translatable(KEY_AFFIX_TOTAL,
                    StyleText.formatDecimal(total)).withStyle(ChatFormatting.GOLD));
        }
    }

    private static void appendAffix(StyleAffix affix, List<Component> tooltip) {
        List<Component> effects = new ArrayList<>();
        AffixText.appendEffects(affix, effects);
        tooltip.add(TooltipGem.of(affix.rarity()).bullet(
                AffixText.name(affix).copy().withStyle(affix.rarity().color())));
        for (Component effect : effects) {
            tooltip.add(TooltipGem.hang(effect));
        }
    }

    public static void appendDefaultAffixes(StylePage page, List<Component> tooltip) {
        if (page == null) {
            return;
        }
        AttackOverride base = AttackOverrides.get(page.styleId());
        if (base == null || base.affixes().isEmpty()) {
            return;
        }
        List<StyleAffix> resolved = new ArrayList<>();
        for (String id : base.affixes()) {
            StyleAffix affix = AffixRegistry.get(id);
            if (affix != null) {
                resolved.add(affix);
            }
        }
        if (resolved.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(KEY_DEFAULT_AFFIXES).withStyle(ChatFormatting.AQUA));
        for (StyleAffix affix : resolved) {
            appendAffix(affix, tooltip);
        }
    }

    public static void appendInnateAffixes(List<StyleAffix> affixes, List<Component> tooltip) {
        if (affixes.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(KEY_INNATE).withStyle(ChatFormatting.AQUA));
        for (StyleAffix affix : affixes) {
            appendAffix(affix, tooltip);
        }
    }

    public static void appendTiming(AttackOverride override, List<Component> tooltip) {
        appendTiming(override, List.of(), tooltip);
    }

    public static void appendTiming(AttackOverride override, List<StyleAffix> affixes,
                                    List<Component> tooltip) {
        if (override == null && affixes.isEmpty()) {
            return;
        }
        appendDrag(AttackDrag.effective(override, affixes), tooltip);
        if (override == null) {
            return;
        }
        appendMotion(override.behaviourOrDefault().motionOrNone(), tooltip);
        appendPlacement(override.behaviourOrDefault().placementOrNone(), tooltip);
        override.damageType()
                .filter(type -> !type.isBlank())
                .ifPresent(type -> tooltip.add(Component.translatable(KEY_DAMAGE_TYPE,
                        StyleText.prettify(type)).withStyle(ChatFormatting.AQUA)));
        override.cooldownScale()
                .filter(scale -> scale.amount() != AttackOverride.NEUTRAL_COOLDOWN)
                .ifPresent(scale -> tooltip.add(Component.translatable(KEY_COOLDOWN_SCALE,
                        StyleText.formatDecimal(scale.amount()))
                        .withStyle(scale.amount() < AttackOverride.NEUTRAL_COOLDOWN
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED)));
        override.hastensNext()
                .filter(haste -> haste.amount() != AttackOverride.NEUTRAL_COOLDOWN)
                .ifPresent(haste -> tooltip.add(Component.translatable(KEY_HASTENS_NEXT,
                        StyleText.formatDecimal(haste.amount()))
                        .withStyle(haste.amount() < AttackOverride.NEUTRAL_COOLDOWN
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED)));
    }

    private static void appendMotion(AffixEffects.Motion motion, List<Component> tooltip) {
        if (motion == null || motion.isEmpty()) {
            return;
        }
        motion.dash().ifPresent(dash -> {
            if (dash.forward() != 0.0F) {
                tooltip.add(Component.translatable(KEY_MOTION_DASH,
                                StyleText.formatDecimal(dash.forward()))
                        .withStyle(ChatFormatting.AQUA));
            }
            if (dash.upward() != 0.0F) {
                tooltip.add(Component.translatable(KEY_MOTION_LIFT,
                                StyleText.formatDecimal(dash.upward()))
                        .withStyle(ChatFormatting.AQUA));
            }
        });
        motion.blink().ifPresent(blink -> tooltip.add(Component.translatable(KEY_MOTION_BLINK,
                StyleText.formatDecimal(blink.distance()))
                .withStyle(ChatFormatting.AQUA)));
    }

    private static void appendPlacement(AttackPlacement placement, List<Component> tooltip) {
        if (placement == null || !placement.followsCursor()) {
            return;
        }
        tooltip.add(Component.translatable(KEY_PLACEMENT_CURSOR,
                        StyleText.formatDecimal(placement.clampedRange()))
                .withStyle(ChatFormatting.AQUA));
    }

    private static void appendDrag(AttackDrag drag, List<Component> tooltip) {
        if (drag == null || !drag.enabled()) {
            return;
        }
        tooltip.add(Component.translatable(KEY_DRAG_WINDOW, drag.ticks())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(KEY_DRAG_TARGETS, drag.maxTargets())
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    public static void appendStatGrid(StylePage page, List<Component> tooltip) {
        tooltip.add(pair(
                stat(KEY_DAMAGE, StyleText.formatMultiplier(page.damageMultiplier()),
                        colorFor(page.damageMultiplier())),
                stat(KEY_REACH, StyleText.formatMultiplier(page.rangeMultiplier()),
                        colorFor(page.rangeMultiplier()))));
        tooltip.add(pair(
                stat(KEY_UPSWING, StyleText.formatDecimal(page.upswing()),
                        StyleText.upswingColor(page.upswing(), 0.5D)),
                stat(KEY_MOVE, StyleText.formatMultiplier(page.movementSpeedMultiplier()),
                        colorFor(page.movementSpeedMultiplier()))));
    }

    public static void appendDetails(StylePage page, List<Component> tooltip) {
        appendDetails(page, null, tooltip);
    }

    public static void appendDetails(StylePage page, String soundOverride,
                                     List<Component> tooltip) {
        tooltip.add(Component.empty());
        tooltip.add(labelled(KEY_CATEGORY, StyleText.categoryName(page.category())));
        tooltip.add(labelled(KEY_ANGLE, Component.literal(
                StyleText.formatDegrees(page.angle()))));
        tooltip.add(labelled(KEY_HITBOX, StyleText.hitboxName(page.hitbox())));

        String soundId = soundOverride == null || soundOverride.isBlank()
                ? page.swingSoundId()
                : soundOverride;
        if (soundId != null && !soundId.isBlank()) {
            tooltip.add(labelled(KEY_SOUND, Component.literal(StyleText.soundName(soundId))));
        }

        WeaponAttributes.Condition[] conditions = page.attack().conditions();
        if (conditions != null && conditions.length > 0) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable(KEY_REQUIRES).withStyle(ChatFormatting.GOLD));
            for (WeaponAttributes.Condition condition : conditions) {
                tooltip.add(TooltipGem.NOTE.bullet(StyleText.conditionName(condition).copy()
                        .withStyle(ChatFormatting.YELLOW)));
            }
        }
    }

    public static void appendAttributes(AttackOverride override, List<Component> tooltip) {
        if (override == null || override.attributes().isEmpty()) {
            return;
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(KEY_ATTRIBUTES).withStyle(ChatFormatting.GOLD));
        for (Map.Entry<String, AttackAttribute> entry : override.attributes().entrySet()) {
            AttackAttribute value = entry.getValue();
            if (value == null || value.amount() == 0.0D) {
                continue;
            }
            tooltip.add(TooltipGem.STAT.bullet(
                    StyleText.attributeLine(entry.getKey(), value, ChatFormatting.GRAY)));
        }
    }

    public static Component expandHint() {
        return Component.translatable(KEY_EXPAND).withStyle(DIM);
    }

    private static MutableComponent labelled(String labelKey, Component value) {
        return Component.translatable(labelKey).withStyle(LABEL)
                .append(Component.literal(" ").append(value).withStyle(VALUE));
    }

    private static MutableComponent stat(String labelKey, String value, ChatFormatting color) {
        return Component.translatable(labelKey).withStyle(LABEL)
                .append(Component.literal(" " + value).withStyle(color));
    }

    private static MutableComponent pair(MutableComponent left, MutableComponent right) {
        return left.append(Component.literal(" ".repeat(COLUMN_PAD))).append(right);
    }

    private static ChatFormatting colorFor(double value) {
        if (Math.abs(value - 1.0D) < 0.001D) {
            return VALUE;
        }
        return value > 1.0D ? ChatFormatting.GREEN : ChatFormatting.RED;
    }
}
