package net.pixeldreamstudios.combat_styles.damage;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record ScaledNumber(float base, List<DamageSpec.ScalingEntry> by, DamageSpec.ScalingMode mode,
                           Optional<Float> min, Optional<Float> max, Optional<Float> step,
                           Optional<Crit> crit) {

    public static final String KEY = "scaled";
    public static final String KEY_BY = "by";
    public static final String KEY_MODE = "scaling_mode";
    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";
    public static final String KEY_STEP = "step";
    public static final String KEY_CRIT = "crit";
    public static final String SCHEMA = "@scaled_number";
    private static final float NO_STEP = 0.0F;

    public record Crit(Optional<String> group, Optional<ScaledNumber> chance,
                       Optional<ScaledNumber> value, Optional<Mode> mode,
                       Optional<AffixEffects.OnHit> onCrit, Optional<AffixEffects.OnHit> onNoCrit) {

        public enum Mode {
            MULTIPLY("multiply"),
            ADD_VALUE("add_value");

            public static final Codec<Mode> CODEC = Codec.STRING.xmap(Mode::byName, Mode::serializedName);

            private final String serializedName;

            Mode(String serializedName) {
                this.serializedName = serializedName;
            }

            public String serializedName() {
                return serializedName;
            }

            public static Mode byName(String name) {
                for (Mode mode : values()) {
                    if (mode.serializedName.equalsIgnoreCase(name)) {
                        return mode;
                    }
                }
                return MULTIPLY;
            }

            public static List<String> names() {
                return Arrays.stream(values()).map(Mode::serializedName).toList();
            }
        }

        public static final String KEY_GROUP = "group";
        public static final String KEY_CHANCE = "chance";
        public static final String KEY_VALUE = "value";
        public static final String KEY_LEGACY_MULTIPLIER = "multiplier";
        public static final String KEY_MODE = "mode";
        public static final String KEY_ON_CRIT = "on_crit";
        public static final String KEY_ON_NO_CRIT = "on_no_crit";
        public static final float NO_CHANCE = 0.0F;
        public static final float DEFAULT_CHANCE = 0.1F;
        public static final float DEFAULT_MULTIPLIER = 2.0F;
        public static final float NO_ADDITION = 0.0F;
        public static final Crit SAMPLE = new Crit(Optional.empty(), Optional.of(plain(DEFAULT_CHANCE)),
                Optional.of(plain(DEFAULT_MULTIPLIER)), Optional.empty(), Optional.empty(), Optional.empty());
        public static final Codec<Crit> CODEC = Codec.lazyInitialized(
                () -> codec(valueCodec(ScaledNumber.CODEC)));

        private static Codec<Crit> codec(Codec<ScaledNumber> value) {
            return RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf(KEY_GROUP).forGetter(Crit::group),
                    value.optionalFieldOf(KEY_CHANCE).forGetter(Crit::chance),
                    value.optionalFieldOf(KEY_VALUE).forGetter(Crit::value),
                    value.optionalFieldOf(KEY_LEGACY_MULTIPLIER).forGetter(crit -> Optional.<ScaledNumber>empty()),
                    Mode.CODEC.optionalFieldOf(KEY_MODE).forGetter(Crit::mode),
                    Codec.lazyInitialized(() -> AffixEffects.OnHit.CODEC)
                            .optionalFieldOf(KEY_ON_CRIT).forGetter(Crit::onCrit),
                    Codec.lazyInitialized(() -> AffixEffects.OnHit.CODEC)
                            .optionalFieldOf(KEY_ON_NO_CRIT).forGetter(Crit::onNoCrit)
            ).apply(instance, (group, chance, amount, legacy, mode, onCrit, onNoCrit) ->
                    new Crit(group, chance, amount.or(() -> legacy), mode, onCrit, onNoCrit)));
        }

        public Crit {
            group = group == null ? Optional.empty() : group.filter(name -> !name.isBlank());
            chance = chance == null ? Optional.empty() : chance;
            value = value == null ? Optional.empty() : value;
            mode = mode == null ? Optional.empty() : mode;
            onCrit = onCrit == null ? Optional.empty() : onCrit;
            onNoCrit = onNoCrit == null ? Optional.empty() : onNoCrit;
        }

        public Crit effective() {
            Crit base = group.map(CritGroups::find).orElse(null);
            if (base == null) {
                return this;
            }
            return new Crit(group, chance.or(base::chance), value.or(base::value),
                    mode.or(base::mode), onCrit.or(base::onCrit), onNoCrit.or(base::onNoCrit));
        }

        public float chanceFor(Player player) {
            return effective().chance.map(value -> value.resolve(player)).orElse(NO_CHANCE);
        }

        public Mode modeOrDefault() {
            return effective().mode.orElse(Mode.MULTIPLY);
        }

        public float valueFor(Player player) {
            return effective().value.map(amount -> amount.resolve(player))
                    .orElse(modeOrDefault() == Mode.ADD_VALUE ? NO_ADDITION : DEFAULT_MULTIPLIER);
        }

        public float applyTo(float amount, Player player) {
            float critValue = valueFor(player);
            return modeOrDefault() == Mode.ADD_VALUE ? amount + critValue : amount * critValue;
        }

        public List<String> chanceSources() {
            return effective().chance.map(ScaledNumber::sources).orElse(List.of());
        }
    }

    public static final Codec<ScaledNumber> CODEC = Codec.recursive("scaled_number",
            self -> RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf(KEY).forGetter(ScaledNumber::base),
                    DamageSpec.ScalingEntry.CODEC.listOf().optionalFieldOf(KEY_BY, List.of())
                            .forGetter(ScaledNumber::by),
                    DamageSpec.ScalingMode.CODEC.optionalFieldOf(KEY_MODE, DamageSpec.ScalingMode.SUM)
                            .forGetter(ScaledNumber::mode),
                    Codec.FLOAT.optionalFieldOf(KEY_MIN).forGetter(ScaledNumber::min),
                    Codec.FLOAT.optionalFieldOf(KEY_MAX).forGetter(ScaledNumber::max),
                    Codec.FLOAT.optionalFieldOf(KEY_STEP).forGetter(ScaledNumber::step),
                    Crit.codec(valueCodec(self)).optionalFieldOf(KEY_CRIT).forGetter(ScaledNumber::crit)
            ).apply(instance, ScaledNumber::new)));

    public ScaledNumber {
        by = by == null ? List.of() : List.copyOf(by);
        mode = mode == null ? DamageSpec.ScalingMode.SUM : mode;
        min = min == null ? Optional.empty() : min;
        max = max == null ? Optional.empty() : max;
        step = step == null ? Optional.empty() : step;
        crit = crit == null ? Optional.empty() : crit;
    }

    private static Codec<ScaledNumber> valueCodec(Codec<ScaledNumber> self) {
        return Codec.either(Codec.FLOAT, self).xmap(
                either -> either.map(ScaledNumber::plain, number -> number),
                number -> number.isPlain() ? Either.left(number.base()) : Either.right(number));
    }

    public static ScaledNumber plain(float base) {
        return of(base, List.of());
    }

    public static ScaledNumber of(float base, List<DamageSpec.ScalingEntry> by) {
        return new ScaledNumber(base, by, DamageSpec.ScalingMode.SUM, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean isPlain() {
        return by.isEmpty() && min.isEmpty() && max.isEmpty() && step.isEmpty() && crit.isEmpty();
    }

    public static boolean isScaled(JsonElement element) {
        return element != null && element.isJsonObject() && element.getAsJsonObject().has(KEY)
                && element.getAsJsonObject().get(KEY).isJsonPrimitive()
                && element.getAsJsonObject().get(KEY).getAsJsonPrimitive().isNumber();
    }

    public static Optional<ScaledNumber> parse(JsonElement element) {
        return isScaled(element)
                ? CODEC.parse(JsonOps.INSTANCE, element).result()
                : Optional.empty();
    }

    public float resolve(Player player) {
        if (player == null || by.isEmpty()) {
            return clamped(base);
        }
        float bonus = 0.0F;
        for (DamageSpec.ScalingEntry entry : by) {
            float part = entry.bonusFor(player, base);
            bonus = mode == DamageSpec.ScalingMode.STRONGEST ? Math.max(bonus, part) : bonus + part;
        }
        float stepSize = step.orElse(NO_STEP);
        if (stepSize > NO_STEP) {
            bonus = (float) (Math.floor(bonus / stepSize) * stepSize);
        }
        return clamped(base + bonus);
    }

    public float roll(Player player, RandomSource random) {
        float value = resolve(player);
        if (crit.isEmpty() || random == null) {
            return value;
        }
        Crit rolled = crit.get().effective();
        float chance = rolled.chanceFor(player);
        boolean hit = CritRolls.outcome(rolled, () -> random.nextFloat() < chance);
        return hit ? rolled.applyTo(value, player) : value;
    }

    private float clamped(float value) {
        float low = min.map(bound -> Math.max(value, bound)).orElse(value);
        return max.map(bound -> Math.min(low, bound)).orElse(low);
    }

    public List<String> sources() {
        return by.stream().map(DamageSpec.ScalingEntry::source).distinct().toList();
    }
}
