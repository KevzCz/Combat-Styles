package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record AttackBehaviour(Optional<AffixEffects.Motion> motion,
                              Optional<HitboxOffset> hitboxOffset,
                              List<SlashPlacement> slashes,
                              List<ExtraAttack> extras,
                              Optional<AttackPlacement> placement,
                              Optional<HitboxSet> boxes,
                              Optional<AttackTrail> trail) {

    public static final AttackBehaviour NONE =
            new AttackBehaviour(Optional.empty(), Optional.empty(), List.of(), List.of(),
                    Optional.empty(), Optional.empty());

    public static final Codec<AttackBehaviour> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AffixEffects.Motion.CODEC.optionalFieldOf("motion").forGetter(AttackBehaviour::motion),
            HitboxOffset.CODEC.optionalFieldOf("hitbox_offset").forGetter(AttackBehaviour::hitboxOffset),
            SlashPlacement.CODEC.listOf().optionalFieldOf("slashes", List.of())
                    .forGetter(AttackBehaviour::slashes),
            ExtraAttack.CODEC.listOf().optionalFieldOf("extras", List.of())
                    .forGetter(AttackBehaviour::extras),
            AttackPlacement.CODEC.optionalFieldOf("placement")
                    .forGetter(AttackBehaviour::placement),
            HitboxSet.CODEC.optionalFieldOf("boxes").forGetter(AttackBehaviour::boxes),
            AttackTrail.CODEC.optionalFieldOf("trail").forGetter(AttackBehaviour::trail)
    ).apply(instance, AttackBehaviour::new));

    public AttackBehaviour(Optional<AffixEffects.Motion> motion,
                           Optional<HitboxOffset> hitboxOffset,
                           List<SlashPlacement> slashes,
                           List<ExtraAttack> extras,
                           Optional<AttackPlacement> placement,
                           Optional<HitboxSet> boxes) {
        this(motion, hitboxOffset, slashes, extras, placement, boxes, Optional.empty());
    }

    public AttackBehaviour {
        slashes = slashes == null ? List.of() : List.copyOf(slashes);
        extras = extras == null ? List.of() : List.copyOf(extras);
    }

    public AttackBehaviour(Optional<AffixEffects.Motion> motion) {
        this(motion, Optional.empty(), List.of(), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public AttackBehaviour(Optional<AffixEffects.Motion> motion,
                           Optional<HitboxOffset> hitboxOffset,
                           List<SlashPlacement> slashes) {
        this(motion, hitboxOffset, slashes, List.of(), Optional.empty(), Optional.empty());
    }

    public AttackBehaviour(Optional<AffixEffects.Motion> motion,
                           Optional<HitboxOffset> hitboxOffset,
                           List<SlashPlacement> slashes,
                           List<ExtraAttack> extras) {
        this(motion, hitboxOffset, slashes, extras, Optional.empty(), Optional.empty());
    }

    public boolean isEmpty() {
        return (motion.isEmpty() || motion.get().isEmpty())
                && offsetOrNone().isEmpty()
                && slashes.isEmpty()
                && extras.isEmpty()
                && placementOrNone().isEmpty()
                && boxesOrNone().isEmpty();
    }

    public AffixEffects.Motion motionOrNone() {
        return motion.orElse(AffixEffects.Motion.NONE);
    }

    public HitboxOffset offsetOrNone() {
        return hitboxOffset.orElse(HitboxOffset.NONE);
    }

    public AttackPlacement placementOrNone() {
        return placement.orElse(AttackPlacement.NONE);
    }

    public HitboxSet boxesOrNone() {
        return boxes.orElse(HitboxSet.NONE);
    }

    public AttackBehaviour mergedWith(AttackBehaviour other, MergeMode mode) {
        if (other == null) {
            return this;
        }
        if (mode == null || mode == MergeMode.REPLACE) {
            return other;
        }
        List<SlashPlacement> allSlashes = new ArrayList<>(slashes);
        allSlashes.addAll(other.slashes());
        List<ExtraAttack> allExtras = new ArrayList<>(extras);
        allExtras.addAll(other.extras());
        return new AttackBehaviour(other.motion().or(this::motion),
                other.hitboxOffset().or(this::hitboxOffset),
                allSlashes, allExtras,
                other.placement().or(this::placement),
                mergedBoxes(boxesOrNone(), other.boxesOrNone()));
    }

    private static Optional<HitboxSet> mergedBoxes(HitboxSet parent, HitboxSet own) {
        if (parent.isEmpty()) {
            return own.isEmpty() ? Optional.empty() : Optional.of(own);
        }
        if (own.isEmpty()) {
            return Optional.of(parent);
        }
        List<HitboxSet.Shape> all = new ArrayList<>(parent.shapes());
        all.addAll(own.shapes());
        return Optional.of(new HitboxSet(own.resolve(), all));
    }

    public AttackBehaviour withHitboxOffset(HitboxOffset value) {
        return new AttackBehaviour(motion,
                value == null || value.isEmpty() ? Optional.empty() : Optional.of(value),
                slashes, extras, placement, boxes);
    }

    public AttackBehaviour withSlashes(List<SlashPlacement> value) {
        return new AttackBehaviour(motion, hitboxOffset, value, extras, placement, boxes);
    }

    public AttackBehaviour withExtras(List<ExtraAttack> value) {
        return new AttackBehaviour(motion, hitboxOffset, slashes, value, placement, boxes);
    }

    public AttackBehaviour withBoxes(HitboxSet value) {
        return new AttackBehaviour(motion, hitboxOffset, slashes, extras, placement,
                value == null || value.isEmpty() ? Optional.empty() : Optional.of(value));
    }

    public String signature() {
        AffixEffects.Motion value = motionOrNone();
        StringBuilder builder = new StringBuilder();
        if (!value.isEmpty()) {
            builder.append("m")
                    .append(value.dash().map(dash -> "d" + dash.forward() + "_" + dash.upward()
                            + (dash.towardTarget() ? "t" : "")).orElse(""))
                    .append(value.blink().map(blink -> "b" + blink.distance()
                            + (blink.behindTarget() ? "t" : "")).orElse(""))
                    .append(value.delayTicks() > 0 ? "@" + value.delayTicks() : "");
        }
        builder.append(offsetOrNone().signature());
        builder.append(placementOrNone().signature());
        builder.append(boxesOrNone().signature());
        for (SlashPlacement slash : slashes) {
            builder.append(slash.signature());
        }
        for (ExtraAttack hit : extras) {
            builder.append(hit.signature());
        }
        return builder.toString();
    }
}
