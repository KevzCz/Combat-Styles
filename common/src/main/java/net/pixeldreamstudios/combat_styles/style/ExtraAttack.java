package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;

import java.util.Map;
import java.util.List;
import java.util.Optional;

public record ExtraAttack(int delayTicks, InheritGroups inherit, AttackOverride override,
                          SlashAnchor anchor, List<String> affixes,
                          Map<String, AffixTuning> tuning) {

    public ExtraAttack {
        anchor = anchor == null ? SlashAnchor.WEAPON : anchor;
        affixes = affixes == null ? List.of() : List.copyOf(affixes);
        tuning = tuning == null ? Map.of() : Map.copyOf(tuning);
    }

    public static final int MAX_DELAY = 60;
    public static final double DEFAULT_RATIO = 1.0D;
    private static final int DEFAULT_DELAY = 5;

    public static final ExtraAttack DEFAULT =
            new ExtraAttack(DEFAULT_DELAY, InheritGroups.ALL, null, SlashAnchor.WEAPON,
                    List.of(), Map.of());

    private static final Codec<AttackOverride> OVERRIDE_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AttackOverride, T>> decode(DynamicOps<T> ops, T input) {
            return AttackOverride.CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AttackOverride value, DynamicOps<T> ops, T prefix) {
            return AttackOverride.CODEC.encode(value, ops, prefix);
        }
    };

    public static final Codec<ExtraAttack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("delay_ticks", DEFAULT_DELAY).forGetter(ExtraAttack::delayTicks),
            InheritGroups.CODEC.optionalFieldOf("inherit", InheritGroups.ALL)
                    .forGetter(ExtraAttack::inherit),
            OVERRIDE_CODEC.optionalFieldOf("override")
                    .forGetter(hit -> Optional.ofNullable(hit.override())),
            SlashAnchor.CODEC.optionalFieldOf("anchor", SlashAnchor.WEAPON)
                    .forGetter(ExtraAttack::anchor),
            Codec.STRING.listOf().optionalFieldOf("affixes", List.of())
                    .forGetter(ExtraAttack::affixes),
            AffixTuning.MAP_CODEC.optionalFieldOf("tuning", Map.of())
                    .forGetter(ExtraAttack::tuning)
    ).apply(instance, (delay, inherit, override, anchor, affixes, tuning) ->
            new ExtraAttack(delay, inherit, override.orElse(null), anchor, affixes, tuning)));

    public AttackOverride overrideOrEmpty() {
        return override == null ? AttackOverride.EMPTY : override;
    }

    public ExtraAttack withDelay(int value) {
        return new ExtraAttack(value, inherit, override, anchor, affixes, tuning);
    }

    public ExtraAttack withOverride(AttackOverride value) {
        return new ExtraAttack(delayTicks, inherit, value, anchor, affixes, tuning);
    }

    public ExtraAttack withInherit(InheritGroups value) {
        return new ExtraAttack(delayTicks, value == null ? InheritGroups.ALL : value, override, anchor, affixes, tuning);
    }

    public ExtraAttack toggleInherit(String group) {
        return withInherit(inheritOrAll().toggle(group));
    }

    public ExtraAttack withAnchor(SlashAnchor value) {
        return new ExtraAttack(delayTicks, inherit, override,
                value == null ? SlashAnchor.WEAPON : value, affixes, tuning);
    }

    public ExtraAttack withAffixes(List<String> value) {
        return new ExtraAttack(delayTicks, inherit, override, anchor, value, tuning);
    }

    public HitboxSet boxes() {
        return overrideOrEmpty().behaviourOrDefault().boxesOrNone();
    }

    public ExtraAttack withBoxes(HitboxSet value) {
        AttackOverride base = overrideOrEmpty();
        AttackBehaviour behaviour = base.behaviourOrDefault();
        AttackBehaviour updated = new AttackBehaviour(behaviour.motion(),
                behaviour.hitboxOffset(), behaviour.slashes(), behaviour.extras(),
                behaviour.placement(),
                value == null || value.isEmpty() ? Optional.empty() : Optional.of(value));
        return new ExtraAttack(delayTicks, inherit, base.withBehaviour(updated),
                anchor, affixes, tuning);
    }

    public ExtraAttack withTuning(Map<String, AffixTuning> value) {
        return new ExtraAttack(delayTicks, inherit, override, anchor, affixes, value);
    }

    public InheritGroups inheritOrAll() {
        return inherit == null ? InheritGroups.ALL : inherit;
    }

    public AttackOverride asOverride() {
        return overrideOrEmpty();
    }

    public ExtraAttack fromOverride(AttackOverride applied) {
        return applied == null ? this : new ExtraAttack(delayTicks, inherit, applied, anchor, affixes, tuning);
    }

    public double damageRatio() {
        return overrideOrEmpty().damageMultiplier().orElse(DEFAULT_RATIO);
    }

    public Optional<HitboxOffset> hitboxOffset() {
        return overrideOrEmpty().behaviourOrDefault().hitboxOffset();
    }

    public Map<String, AttackAttribute> attributes() {
        return overrideOrEmpty().attributes();
    }

    public AttackOverride shapeFrom(AttackOverride parent) {
        AttackOverride own = overrideOrEmpty();
        InheritGroups groups = inheritOrAll();
        return groups.any() && parent != null
                ? parent.overriddenBy(own, groups)
                : own;
    }

    public String signature() {
        return "y" + delayTicks + inheritOrAll().signature() + overrideOrEmpty().signature()
                + anchor.getSerializedName() + String.join(",", affixes)
                + tuning.hashCode();
    }
}
