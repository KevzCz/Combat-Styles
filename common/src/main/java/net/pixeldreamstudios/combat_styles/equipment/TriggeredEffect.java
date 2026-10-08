package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TriggeredEffect(EquipmentTrigger trigger,
                              EquipmentEffect effect,
                              EffectCooldown cooldown,
                              boolean hideTooltip,
                              int delayTicks,
                              List<AffixEffects.SelfEffect> selfEffects,
                              String tooltipHeader,
                              List<String> tooltipLines,
                              Optional<EffectStacking> stacking,
                              Map<String, String> tooltipEdits,
                              List<String> tooltipOrder,
                              Map<String, String> tooltipDetail,
                              Map<String, Integer> tooltipIndent,
                              Map<String, String> tooltipIcons) {

    public static final int NO_DELAY = 0;
    public static final String TOOLTIP_HEADER = "tooltip_header";
    public static final String TOOLTIP_LINES = "tooltip_lines";
    public static final String TOOLTIP_EDITS = "tooltip_edits";
    public static final String TOOLTIP_ORDER = "tooltip_order";
    public static final String TOOLTIP_DETAIL = "tooltip_detail";
    public static final String TOOLTIP_INDENT = "tooltip_indent";
    public static final String TOOLTIP_ICON = "tooltip_icon";

    public static final Codec<TriggeredEffect> CODEC = Customs.wrap(TriggeredEffect.class, RecordCodecBuilder.create(instance -> instance.group(
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
                    .forGetter(TriggeredEffect::tooltipLines),
            EffectStacking.CODEC.optionalFieldOf(EffectStacking.KEY)
                    .forGetter(TriggeredEffect::stacking),
            TooltipOverride.EDITS_CODEC.optionalFieldOf(TOOLTIP_EDITS, Map.of())
                    .forGetter(TriggeredEffect::tooltipEdits),
            Codec.STRING.listOf().optionalFieldOf(TOOLTIP_ORDER, List.of())
                    .forGetter(TriggeredEffect::tooltipOrder),
            TooltipOverride.EDITS_CODEC.optionalFieldOf(TOOLTIP_DETAIL, Map.of())
                    .forGetter(TriggeredEffect::tooltipDetail),
            TooltipOverride.INDENT_CODEC.optionalFieldOf(TOOLTIP_INDENT, Map.of())
                    .forGetter(TriggeredEffect::tooltipIndent),
            TooltipOverride.EDITS_CODEC.optionalFieldOf(TOOLTIP_ICON, Map.of())
                    .forGetter(TriggeredEffect::tooltipIcons)
    ).apply(instance, TriggeredEffect::new)));

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

    public TriggeredEffect(EquipmentTrigger trigger, EquipmentEffect effect,
                           EffectCooldown cooldown, boolean hideTooltip, int delayTicks,
                           List<AffixEffects.SelfEffect> selfEffects, String tooltipHeader,
                           List<String> tooltipLines) {
        this(trigger, effect, cooldown, hideTooltip, delayTicks, selfEffects, tooltipHeader, tooltipLines,
                Optional.empty(), Map.of(), List.of(), Map.of(), Map.of(), Map.of());
    }

    public TriggeredEffect {
        cooldown = cooldown == null ? EffectCooldown.NONE : cooldown;
        delayTicks = Math.max(NO_DELAY, delayTicks);
        selfEffects = selfEffects == null ? List.of() : List.copyOf(selfEffects);
        tooltipHeader = tooltipHeader == null ? "" : tooltipHeader;
        tooltipLines = tooltipLines == null
                ? List.of()
                : tooltipLines.stream().filter(line -> line != null && !line.isBlank()).toList();
        stacking = stacking == null ? Optional.empty() : stacking;
        tooltipEdits = TooltipOverride.normalized(tooltipEdits);
        tooltipOrder = tooltipOrder == null ? List.of() : List.copyOf(tooltipOrder);
        tooltipDetail = TooltipOverride.normalized(tooltipDetail);
        tooltipIndent = tooltipIndent == null ? Map.of() : Map.copyOf(tooltipIndent);
        tooltipIcons = TooltipOverride.normalized(tooltipIcons);
    }

    public boolean rollsChance() {
        return trigger.rollsChance();
    }

    public TriggeredEffect withTrigger(EquipmentTrigger value) {
        return Customs.copy(this, new TriggeredEffect(value, effect, cooldown, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines, stacking, tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
    }

    public TriggeredEffect withEffect(EquipmentEffect value) {
        return Customs.copy(this, new TriggeredEffect(trigger, value, cooldown, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines, stacking, tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
    }

    public TriggeredEffect withCooldown(EffectCooldown value) {
        return Customs.copy(this, new TriggeredEffect(trigger, effect, value, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines, stacking, tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
    }

    public TriggeredEffect withHideTooltip(boolean value) {
        return Customs.copy(this, new TriggeredEffect(trigger, effect, cooldown, value, delayTicks,
                selfEffects, tooltipHeader, tooltipLines, stacking, tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
    }

    public TriggeredEffect withDelayTicks(int value) {
        return Customs.copy(this, new TriggeredEffect(trigger, effect, cooldown, hideTooltip, value,
                selfEffects, tooltipHeader, tooltipLines, stacking, tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
    }

    public TriggeredEffect withSelfEffects(List<AffixEffects.SelfEffect> value) {
        return Customs.copy(this, new TriggeredEffect(trigger, effect, cooldown, hideTooltip, delayTicks,
                value, tooltipHeader, tooltipLines, stacking, tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
    }

    public TooltipOverride tooltip() {
        return new TooltipOverride(tooltipHeader, tooltipLines, tooltipEdits, tooltipOrder, tooltipDetail,
                tooltipIndent, tooltipIcons);
    }

    public TriggeredEffect withTooltip(TooltipOverride value) {
        TooltipOverride applied = value == null ? TooltipOverride.NONE : value;
        return Customs.copy(this, new TriggeredEffect(trigger, effect, cooldown, hideTooltip, delayTicks,
                selfEffects, applied.header(), applied.lines(), stacking, applied.edits(), applied.order(),
                applied.detail(), applied.indent(), applied.icons()));
    }

    public TriggeredEffect withStacking(Optional<EffectStacking> value) {
        return Customs.copy(this, new TriggeredEffect(trigger, effect, cooldown, hideTooltip, delayTicks,
                selfEffects, tooltipHeader, tooltipLines, value == null ? Optional.empty() : value,
                tooltipEdits, tooltipOrder, tooltipDetail, tooltipIndent, tooltipIcons));
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

    public boolean selfEffectsInPasses() {
        return effect.onHit().isPresent() || effect.onSwing().isPresent()
                || effect.events().isEmpty();
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
