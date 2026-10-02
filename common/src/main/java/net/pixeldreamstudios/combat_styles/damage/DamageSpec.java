package net.pixeldreamstudios.combat_styles.damage;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record DamageSpec(float base, float ofHit, float ofTargetMaxHealth,
                         float ofTargetMissingHealth, float ofSelfMissingHealth,
                         Optional<String> damageType,
                         Optional<String> school,
                         List<ScalingEntry> scaling, ScalingMode scalingMode,
                         List<Share> proportional,
                         Optional<Float> minimum, Optional<Float> maximum,
                         float multiplier) {

    public static final float NO_BASE = 0.0F;
    public static final float NO_HIT_RATIO = 0.0F;
    public static final float NO_HEALTH_RATIO = 0.0F;
    public static final float FULL_MULTIPLIER = 1.0F;
    private static final float FULL_SHARE = 1.0F;

    public static final DamageSpec NONE = new DamageSpec(NO_BASE, NO_HIT_RATIO,
            NO_HEALTH_RATIO, NO_HEALTH_RATIO, NO_HEALTH_RATIO,
            Optional.empty(), Optional.empty(), List.of(), ScalingMode.SUM, List.of(),
            Optional.empty(), Optional.empty(), FULL_MULTIPLIER);

    private static final Codec<DamageSpec> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("base", NO_BASE).forGetter(DamageSpec::base),
            Codec.FLOAT.optionalFieldOf("of_hit", NO_HIT_RATIO).forGetter(DamageSpec::ofHit),
            Codec.FLOAT.optionalFieldOf("of_target_max_health", NO_HEALTH_RATIO)
                    .forGetter(DamageSpec::ofTargetMaxHealth),
            Codec.FLOAT.optionalFieldOf("of_target_missing_health", NO_HEALTH_RATIO)
                    .forGetter(DamageSpec::ofTargetMissingHealth),
            Codec.FLOAT.optionalFieldOf("of_self_missing_health", NO_HEALTH_RATIO)
                    .forGetter(DamageSpec::ofSelfMissingHealth),
            Codec.STRING.optionalFieldOf("type").forGetter(DamageSpec::damageType),
            Codec.STRING.optionalFieldOf("school").forGetter(DamageSpec::school),
            ScalingEntry.CODEC.listOf().optionalFieldOf("scaling", List.of())
                    .forGetter(DamageSpec::scaling),
            ScalingMode.CODEC.optionalFieldOf("scaling_mode", ScalingMode.SUM)
                    .forGetter(DamageSpec::scalingMode),
            Share.CODEC.listOf().optionalFieldOf("proportional", List.of())
                    .forGetter(DamageSpec::proportional),
            Codec.FLOAT.optionalFieldOf("minimum").forGetter(DamageSpec::minimum),
            Codec.FLOAT.optionalFieldOf("maximum").forGetter(DamageSpec::maximum),
            Codec.FLOAT.optionalFieldOf("multiplier", FULL_MULTIPLIER)
                    .forGetter(DamageSpec::multiplier)
    ).apply(instance, DamageSpec::new));

    public static final Codec<DamageSpec> CODEC = Codec.either(Codec.FLOAT, FULL)
            .xmap(either -> either.map(DamageSpec::ofFlat, spec -> spec),
                    spec -> spec.isFlatOnly()
                            ? Either.left(spec.base())
                            : Either.right(spec));

    public static float resolve(Optional<DamageSpec> spec, Player player,
                                float source, float fallbackRatio) {
        return spec.map(value -> value.amountFor(player, source))
                .orElseGet(() -> source * Math.max(0.0F, fallbackRatio));
    }

    public static DamageSpec ofFlat(float amount) {
        return new DamageSpec(amount, NO_HIT_RATIO, NO_HEALTH_RATIO, NO_HEALTH_RATIO,
                NO_HEALTH_RATIO, Optional.empty(), Optional.empty(),
                List.of(), ScalingMode.SUM, List.of(),
                Optional.empty(), Optional.empty(), FULL_MULTIPLIER);
    }

    public boolean isFlatOnly() {
        return ofHit == NO_HIT_RATIO && damageType.isEmpty() && school.isEmpty()
                && scaling.isEmpty() && proportional.isEmpty()
                && ofTargetMaxHealth == NO_HEALTH_RATIO
                && ofTargetMissingHealth == NO_HEALTH_RATIO
                && ofSelfMissingHealth == NO_HEALTH_RATIO
                && minimum.isEmpty() && maximum.isEmpty()
                && multiplier == FULL_MULTIPLIER;
    }

    public boolean isNeutral() {
        return isEmpty() && !retypes()
                && minimum.isEmpty() && maximum.isEmpty()
                && multiplier == FULL_MULTIPLIER;
    }

    public DamageSpec withDamageType(Optional<String> type) {
        return new DamageSpec(base, ofHit, ofTargetMaxHealth, ofTargetMissingHealth,
                ofSelfMissingHealth, type, school, scaling, scalingMode,
                proportional, minimum, maximum, multiplier);
    }

    public DamageSpec withAmounts(float newBase, float newOfHit, float newMultiplier) {
        return new DamageSpec(newBase, newOfHit, ofTargetMaxHealth, ofTargetMissingHealth,
                ofSelfMissingHealth, damageType, school, scaling, scalingMode,
                proportional, minimum, maximum, newMultiplier);
    }

    public boolean retypes() {
        return damageType.isPresent() || school.isPresent() || !proportional.isEmpty();
    }

    public boolean isEmpty() {
        return base == NO_BASE && ofHit == NO_HIT_RATIO && scaling.isEmpty()
                && ofTargetMaxHealth == NO_HEALTH_RATIO
                && ofTargetMissingHealth == NO_HEALTH_RATIO
                && ofSelfMissingHealth == NO_HEALTH_RATIO;
    }

    public float amountFor(Player player, float hitDamage) {
        return amountFor(player, hitDamage, null);
    }

    public float amountFor(Player player, float hitDamage, LivingEntity target) {
        float total = base + hitDamage * ofHit;
        if (target != null) {
            total += target.getMaxHealth() * ofTargetMaxHealth;
            total += (target.getMaxHealth() - target.getHealth()) * ofTargetMissingHealth;
        }
        if (player != null) {
            total += (player.getMaxHealth() - player.getHealth()) * ofSelfMissingHealth;
        }
        if (!scaling.isEmpty()) {
            total += scalingMode == ScalingMode.STRONGEST
                    ? strongestBonus(player, total)
                    : summedBonus(player, total);
        }
        total *= multiplier;

        float clamped = Math.max(minimum.orElse(0.0F), total);
        if (maximum.isPresent()) {
            clamped = Math.min(maximum.get(), clamped);
        }
        return Math.max(0.0F, clamped);
    }

    private float summedBonus(Player player, float total) {
        float bonus = 0.0F;
        for (ScalingEntry entry : scaling) {
            bonus += entry.bonusFor(player, total);
        }
        return bonus;
    }

    private float strongestBonus(Player player, float total) {
        float best = 0.0F;
        for (ScalingEntry entry : scaling) {
            best = Math.max(best, entry.bonusFor(player, total));
        }
        return best;
    }

    public List<Share> normalisedShares() {
        if (proportional.isEmpty()) {
            return List.of();
        }
        float sum = 0.0F;
        for (Share share : proportional) {
            sum += Math.max(0.0F, share.share());
        }
        if (sum <= 0.0F) {
            return List.of();
        }
        if (Math.abs(sum - FULL_SHARE) < 1.0E-4F) {
            return proportional;
        }
        List<Share> scaled = new ArrayList<>(proportional.size());
        for (Share share : proportional) {
            scaled.add(new Share(share.damageType(), Math.max(0.0F, share.share()) / sum));
        }
        return List.copyOf(scaled);
    }

    public enum ScalingMode {
        SUM("sum"),
        STRONGEST("strongest");

        public static final Codec<ScalingMode> CODEC = Codec.STRING.xmap(
                ScalingMode::byName, ScalingMode::serializedName);

        private final String serializedName;

        ScalingMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static ScalingMode byName(String name) {
            for (ScalingMode mode : values()) {
                if (mode.serializedName.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return SUM;
        }
    }

    public enum ScalingKind {
        PER_POINT("per_point"),
        MULTIPLIER("multiplier"),
        DIVISOR("divisor");

        public static final Codec<ScalingKind> CODEC = Codec.STRING.xmap(
                ScalingKind::byName, ScalingKind::serializedName);

        private final String serializedName;

        ScalingKind(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static ScalingKind byName(String name) {
            for (ScalingKind kind : values()) {
                if (kind.serializedName.equalsIgnoreCase(name)) {
                    return kind;
                }
            }
            return PER_POINT;
        }
    }

    public record ScalingEntry(String source, float perPoint, Optional<Float> cap,
                               ScalingKind mode, float baseline) {

        public static final float DEFAULT_BASELINE = 6.0F;

        public static final Codec<ScalingEntry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.STRING.fieldOf("source").forGetter(ScalingEntry::source),
                        Codec.FLOAT.optionalFieldOf("per_point", 0.0F)
                                .forGetter(ScalingEntry::perPoint),
                        Codec.FLOAT.optionalFieldOf("cap").forGetter(ScalingEntry::cap),
                        ScalingKind.CODEC.optionalFieldOf("mode", ScalingKind.PER_POINT)
                                .forGetter(ScalingEntry::mode),
                        Codec.FLOAT.optionalFieldOf("baseline", DEFAULT_BASELINE)
                                .forGetter(ScalingEntry::baseline)
                ).apply(instance, ScalingEntry::new));

        public float bonusFor(Player player, float total) {
            double value = SpellPowerAccess.valueOf(player, source);
            if (value <= 0.0D) {
                return 0.0F;
            }
            float bonus = switch (mode) {
                case MULTIPLIER -> (float) (total * value * perPoint);
                case DIVISOR -> baseline <= 0.0F
                        ? 0.0F
                        : (float) (total * (value / baseline) - total);
                default -> (float) (value * perPoint);
            };
            return cap.map(limit -> Math.min(bonus, limit)).orElse(bonus);
        }
    }

    public record Share(String damageType, float share) {
        public static final Codec<Share> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.STRING.fieldOf("type").forGetter(Share::damageType),
                        Codec.FLOAT.optionalFieldOf("share", FULL_SHARE).forGetter(Share::share)
                ).apply(instance, Share::new));
    }
}
