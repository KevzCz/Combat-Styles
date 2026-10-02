package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffect;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;
import net.pixeldreamstudios.combat_styles.equipment.EventKind;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;

public final class EffectSummary {

    private static final String KEY_TRIGGER = "screen.combat_styles.effect_trigger";
    private static final String KEY_ALWAYS = "screen.combat_styles.effect_always";
    private static final String KEY_COOLDOWN = "screen.combat_styles.effect_cooldown";
    private static final String KEY_SUBSTITUTE = "screen.combat_styles.effect_substitute";
    private static final String KEY_AUGMENT = "screen.combat_styles.effect_augment";
    private static final String KEY_GRANTS = "screen.combat_styles.effect_grants";
    private static final String KEY_TUNES = "screen.combat_styles.effect_tunes";
    private static final String KEY_ATTRS = "screen.combat_styles.effect_attributes";
    private static final String KEY_SUMMON = "screen.combat_styles.effect_summon";
    private static final String KEY_ON_SWING = "screen.combat_styles.effect_on_swing";
    private static final String KEY_ON_HIT = "screen.combat_styles.effect_on_hit";
    private static final String KEY_ECHO = "screen.combat_styles.effect_echo";
    private static final String KEY_RESET = "screen.combat_styles.effect_reset_combo";

    private EffectSummary() {
    }

    public static List<Component> describe(String name, TriggeredEffect triggered) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(name).withStyle(ChatFormatting.GOLD));
        lines.add(triggerLine(triggered.trigger()));
        if (triggered.cooldownTicks() > 0) {
            lines.add(Component.translatable(KEY_COOLDOWN, triggered.cooldownTicks())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        appendEffect(triggered.effect(), lines);
        return lines;
    }

    private static Component triggerLine(EquipmentTrigger trigger) {
        List<String> parts = new ArrayList<>();
        trigger.everyNthSwing().ifPresent(value -> parts.add("every " + value));
        trigger.slotIndex().ifPresent(value -> parts.add("slot " + value));
        trigger.lastSlot().ifPresent(value -> parts.add(value ? "last slot" : "not last"));
        trigger.hitbox().ifPresent(value -> parts.add(StyleText.prettify(value)));
        trigger.styleId().ifPresent(value -> parts.add(StyleText.prettify(value)));
        trigger.affixId().ifPresent(value -> parts.add(StyleText.prettify(value)));
        trigger.idleTicks().ifPresent(value -> parts.add("idle " + value + "t"));
        trigger.onMiss().ifPresent(value -> parts.add(value ? "on miss" : ""));
        trigger.onKill().ifPresent(value -> parts.add(value ? "on kill" : ""));
        trigger.onSummonHit().ifPresent(value -> parts.add(value ? "summon hit" : ""));
        if (trigger.reactsToHurt()) {
            parts.add("on hurt");
        }
        parts.removeIf(String::isBlank);
        return parts.isEmpty()
                ? Component.translatable(KEY_ALWAYS).withStyle(ChatFormatting.GRAY)
                : Component.translatable(KEY_TRIGGER, String.join(", ", parts))
                        .withStyle(ChatFormatting.GRAY);
    }

    private static void appendEffect(EquipmentEffect effect, List<Component> lines) {
        effect.substituteStyle().ifPresent(style ->
                lines.add(Component.translatable(KEY_SUBSTITUTE, StyleText.prettify(style))
                        .withStyle(ChatFormatting.AQUA)));
        effect.substitution().ifPresent(substitution ->
                lines.add(Component.translatable(KEY_SUBSTITUTE,
                                String.join(", ", substitution.styles()))
                        .withStyle(ChatFormatting.AQUA)));
        effect.augment().ifPresent(augment -> appendAugment(augment, lines));
        if (!effect.grantAffixes().isEmpty()) {
            lines.add(Component.translatable(KEY_GRANTS,
                            String.join(", ", effect.grantAffixes()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (!effect.tuneAffixes().isEmpty()) {
            lines.add(Component.translatable(KEY_TUNES, effect.tuneAffixes().size())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (!effect.attributes().isEmpty()) {
            lines.add(Component.translatable(KEY_ATTRS, effect.attributes().size())
                    .withStyle(ChatFormatting.YELLOW));
        }
        effect.onSwing().ifPresent(value ->
                lines.add(Component.translatable(KEY_ON_SWING).withStyle(ChatFormatting.GRAY)));
        effect.onHit().ifPresent(value ->
                lines.add(Component.translatable(KEY_ON_HIT).withStyle(ChatFormatting.GRAY)));
        for (EventKind kind : EventKind.values()) {
            if (effect.events().has(kind)) {
                lines.add(Component.translatable(kind.labelKey()).withStyle(ChatFormatting.GRAY));
            }
        }
        effect.echo().ifPresent(value ->
                lines.add(Component.translatable(KEY_ECHO).withStyle(ChatFormatting.GRAY)));
        effect.resetCombo().ifPresent(value -> {
            if (value > 0) {
                lines.add(Component.translatable(KEY_RESET).withStyle(ChatFormatting.GRAY));
            }
        });
        effect.summonReplacement().ifPresent(summon ->
                lines.add(Component.translatable(KEY_SUMMON,
                        StyleText.registryName(summon.entity())).withStyle(ChatFormatting.GREEN)));
        effect.summonProfile().ifPresent(profile -> profile.entity().ifPresent(entity ->
                lines.add(Component.translatable(KEY_SUMMON, StyleText.registryName(entity))
                        .withStyle(ChatFormatting.GREEN))));
    }

    private static void appendAugment(AttackOverride augment, List<Component> lines) {
        List<String> parts = new ArrayList<>();
        augment.damageMultiplier().ifPresent(value ->
                parts.add(StyleText.formatMultiplier(value) + " dmg"));
        augment.rangeMultiplier().ifPresent(value ->
                parts.add(StyleText.formatMultiplier(value) + " range"));
        augment.hitbox().ifPresent(value -> parts.add(StyleText.prettify(value)));
        if (!augment.attributes().isEmpty()) {
            parts.add(augment.attributes().size() + " attr");
        }
        augment.behaviour().ifPresent(behaviour -> {
            if (!behaviour.slashes().isEmpty()) {
                parts.add(behaviour.slashes().size() + " slash");
            }
            if (!behaviour.extras().isEmpty()) {
                parts.add(behaviour.extras().size() + " extra");
            }
        });
        lines.add(Component.translatable(KEY_AUGMENT,
                        parts.isEmpty() ? "-" : String.join(", ", parts))
                .withStyle(ChatFormatting.YELLOW));
    }

}
