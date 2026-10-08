package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record HitboxSet(Resolve resolve, List<Shape> shapes) {

    public static final HitboxSet NONE = new HitboxSet(Resolve.UNION, List.of());

    public static final Codec<HitboxSet> CODEC = Customs.wrap(HitboxSet.class, RecordCodecBuilder.create(
            instance -> instance.group(
                    Resolve.CODEC.optionalFieldOf("resolve", Resolve.UNION)
                            .forGetter(HitboxSet::resolve),
                    Shape.CODEC.listOf().optionalFieldOf("shapes", List.of())
                            .forGetter(HitboxSet::shapes)
            ).apply(instance, HitboxSet::new)));

    public HitboxSet {
        shapes = shapes == null ? List.of() : List.copyOf(shapes);
    }

    public boolean isEmpty() {
        return shapes.isEmpty();
    }

    public boolean usesPriority() {
        return resolve == Resolve.PRIORITY;
    }

    public boolean staggered() {
        if (shapes.size() < 2) {
            return !shapes.isEmpty() && shapes.getFirst().clampedDelay() > 0;
        }
        int first = shapes.getFirst().clampedDelay();
        for (Shape shape : shapes) {
            if (shape.clampedDelay() != first) {
                return true;
            }
        }
        return first > 0;
    }

    public List<Integer> delaySteps() {
        List<Integer> steps = new ArrayList<>();
        for (Shape shape : shapes) {
            int delay = shape.clampedDelay();
            if (!steps.contains(delay)) {
                steps.add(delay);
            }
        }
        Collections.sort(steps);
        return steps;
    }

    public HitboxSet atDelay(int delay) {
        List<Shape> group = new ArrayList<>();
        for (Shape shape : shapes) {
            if (shape.clampedDelay() == delay) {
                group.add(shape);
            }
        }
        return group.isEmpty() ? NONE : new HitboxSet(resolve, group);
    }

    public double reach() {
        double furthest = 0.0D;
        for (Shape shape : shapes) {
            furthest = Math.max(furthest, reachOf(shape));
        }
        return furthest;
    }

    public static double reachOf(Shape shape) {
        HitboxOffset offset = shape.offset().clamped();
        double centre = Math.sqrt(offset.forward() * offset.forward()
                + offset.up() * offset.up()
                + offset.right() * offset.right());
        Vec3 size = sizeOf(shape);
        return centre + size.length() / 2.0D;
    }

    public static Vec3 centreOf(Shape shape, Vec3 origin, float bodyYaw) {
        HitboxOffset offset = shape.offset().clamped();
        float yaw = (bodyYaw + offset.yaw()) * ((float) Math.PI / 180.0F);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return origin.add(
                offset.forward() * -sin + offset.right() * cos,
                offset.up(),
                offset.forward() * cos + offset.right() * sin);
    }

    public static Vec3 sizeOf(Shape shape) {
        return new Vec3(shape.clampedWidth(), shape.clampedHeight(), shape.clampedDepth());
    }

    public static float yawOf(Shape shape, float bodyYaw) {
        return bodyYaw + shape.offset().clamped().yaw();
    }

    public static float pitchOf(Shape shape, float bodyPitch) {
        return bodyPitch + shape.offset().clamped().pitch();
    }

    public String signature() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("h").append(resolve.getSerializedName());
        for (Shape shape : shapes) {
            builder.append(shape.signature());
        }
        return builder.toString();
    }

    public record Shape(HitboxOffset offset, int priority, Optional<DamageSpec> damageSpec,
                        float width, float height, float depth,
                        List<String> affixes, Map<String, AffixTuning> tuning,
                        Optional<AttackOverride> override, int delayTicks) {

        public static final float DEFAULT_SIZE = 1.5F;
        public static final float MAX_SIZE = 16.0F;
        public static final int MAX_DELAY = 60;

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

        public Shape(HitboxOffset offset, int priority, Optional<DamageSpec> damageSpec) {
            this(offset, priority, damageSpec, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        public Shape(HitboxOffset offset, int priority, Optional<DamageSpec> damageSpec,
                     float width, float height, float depth) {
            this(offset, priority, damageSpec, width, height, depth, List.of(), Map.of(),
                    Optional.empty());
        }

        public Shape(HitboxOffset offset, int priority, Optional<DamageSpec> damageSpec,
                     float width, float height, float depth,
                     List<String> affixes, Map<String, AffixTuning> tuning) {
            this(offset, priority, damageSpec, width, height, depth, affixes, tuning,
                    Optional.empty(), 0);
        }

        public Shape(HitboxOffset offset, int priority, Optional<DamageSpec> damageSpec,
                     float width, float height, float depth,
                     List<String> affixes, Map<String, AffixTuning> tuning,
                     Optional<AttackOverride> override) {
            this(offset, priority, damageSpec, width, height, depth, affixes, tuning,
                    override, 0);
        }

        public int clampedDelay() {
            return Math.clamp(delayTicks, 0, MAX_DELAY);
        }

        public Shape withAffixes(List<String> chosen, Map<String, AffixTuning> tuned) {
            return new Shape(offset, priority, damageSpec, width, height, depth, chosen, tuned,
                    override, delayTicks);
        }

        public Shape withOverride(Optional<AttackOverride> value) {
            return new Shape(offset, priority, damageSpec, width, height, depth, affixes, tuning,
                    value, delayTicks);
        }

        public Shape withOffset(HitboxOffset value) {
            return new Shape(value, priority, damageSpec, width, height, depth, affixes, tuning,
                    override, delayTicks);
        }

        public Shape withSize(float newWidth, float newHeight, float newDepth) {
            return new Shape(offset, priority, damageSpec, newWidth, newHeight, newDepth,
                    affixes, tuning, override, delayTicks);
        }

        public Shape withPriority(int value) {
            return new Shape(offset, value, damageSpec, width, height, depth, affixes, tuning,
                    override, delayTicks);
        }

        public Shape withDelay(int value) {
            return new Shape(offset, priority, damageSpec, width, height, depth, affixes, tuning,
                    override, value);
        }

        public AttackOverride overrideOrEmpty() {
            return override.orElse(AttackOverride.EMPTY);
        }

        public Shape withDamageSpec(Optional<DamageSpec> spec) {
            return new Shape(offset, priority, spec, width, height, depth, affixes, tuning,
                    override, delayTicks);
        }

        public float clampedWidth() {
            return Math.clamp(width, 0.1F, MAX_SIZE);
        }

        public float clampedHeight() {
            return Math.clamp(height, 0.1F, MAX_SIZE);
        }

        public float clampedDepth() {
            return Math.clamp(depth, 0.1F, MAX_SIZE);
        }

        public static final Codec<Shape> CODEC = Customs.wrap(Shape.class, RecordCodecBuilder.create(
                instance -> instance.group(
                        HitboxOffset.CODEC.optionalFieldOf("offset", HitboxOffset.NONE)
                                .forGetter(Shape::offset),
                        Codec.INT.optionalFieldOf("priority", 0).forGetter(Shape::priority),
                        DamageSpec.CODEC.optionalFieldOf("damage_spec")
                                .forGetter(Shape::damageSpec),
                        Codec.FLOAT.optionalFieldOf("width", DEFAULT_SIZE)
                                .forGetter(Shape::width),
                        Codec.FLOAT.optionalFieldOf("height", DEFAULT_SIZE)
                                .forGetter(Shape::height),
                        Codec.FLOAT.optionalFieldOf("depth", DEFAULT_SIZE)
                                .forGetter(Shape::depth),
                        Codec.STRING.listOf().optionalFieldOf("affixes", List.of())
                                .forGetter(Shape::affixes),
                        AffixTuning.MAP_CODEC.optionalFieldOf("tuning", Map.of())
                                .forGetter(Shape::tuning),
                        OVERRIDE_CODEC.optionalFieldOf("override")
                                .forGetter(Shape::override),
                        Codec.INT.optionalFieldOf("delay_ticks", 0)
                                .forGetter(Shape::delayTicks)
                ).apply(instance, Shape::new)));

        public Shape {
            offset = offset == null ? HitboxOffset.NONE : offset;
            affixes = affixes == null ? List.of() : List.copyOf(affixes);
            tuning = tuning == null ? Map.of() : Map.copyOf(tuning);
        }

        public boolean tunes() {
            return damageSpec.isPresent() || !affixes.isEmpty() || override.isPresent();
        }

        public String signature() {
            return "s" + offset.signature() + priority
                    + (damageSpec.isPresent() ? "d" : "")
                    + String.join(",", affixes) + tuning.hashCode()
                    + override.map(AttackOverride::signature).orElse("")
                    + (delayTicks == 0 ? "" : "t" + delayTicks);
        }
    }

    public enum Resolve implements StringRepresentable {

        UNION("union"),
        PRIORITY("priority");

        public static final List<Resolve> ALL = List.of(values());

        public static final Codec<Resolve> CODEC = StringRepresentable.fromEnum(Resolve::values);

        private final String name;

        Resolve(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
