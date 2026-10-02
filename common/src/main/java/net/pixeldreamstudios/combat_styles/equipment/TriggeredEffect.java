package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;

import java.util.List;

public record TriggeredEffect(EquipmentTrigger trigger,
                              EquipmentEffect effect,
                              EffectCooldown cooldown,
                              boolean hideTooltip,
                              int delayTicks,
                              List<AffixEffects.SelfEffect> selfEffects,
                              String tooltipHeader,
                              List<String> tooltipLines) {

    public static final int NO_DELAY = 0;
    public static final String TOOLTIP_HEADER = "tooltip_header";
    public static final String TOOLTIP_LINES = "tooltip_lines";

    public static final Codec<TriggeredEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EquipmentTrigger.CODEC.optionalFieldOf("trigger", EquipmentTrigger.ALWAYS)
                    .forGetter(TriggeredEffect::trigger),
            EquipmentEffect.CODEC.fieldOf("effect").forGetter(TriggeredEffect::effect),
            EffectCooldown.CODEC.optionalFieldOf("cooldown", EffectCooldown.NONE)
                    .forGetter(TriggeredEffect::cooldown),
            Codec.BOOL.optionalFieldOf("hide_tooltip", false)
                    .forGetter(TriggeredEffect::hideTooltip),
            Codec.INT.optionalFieldOf("delay_ticks", NO_DELAY)
                    .forGetter(TriggeredEffect::delayTicks),
            AffixEffects.SelfEffect.CODEC.listOf().optionalFieldOf("self_effects", List.of())
                    .forGetter(TriggeredEffect::selfEffects),
            Codec.STRING.optionalFieldOf(TOOLTIP_HEADER, "")
                    .forGetter(TriggeredEffect::tooltipHeader),
            Codec.STRING.listOf().optionalFieldOf(TOOLTIP_LINES, List.of())
                    .forGetter(TriggeredEffect::tooltipLines)
    ).apply(instance, TriggeredEffect::new));

    public TriggeredEffect(EquipmentTrigger trigger, EquipmentEffect effect,
                           EffectCooldown cooldown, boolean hideTooltip) {
        this(trigger, effect, cooldown, hideTooltip, NO_DELAY, List.of());
    }

    public TriggeredEffect(EquipmentTrigger trigger, EquipmentEffect effect,
                           EffectCooldown cooldown, boolean hideTooltip, int delayTicks) {
        this(trigger, effect, cooldown, hideTooltip, delayTicks, List.of());
    }

    public TriggeredEffect(EquipmentTrigger trigger, EquipmentEffect effect,
                           EffectCooldown cooldown, boolean hideTooltip, int delayTicks,
                           List<AffixEffects.SelfEffect> selfEffects) {
        this(trigger, effect, cooldown, hideTooltip, delayTicks, selfEffects, "", List.of());
    }

    public TriggeredEffect {
        cooldown = cooldown == null ? EffectCooldown.NONE : cooldown;
        delayTicks = Math.max(NO_DELAY, delayTicks);
        selfEffects = selfEffects == null ? List.of() : List.copyOf(selfEffects);
        tooltipHeader = tooltipHeader == null ? "" : tooltipHeader;
        tooltipLines = tooltipLines == null
                ? List.of()
                : tooltipLines.stream().filter(line -> line != null && !line.isBlank()).toList();
    }

    public boolean rollsChance() {
        return trigger.rollsChance();
    }

    public TriggeredEffect withTrigger(EquipmentTrigger value) {
        return new TriggeredEffect(value, effect, cooldown, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines);
    }

    public TriggeredEffect withEffect(EquipmentEffect value) {
        return new TriggeredEffect(trigger, value, cooldown, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines);
    }

    public TriggeredEffect withCooldown(EffectCooldown value) {
        return new TriggeredEffect(trigger, effect, value, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines);
    }

    public TriggeredEffect withHideTooltip(boolean value) {
        return new TriggeredEffect(trigger, effect, cooldown, value, delayTicks,
                selfEffects, tooltipHeader, tooltipLines);
    }

    public TriggeredEffect withDelayTicks(int value) {
        return new TriggeredEffect(trigger, effect, cooldown, hideTooltip, value,
                selfEffects, tooltipHeader, tooltipLines);
    }

    public TriggeredEffect withSelfEffects(List<AffixEffects.SelfEffect> value) {
        return new TriggeredEffect(trigger, effect, cooldown, hideTooltip, delayTicks,
                value, tooltipHeader, tooltipLines);
    }

    public TooltipOverride tooltip() {
        return new TooltipOverride(tooltipHeader, tooltipLines);
    }

    public TriggeredEffect withTooltip(TooltipOverride value) {
        TooltipOverride applied = value == null ? TooltipOverride.NONE : value;
        return new TriggeredEffect(trigger, effect, cooldown, hideTooltip, delayTicks,
                selfEffects, applied.header(), applied.lines());
    }

    public boolean needsArming() {
        if (!effect.hasAlwaysOnParts() || cooldown.windowed() || trigger.isAlways()) {
            return false;
        }
        return trigger.needsArmingAtRest();
    }

    public boolean needsEdgeWindow() {
        if (!effect.hasAlwaysOnParts() || cooldown.windowed() || trigger.isAlways()) {
            return false;
        }
        return !trigger.needsArmingAtRest() && !cooldown.holdWhileActive();
    }

    public boolean armsOnSwing() {
        return effect.hasAlwaysOnParts() && cooldown.windowed() && !trigger.isEvent()
                && trigger.swingDriven() && !EquipmentDefinition.substitutes(effect);
    }

    public boolean liveWhileSwingMatches() {
        return effect.hasAlwaysOnParts() && !cooldown.windowed() && !trigger.isEvent()
                && trigger.swingOnlyAtRest() && !EquipmentDefinition.substitutes(effect);
    }

    public boolean swingGateIgnored() {
        return EquipmentDefinition.substitutes(effect) && trigger.swingOnlyAtRest()
                && !trigger.arms(cooldown);
    }

    public boolean selfEffectsOnHit() {
        if (effect.onSwing().isPresent()) {
            return false;
        }
        if (effect.onHit().isPresent()) {
            return true;
        }
        return trigger.requiresTarget()
                || trigger.inTree(node -> node.onKill().orElse(false));
    }

    public boolean liveWhileUsing() {
        return effect.hasAlwaysOnParts() && trigger.liveWhileUsing() && !cooldown.windowed()
                && cooldown.holdWhileActive();
    }

    public boolean delayed() {
        return delayTicks > NO_DELAY;
    }

    public int cooldownTicks() {
        return cooldown.ticks();
    }
}
