package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

import java.util.Map;
import java.util.Optional;

public record AbilityTuning(Damage damage, Timing timing, Shape shape, Presentation fx,
                            Map<String, Double> extra) {

    public record Damage(Optional<Float> baseDamage,
                         Optional<Float> weaponDamageRatio,
                         Optional<Double> perPoint,
                         Optional<String> scalesWith,
                         Optional<String> school,
                         Optional<Integer> hits,
                         Optional<String> applies,
                         Optional<Integer> appliesStacks,
                         Optional<Float> appliesChance,
                         Optional<Float> lifesteal,
                         Optional<Boolean> detonates) {

        public static final Damage NONE = new Damage(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());

        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("base_damage").forGetter(Damage::baseDamage),
                Codec.FLOAT.optionalFieldOf("weapon_damage_ratio").forGetter(Damage::weaponDamageRatio),
                Codec.DOUBLE.optionalFieldOf("per_point").forGetter(Damage::perPoint),
                Codec.STRING.optionalFieldOf("scales_with").forGetter(Damage::scalesWith),
                Codec.STRING.optionalFieldOf("school").forGetter(Damage::school),
                Codec.INT.optionalFieldOf("hits").forGetter(Damage::hits),
                Codec.STRING.optionalFieldOf("applies").forGetter(Damage::applies),
                Codec.INT.optionalFieldOf("applies_stacks").forGetter(Damage::appliesStacks),
                Codec.FLOAT.optionalFieldOf("applies_chance").forGetter(Damage::appliesChance),
                Codec.FLOAT.optionalFieldOf("lifesteal").forGetter(Damage::lifesteal),
                Codec.BOOL.optionalFieldOf("detonates").forGetter(Damage::detonates)
        ).apply(instance, Damage::new));
    }

    public record Timing(Optional<Integer> durationTicks, Optional<Integer> windupTicks) {

        public static final Timing NONE = new Timing(Optional.empty(), Optional.empty());

        public static final Codec<Timing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("duration_ticks").forGetter(Timing::durationTicks),
                Codec.INT.optionalFieldOf("windup_ticks").forGetter(Timing::windupTicks)
        ).apply(instance, Timing::new));
    }

    public record Shape(Optional<Double> radius, Optional<Double> range, Optional<Double> rise,
                        Optional<Double> pull, Optional<Double> lift,
                        Optional<Integer> maxTargets) {

        public static final Shape NONE = new Shape(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Shape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius").forGetter(Shape::radius),
                Codec.DOUBLE.optionalFieldOf("range").forGetter(Shape::range),
                Codec.DOUBLE.optionalFieldOf("rise").forGetter(Shape::rise),
                Codec.DOUBLE.optionalFieldOf("pull").forGetter(Shape::pull),
                Codec.DOUBLE.optionalFieldOf("lift").forGetter(Shape::lift),
                Codec.INT.optionalFieldOf("max_targets").forGetter(Shape::maxTargets)
        ).apply(instance, Shape::new));
    }

    public record Presentation(Optional<Integer> particles, Optional<Double> spread,
                               Optional<Float> volume, Optional<Float> pitch) {

        public static final Presentation NONE = new Presentation(Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Presentation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("particles").forGetter(Presentation::particles),
                Codec.DOUBLE.optionalFieldOf("spread").forGetter(Presentation::spread),
                Codec.FLOAT.optionalFieldOf("volume").forGetter(Presentation::volume),
                Codec.FLOAT.optionalFieldOf("pitch").forGetter(Presentation::pitch)
        ).apply(instance, Presentation::new));
    }

    public static final Codec<AbilityTuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Damage.CODEC.optionalFieldOf("damage", Damage.NONE).forGetter(AbilityTuning::damage),
            Timing.CODEC.optionalFieldOf("timing", Timing.NONE).forGetter(AbilityTuning::timing),
            Shape.CODEC.optionalFieldOf("shape", Shape.NONE).forGetter(AbilityTuning::shape),
            Presentation.CODEC.optionalFieldOf("fx", Presentation.NONE).forGetter(AbilityTuning::fx),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("extra", Map.of())
                    .forGetter(AbilityTuning::extra)
    ).apply(instance, AbilityTuning::new));

    public AbilityTuning {
        extra = extra == null ? Map.of() : Map.copyOf(extra);
    }

    public AbilityConfig.Entry over(AbilityConfig.Entry base) {
        AbilityConfig.Entry merged = base.copy();

        damage.baseDamage().ifPresent(value -> merged.damage.baseDamage = value);
        damage.weaponDamageRatio().ifPresent(value -> merged.damage.weaponDamageRatio = value);
        damage.perPoint().ifPresent(value -> merged.damage.perPoint = value);
        damage.scalesWith().ifPresent(value -> merged.damage.scalesWith = value);
        damage.school().ifPresent(value -> merged.damage.school = value);
        damage.hits().ifPresent(value -> merged.damage.hits = value);
        damage.detonates().ifPresent(value -> merged.damage.detonates = value);
        damage.applies().ifPresent(value -> merged.damage.applies = value);
        damage.appliesStacks().ifPresent(value -> merged.damage.appliesStacks = value);
        damage.appliesChance().ifPresent(value -> merged.damage.appliesChance = value);
        damage.lifesteal().ifPresent(value -> merged.damage.lifesteal = value);

        timing.durationTicks().ifPresent(value -> merged.timing.durationTicks = value);
        timing.windupTicks().ifPresent(value -> merged.timing.windupTicks = value);

        shape.radius().ifPresent(value -> merged.shape.radius = value);
        shape.range().ifPresent(value -> merged.shape.range = value);
        shape.rise().ifPresent(value -> merged.shape.rise = value);
        shape.pull().ifPresent(value -> merged.shape.pull = value);
        shape.lift().ifPresent(value -> merged.shape.lift = value);
        shape.maxTargets().ifPresent(value -> merged.shape.maxTargets = value);

        fx.particles().ifPresent(value -> merged.fx.particles = value);
        fx.spread().ifPresent(value -> merged.fx.spread = value);
        fx.volume().ifPresent(value -> merged.fx.volume = value);
        fx.pitch().ifPresent(value -> merged.fx.pitch = value);

        for (Map.Entry<String, Double> option : extra.entrySet()) {
            merged.with(option.getKey(), option.getValue());
        }

        return merged.sane();
    }
}
