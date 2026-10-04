package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public record AttackOverride(Optional<Double> damageMultiplier,
                             Optional<Double> angle,
                             Optional<Double> upswing,
                             Optional<Float> rangeMultiplier,
                             Optional<Float> movementSpeedMultiplier,
                             Optional<String> hitbox,
                             Optional<String> animation,
                             Optional<String> swingSound,
                             Optional<String> rarity,
                             Optional<Double> costOverride,
                             Optional<Boolean> disabled,
                             Map<String, AttackAttribute> attributes,
                             Optional<Boolean> ignoreAffinity,
                             Optional<AttackAttribute> cooldownScale,
                             Optional<AttackAttribute> hastensNext,
                             Optional<AttackDrag> drag,
                             Optional<AttackBehaviour> behaviour,
                             Optional<String> damageType,
                             List<String> affixes,
                             Map<String, AffixTuning> affixTuning,
                             Optional<Boolean> scaleAnimation) {

    private record Shape(Optional<Double> damageMultiplier, Optional<Double> angle,
                          Optional<Double> upswing, Optional<Float> rangeMultiplier,
                          Optional<Float> movementSpeedMultiplier, Optional<String> hitbox,
                          Optional<String> animation) {
    }

    private record Meta(Optional<String> swingSound, Optional<String> rarity,
                        Optional<Double> costOverride, Optional<Boolean> disabled,
                        Map<String, AttackAttribute> attributes,
                        Optional<Boolean> ignoreAffinity, Optional<String> damageType,
                        List<String> affixes, Map<String, AffixTuning> affixTuning) {
    }

    public static final double NEUTRAL_COOLDOWN = 1.0D;
    public static final String SILENT_SOUND = CombatStyles.MOD_ID + ":none";
    private static final String DIFF_ARROW = " → ";
    private static final String DIFF_NONE = "-";

    public static boolean isSilent(String soundId) {
        return SILENT_SOUND.equals(soundId);
    }

    private record Timing(Optional<AttackAttribute> cooldownScale,
                          Optional<AttackAttribute> hastensNext,
                          Optional<AttackDrag> drag,
                          Optional<Boolean> scaleAnimation) {
    }

    private static final MapCodec<Shape> SHAPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("damage_multiplier").forGetter(Shape::damageMultiplier),
            Codec.DOUBLE.optionalFieldOf("angle").forGetter(Shape::angle),
            Codec.DOUBLE.optionalFieldOf("upswing").forGetter(Shape::upswing),
            Codec.FLOAT.optionalFieldOf("range_multiplier").forGetter(Shape::rangeMultiplier),
            Codec.FLOAT.optionalFieldOf("movement_speed_multiplier")
                    .forGetter(Shape::movementSpeedMultiplier),
            Codec.STRING.optionalFieldOf("hitbox").forGetter(Shape::hitbox),
            Codec.STRING.optionalFieldOf("animation").forGetter(Shape::animation)
    ).apply(instance, Shape::new));

    private static final MapCodec<Meta> META_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("swing_sound").forGetter(Meta::swingSound),
            Codec.STRING.optionalFieldOf("rarity").forGetter(Meta::rarity),
            Codec.DOUBLE.optionalFieldOf("cost").forGetter(Meta::costOverride),
            Codec.BOOL.optionalFieldOf("disabled").forGetter(Meta::disabled),
            Codec.unboundedMap(Codec.STRING, AttackAttribute.CODEC)
                    .optionalFieldOf("attributes", Map.of())
                    .forGetter(Meta::attributes),
            Codec.BOOL.optionalFieldOf("ignore_affinity").forGetter(Meta::ignoreAffinity),
            Codec.STRING.optionalFieldOf("damage_type").forGetter(Meta::damageType),
            Codec.STRING.listOf().optionalFieldOf("affixes", List.of())
                    .forGetter(Meta::affixes),
            AffixTuning.MAP_CODEC.optionalFieldOf("affix_tuning", Map.of())
                    .forGetter(Meta::affixTuning)
    ).apply(instance, Meta::new));

    private static final MapCodec<Timing> TIMING_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttackAttribute.CODEC.optionalFieldOf("cooldown_scale").forGetter(Timing::cooldownScale),
            AttackAttribute.CODEC.optionalFieldOf("hastens_next").forGetter(Timing::hastensNext),
            AttackDrag.CODEC.optionalFieldOf("drag").forGetter(Timing::drag),
            Codec.BOOL.optionalFieldOf("scale_animation").forGetter(Timing::scaleAnimation)
    ).apply(instance, Timing::new));

    public static final Codec<AttackOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SHAPE_CODEC.forGetter(override -> new Shape(override.damageMultiplier(),
                    override.angle(), override.upswing(), override.rangeMultiplier(),
                    override.movementSpeedMultiplier(), override.hitbox(), override.animation())),
            META_CODEC.forGetter(override -> new Meta(override.swingSound(), override.rarity(),
                    override.costOverride(), override.disabled(), override.attributes(),
                    override.ignoreAffinity(), override.damageType(),
                    override.affixes(), override.affixTuning())),
            TIMING_CODEC.forGetter(override -> new Timing(override.cooldownScale(),
                    override.hastensNext(), override.drag(), override.scaleAnimation())),
            AttackBehaviour.CODEC.optionalFieldOf("behaviour")
                    .forGetter(AttackOverride::behaviour)
    ).apply(instance, (shape, meta, timing, behaviour) -> new AttackOverride(
            shape.damageMultiplier(), shape.angle(), shape.upswing(), shape.rangeMultiplier(),
            shape.movementSpeedMultiplier(), shape.hitbox(), shape.animation(),
            meta.swingSound(), meta.rarity(), meta.costOverride(), meta.disabled(),
            meta.attributes(), meta.ignoreAffinity(),
            timing.cooldownScale(), timing.hastensNext(), timing.drag(), behaviour,
            meta.damageType(), meta.affixes(), meta.affixTuning(),
            timing.scaleAnimation())));

    public AttackOverride {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        affixes = affixes == null ? List.of() : List.copyOf(affixes);
        affixTuning = affixTuning == null ? Map.of() : Map.copyOf(affixTuning);
        scaleAnimation = scaleAnimation == null ? Optional.empty() : scaleAnimation;
    }

    public AttackOverride(Optional<Double> damageMultiplier, Optional<Double> angle,
                          Optional<Double> upswing, Optional<Float> rangeMultiplier,
                          Optional<Float> movementSpeedMultiplier, Optional<String> hitbox,
                          Optional<String> animation, Optional<String> swingSound,
                          Optional<String> rarity, Optional<Double> costOverride,
                          Optional<Boolean> disabled, Map<String, AttackAttribute> attributes,
                          Optional<Boolean> ignoreAffinity,
                          Optional<AttackAttribute> cooldownScale,
                          Optional<AttackAttribute> hastensNext,
                          Optional<AttackDrag> drag,
                          Optional<AttackBehaviour> behaviour,
                          Optional<String> damageType, List<String> affixes,
                          Map<String, AffixTuning> affixTuning) {
        this(damageMultiplier, angle, upswing, rangeMultiplier, movementSpeedMultiplier,
                hitbox, animation, swingSound, rarity, costOverride, disabled, attributes,
                ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType,
                affixes, affixTuning, Optional.empty());
    }

    public AttackOverride(Optional<Double> damageMultiplier, Optional<Double> angle,
                          Optional<Double> upswing, Optional<Float> rangeMultiplier,
                          Optional<Float> movementSpeedMultiplier, Optional<String> hitbox,
                          Optional<String> animation, Optional<String> swingSound,
                          Optional<String> rarity, Optional<Double> costOverride,
                          Optional<Boolean> disabled, Map<String, AttackAttribute> attributes,
                          Optional<Boolean> ignoreAffinity,
                          Optional<AttackAttribute> cooldownScale,
                          Optional<AttackAttribute> hastensNext,
                          Optional<AttackDrag> drag,
                          Optional<AttackBehaviour> behaviour,
                          Optional<String> damageType) {
        this(damageMultiplier, angle, upswing, rangeMultiplier, movementSpeedMultiplier,
                hitbox, animation, swingSound, rarity, costOverride, disabled, attributes,
                ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType,
                List.of(), Map.of());
    }

    public AttackOverride(Optional<Double> damageMultiplier, Optional<Double> angle,
                          Optional<Double> upswing, Optional<Float> rangeMultiplier,
                          Optional<Float> movementSpeedMultiplier, Optional<String> hitbox,
                          Optional<String> animation, Optional<String> swingSound,
                          Optional<String> rarity, Optional<Double> costOverride,
                          Optional<Boolean> disabled) {
        this(damageMultiplier, angle, upswing, rangeMultiplier, movementSpeedMultiplier,
                hitbox, animation, swingSound, rarity, costOverride, disabled, Map.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public static AttackOverride from(StylePage page) {
        WeaponAttributes.Attack attack = page.attack();
        return new AttackOverride(
                Optional.of(attack.damageMultiplier()),
                Optional.of(attack.angle()),
                Optional.of(attack.upswing()),
                Optional.of(attack.rangeMultiplier()),
                Optional.of(attack.movementSpeedMultiplier()),
                Optional.ofNullable(attack.hitbox()).map(Enum::name),
                Optional.ofNullable(attack.animation()),
                Optional.ofNullable(page.swingSoundId()),
                Optional.of(page.rarity().name().toLowerCase(Locale.ROOT)),
                Optional.empty(),
                Optional.of(Boolean.FALSE),
                Map.of(),
                Optional.empty(),
                Optional.of(AttackAttribute.flat(NEUTRAL_COOLDOWN)),
                Optional.of(AttackAttribute.flat(NEUTRAL_COOLDOWN)),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static final StreamCodec<ByteBuf, AttackOverride> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public static final StreamCodec<ByteBuf, Map<String, AttackOverride>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

    public static final AttackOverride EMPTY = new AttackOverride(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public String signature() {
        return damageMultiplier.map(v -> "d" + Math.round(v * 1000)).orElse("")
                + angle.map(v -> "a" + Math.round(v * 100)).orElse("")
                + upswing.map(v -> "u" + Math.round(v * 1000)).orElse("")
                + rangeMultiplier.map(v -> "r" + Math.round(v * 1000)).orElse("")
                + movementSpeedMultiplier.map(v -> "m" + Math.round(v * 1000)).orElse("")
                + hitbox.orElse("") + animation.orElse("") + swingSound.orElse("")
                + rarity.orElse("") + costOverride.map(v -> "c" + v).orElse("")
                + damageType.map(v -> "t" + v).orElse("")
                + disabled.map(v -> "x" + v).orElse("")
                + cooldownScale.map(v -> "s" + Math.round(v.amount() * 1000)).orElse("")
                + hastensNext.map(v -> "h" + Math.round(v.amount() * 1000)).orElse("")
                + drag.map(v -> "g" + v.signature()).orElse("")
                + scaleAnimation.map(v -> "z" + v).orElse("")
                + behaviour.map(AttackBehaviour::signature).orElse("");
    }

    public AttackOverride overriddenBy(AttackOverride other, InheritGroups groups) {
        if (other == null) {
            return this;
        }
        if (groups == null || groups.all()) {
            return overriddenBy(other);
        }
        InheritMode shapeMode = groups.shape();
        InheritMode metaMode = groups.meta();
        InheritMode timingMode = groups.timing();
        InheritMode behaviourMode = groups.behaviour();
        AttackOverride shape = shapeMode.inherits() ? this : other;
        AttackOverride meta = metaMode.inherits() ? this : other;
        AttackOverride timing = timingMode.inherits() ? this : other;
        return new AttackOverride(
                pick(shapeMode, damageMultiplier, other.damageMultiplier(), Double::doubleValue),
                pick(shapeMode, angle, other.angle(), Double::doubleValue),
                pick(shapeMode, upswing, other.upswing(), Double::doubleValue),
                pick(shapeMode, rangeMultiplier, other.rangeMultiplier(), Float::doubleValue),
                pick(shapeMode, movementSpeedMultiplier, other.movementSpeedMultiplier(), Float::doubleValue),
                shape.hitbox().or(other::hitbox),
                shape.animation().or(other::animation),
                meta.swingSound().or(other::swingSound),
                meta.rarity().or(other::rarity),
                pick(metaMode, costOverride, other.costOverride(), Double::doubleValue),
                meta.disabled().or(other::disabled),
                pickAttributes(metaMode, attributes, other.attributes()),
                meta.ignoreAffinity().or(other::ignoreAffinity),
                pick(timingMode, cooldownScale, other.cooldownScale(), AttackAttribute::amount),
                timing.hastensNext().or(other::hastensNext),
                timing.drag().or(other::drag),
                mergedBehaviour(behaviourMode, behaviour(), other.behaviour()),
                meta.damageType().or(other::damageType))
                .withScaleAnimation(timing.scaleAnimation().or(other::scaleAnimation));
    }

    public AttackOverride mergedWith(AttackOverride other, MergeMode mode) {
        if (other == null) {
            return this;
        }
        if (mode == null || mode == MergeMode.REPLACE) {
            return overriddenBy(other);
        }
        boolean scaling = mode == MergeMode.MULTIPLY;
        return new AttackOverride(
                scaling
                        ? compound(damageMultiplier, other.damageMultiplier())
                        : sum(damageMultiplier, other.damageMultiplier()),
                other.angle().or(this::angle),
                other.upswing().or(this::upswing),
                scaling
                        ? compoundFloat(rangeMultiplier, other.rangeMultiplier())
                        : sumFloat(rangeMultiplier, other.rangeMultiplier()),
                scaling
                        ? compoundFloat(movementSpeedMultiplier,
                                other.movementSpeedMultiplier())
                        : sumFloat(movementSpeedMultiplier, other.movementSpeedMultiplier()),
                other.hitbox().or(this::hitbox),
                other.animation().or(this::animation),
                other.swingSound().or(this::swingSound),
                other.rarity().or(this::rarity),
                other.costOverride().or(this::costOverride),
                other.disabled().or(this::disabled),
                stackAttributes(attributes, other.attributes()),
                other.ignoreAffinity().or(this::ignoreAffinity),
                other.cooldownScale().or(this::cooldownScale),
                other.hastensNext().or(this::hastensNext),
                other.drag().or(this::drag),
                mergedBehaviour(other, mode),
                other.damageType().or(this::damageType))
                .withScaleAnimation(other.scaleAnimation().or(this::scaleAnimation));
    }

    private Optional<AttackBehaviour> mergedBehaviour(AttackOverride other, MergeMode mode) {
        if (behaviour.isEmpty()) {
            return other.behaviour();
        }
        if (other.behaviour().isEmpty()) {
            return behaviour;
        }
        return Optional.of(behaviour.get().mergedWith(other.behaviour().get(), mode));
    }

    private static Map<String, AttackAttribute> pickAttributes(
            InheritMode mode, Map<String, AttackAttribute> parent,
            Map<String, AttackAttribute> own) {
        if (parent.isEmpty()) {
            return own;
        }
        if (own.isEmpty()) {
            return parent;
        }
        Map<String, AttackAttribute> merged = new LinkedHashMap<>(parent);
        for (Map.Entry<String, AttackAttribute> entry : own.entrySet()) {
            AttackAttribute mine = entry.getValue();
            AttackAttribute theirs = merged.get(entry.getKey());
            if (theirs == null) {
                merged.put(entry.getKey(), mine);
                continue;
            }
            merged.put(entry.getKey(), pick(mode, Optional.of(theirs), Optional.of(mine),
                    AttackAttribute::amount).orElse(mine));
        }
        return Map.copyOf(merged);
    }

    private static Optional<AttackBehaviour> mergedBehaviour(InheritMode mode,
                                                             Optional<AttackBehaviour> parent,
                                                             Optional<AttackBehaviour> own) {
        if (mode == InheritMode.OWN) {
            return own;
        }
        if (parent.isEmpty() || own.isEmpty()) {
            return own.or(() -> parent);
        }
        return Optional.of(parent.get().mergedWith(own.get(), MergeMode.ADD));
    }

    private static <T> Optional<T> pick(InheritMode mode, Optional<T> parent, Optional<T> own,
                                        ToDoubleFunction<T> weight) {
        if (!mode.compares()) {
            return mode.inherits() ? parent.or(() -> own) : own;
        }
        if (parent.isEmpty() || own.isEmpty()) {
            return own.or(() -> parent);
        }
        double ownValue = weight.applyAsDouble(own.get());
        double parentValue = weight.applyAsDouble(parent.get());
        boolean ownWins = mode == InheritMode.HIGHEST
                ? ownValue >= parentValue
                : ownValue <= parentValue;
        return ownWins ? own : parent;
    }

    private static Optional<Double> compound(Optional<Double> base, Optional<Double> extra) {
        if (base.isEmpty() || extra.isEmpty()) {
            return extra.or(() -> base);
        }
        return Optional.of(base.get() * extra.get());
    }

    private static Optional<Double> sum(Optional<Double> base, Optional<Double> extra) {
        if (base.isEmpty() || extra.isEmpty()) {
            return extra.or(() -> base);
        }
        return Optional.of(base.get() + extra.get());
    }

    private static Optional<Float> compoundFloat(Optional<Float> base, Optional<Float> extra) {
        if (base.isEmpty() || extra.isEmpty()) {
            return extra.or(() -> base);
        }
        return Optional.of(base.get() * extra.get());
    }

    private static Optional<Float> sumFloat(Optional<Float> base, Optional<Float> extra) {
        if (base.isEmpty() || extra.isEmpty()) {
            return extra.or(() -> base);
        }
        return Optional.of(base.get() + extra.get());
    }

    private static Map<String, AttackAttribute> stackAttributes(
            Map<String, AttackAttribute> base, Map<String, AttackAttribute> extra) {
        if (extra.isEmpty()) {
            return base;
        }
        Map<String, AttackAttribute> merged = new LinkedHashMap<>(base);
        for (Map.Entry<String, AttackAttribute> entry : extra.entrySet()) {
            AttackAttribute existing = merged.get(entry.getKey());
            AttackAttribute added = entry.getValue();
            if (existing == null || existing.operation() != added.operation()) {
                merged.put(entry.getKey(), added);
                continue;
            }
            if (!added.stacks()) {
                continue;
            }
            merged.put(entry.getKey(), new AttackAttribute(
                    existing.amount() + added.amount(), existing.operation(), true));
        }
        return Map.copyOf(merged);
    }

    public AttackOverride overriddenBy(AttackOverride other) {
        if (other == null) {
            return this;
        }
        Map<String, AttackAttribute> mergedAttributes;
        if (other.attributes().isEmpty()) {
            mergedAttributes = attributes;
        } else {
            Map<String, AttackAttribute> merged = new LinkedHashMap<>(attributes);
            merged.putAll(other.attributes());
            mergedAttributes = Map.copyOf(merged);
        }
        return new AttackOverride(
                other.damageMultiplier().or(this::damageMultiplier),
                other.angle().or(this::angle),
                other.upswing().or(this::upswing),
                other.rangeMultiplier().or(this::rangeMultiplier),
                other.movementSpeedMultiplier().or(this::movementSpeedMultiplier),
                other.hitbox().or(this::hitbox),
                other.animation().or(this::animation),
                other.swingSound().or(this::swingSound),
                other.rarity().or(this::rarity),
                other.costOverride().or(this::costOverride),
                other.disabled().or(this::disabled),
                mergedAttributes,
                other.ignoreAffinity().or(this::ignoreAffinity),
                other.cooldownScale().or(this::cooldownScale),
                other.hastensNext().or(this::hastensNext),
                other.drag().or(this::drag),
                other.behaviour().or(this::behaviour),
                other.damageType().or(this::damageType))
                .withScaleAnimation(other.scaleAnimation().or(this::scaleAnimation));
    }

    public AttackOverride withBehaviour(AttackBehaviour value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, ignoreAffinity, cooldownScale, hastensNext, drag,
                value == null || value.isEmpty() ? Optional.empty() : Optional.of(value),
                damageType).withScaleAnimation(scaleAnimation);
    }

    public AttackBehaviour behaviourOrDefault() {
        return behaviour.orElse(AttackBehaviour.NONE);
    }

    public AttackOverride withAffixes(List<String> ids, Map<String, AffixTuning> tuning) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, ignoreAffinity, cooldownScale, hastensNext, drag,
                behaviour, damageType,
                ids == null ? List.of() : List.copyOf(ids),
                tuning == null ? Map.of() : Map.copyOf(tuning), scaleAnimation);
    }

    public AttackOverride withAttributes(Map<String, AttackAttribute> value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, value == null ? Map.of() : Map.copyOf(value), ignoreAffinity,
                cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withIgnoreAffinity(boolean value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, Optional.of(value), cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withDamageMultiplier(double value) {
        return new AttackOverride(Optional.of(value), angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withHitbox(String value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, Optional.ofNullable(value), animation, swingSound,
                rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withSwingSound(String value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, Optional.ofNullable(value),
                rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour,
                damageType).withScaleAnimation(scaleAnimation);
    }

    public AttackOverride withDamageType(String value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound,
                rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour,
                value == null || value.isEmpty() ? Optional.empty() : Optional.of(value))
                .withScaleAnimation(scaleAnimation);
    }

    public AttackOverride withAngle(double value) {
        return new AttackOverride(damageMultiplier, Optional.of(value), upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withUpswing(double value) {
        return new AttackOverride(damageMultiplier, angle, Optional.of(value), rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withRangeMultiplier(float value) {
        return new AttackOverride(damageMultiplier, angle, upswing, Optional.of(value),
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withMovementSpeedMultiplier(float value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                Optional.of(value), hitbox, animation, swingSound, rarity, costOverride, disabled,
                attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withCooldownScale(AttackAttribute value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, ignoreAffinity, Optional.ofNullable(value), hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withCostOverride(Double value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity,
                Optional.ofNullable(value),
                disabled, attributes, ignoreAffinity, cooldownScale, hastensNext, drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withHastensNext(AttackAttribute value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, ignoreAffinity, cooldownScale, Optional.ofNullable(value),
                drag, behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withDrag(AttackDrag value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, ignoreAffinity, cooldownScale, hastensNext,
                Optional.of(value), behaviour, damageType, affixes, affixTuning, scaleAnimation);
    }

    public AttackOverride withScaleAnimation(Optional<Boolean> value) {
        return new AttackOverride(damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, hitbox, animation, swingSound, rarity, costOverride,
                disabled, attributes, ignoreAffinity, cooldownScale, hastensNext, drag,
                behaviour, damageType, affixes, affixTuning, value);
    }

    public boolean scalesAnimation() {
        return scaleAnimation.orElse(Boolean.FALSE);
    }

    public AttackDrag dragOrDefault() {
        return drag.orElse(AttackDrag.NONE);
    }

    public boolean isDisabled() {
        return disabled.orElse(Boolean.FALSE);
    }

    public AttackOverride withoutMatching(AttackOverride reference) {
        JsonObject mine = encoded(this);
        JsonObject theirs = encoded(reference);
        if (mine == null || theirs == null) {
            return this;
        }
        for (String key : List.copyOf(mine.keySet())) {
            if (mine.get(key).equals(theirs.get(key))) {
                mine.remove(key);
            }
        }
        return CODEC.parse(JsonOps.INSTANCE, mine).result().orElse(this);
    }

    public List<String> differencesFrom(AttackOverride reference) {
        JsonObject mine = encoded(this);
        JsonObject theirs = encoded(reference);
        List<String> lines = new ArrayList<>();
        if (mine == null) {
            return lines;
        }
        for (String key : mine.keySet()) {
            JsonElement other = theirs == null ? null : theirs.get(key);
            if (!mine.get(key).equals(other)) {
                lines.add(key + " " + mine.get(key) + DIFF_ARROW
                        + (other == null ? DIFF_NONE : other.toString()));
            }
        }
        return lines;
    }

    public boolean isBlank() {
        JsonObject json = encoded(this);
        return json == null || json.size() == 0;
    }

    private static JsonObject encoded(AttackOverride override) {
        return override == null
                ? null
                : CODEC.encodeStart(JsonOps.INSTANCE, override).result()
                        .filter(JsonElement::isJsonObject)
                        .map(JsonElement::getAsJsonObject)
                        .orElse(null);
    }
}
