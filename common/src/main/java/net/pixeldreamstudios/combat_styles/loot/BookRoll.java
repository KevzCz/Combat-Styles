package net.pixeldreamstudios.combat_styles.loot;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.AttackDrag;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record BookRoll(StyleRarity rarity, int minAttacks, int maxAttacks,
                       List<Weighted> pool, List<String> categories,
                       float affixChance, int maxAffixes, boolean allowDuplicates,
                       int randomWeight, Rolled roll, Mode mode, int rollWeight,
                       List<WeightedAffix> affixPool, List<AffixSlot> affixPools) {

    public enum Mode {
        VARIANT,
        INJECT;

        public static final Codec<Mode> CODEC = Codec.STRING.xmap(
                raw -> "inject".equals(raw == null ? "" : raw.toLowerCase(Locale.ROOT))
                        ? INJECT
                        : VARIANT,
                mode -> mode.name().toLowerCase(Locale.ROOT));
    }

    private static final int DEFAULT_MIN = 1;
    private static final int DEFAULT_MAX = 3;
    private static final float DEFAULT_AFFIX_CHANCE = 0.0F;
    private static final int DEFAULT_MAX_AFFIXES = 1;
    private static final int DEFAULT_RANDOM_WEIGHT = 60;
    private static final int DEFAULT_ROLL_WEIGHT = 10;

    public record Range(double min, double max) {

        public static final Codec<Range> CODEC = Codec.either(
                        Codec.DOUBLE,
                        RecordCodecBuilder.<Range>create(instance -> instance.group(
                                Codec.DOUBLE.fieldOf("min").forGetter(Range::min),
                                Codec.DOUBLE.fieldOf("max").forGetter(Range::max)
                        ).apply(instance, Range::new)))
                .xmap(either -> either.map(fixed -> new Range(fixed, fixed), range -> range),
                        range -> range.min() == range.max()
                                ? Either.left(range.min())
                                : Either.right(range));

        public double sample(RandomSource random) {
            if (min >= max) {
                return min;
            }
            return min + random.nextDouble() * (max - min);
        }
    }

    public record RolledAttribute(Range amount, String operation, boolean stacks) {

        public static final Codec<RolledAttribute> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Range.CODEC.fieldOf("amount").forGetter(RolledAttribute::amount),
                        Codec.STRING.optionalFieldOf("operation", "add_value")
                                .forGetter(RolledAttribute::operation),
                        Codec.BOOL.optionalFieldOf("stacks", true).forGetter(RolledAttribute::stacks)
                ).apply(instance, RolledAttribute::new));

        public AttackAttribute sample(RandomSource random) {
            return new AttackAttribute(amount.sample(random),
                    AttackAttribute.operationOf(operation), stacks);
        }
    }

    public record RolledDrag(Optional<Range> ticks, Optional<Range> maxTargets,
                             Optional<Boolean> trail) {

        public static final Codec<RolledDrag> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Range.CODEC.optionalFieldOf("ticks").forGetter(RolledDrag::ticks),
                        Range.CODEC.optionalFieldOf("max_targets").forGetter(RolledDrag::maxTargets),
                        Codec.BOOL.optionalFieldOf("trail").forGetter(RolledDrag::trail)
                ).apply(instance, RolledDrag::new));

        public AttackDrag sample(RandomSource random) {
            return new AttackDrag(true,
                    ticks.map(range -> (int) Math.round(range.sample(random)))
                            .orElse(AttackDrag.DEFAULT_TICKS),
                    maxTargets.map(range -> (int) Math.round(range.sample(random)))
                            .orElse(AttackDrag.DEFAULT_MAX_TARGETS),
                    trail.orElse(Boolean.TRUE));
        }
    }

    public record Rolled(Optional<Range> damageMultiplier, Optional<Range> angle,
                         Optional<Range> upswing, Optional<Range> rangeMultiplier,
                         Optional<Range> movementSpeedMultiplier,
                         Optional<Range> cooldownScale, Optional<Range> hastensNext,
                         Optional<RolledDrag> drag,
                         Map<String, RolledAttribute> attributes) {

        public static final Rolled NONE = new Rolled(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Map.of());

        public static final Codec<Rolled> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Range.CODEC.optionalFieldOf("damage_multiplier").forGetter(Rolled::damageMultiplier),
                Range.CODEC.optionalFieldOf("angle").forGetter(Rolled::angle),
                Range.CODEC.optionalFieldOf("upswing").forGetter(Rolled::upswing),
                Range.CODEC.optionalFieldOf("range_multiplier").forGetter(Rolled::rangeMultiplier),
                Range.CODEC.optionalFieldOf("movement_speed_multiplier")
                        .forGetter(Rolled::movementSpeedMultiplier),
                Range.CODEC.optionalFieldOf("cooldown_scale").forGetter(Rolled::cooldownScale),
                Range.CODEC.optionalFieldOf("hastens_next").forGetter(Rolled::hastensNext),
                RolledDrag.CODEC.optionalFieldOf("drag").forGetter(Rolled::drag),
                Codec.unboundedMap(Codec.STRING, RolledAttribute.CODEC)
                        .optionalFieldOf("attributes", Map.of()).forGetter(Rolled::attributes)
        ).apply(instance, Rolled::new));

        public boolean isEmpty() {
            return damageMultiplier.isEmpty() && angle.isEmpty() && upswing.isEmpty()
                    && rangeMultiplier.isEmpty() && movementSpeedMultiplier.isEmpty()
                    && cooldownScale.isEmpty() && hastensNext.isEmpty() && drag.isEmpty()
                    && attributes.isEmpty();
        }

        public Rolled orElse(Rolled fallback) {
            if (fallback == null || fallback.isEmpty()) {
                return this;
            }
            return new Rolled(
                    damageMultiplier.or(fallback::damageMultiplier),
                    angle.or(fallback::angle),
                    upswing.or(fallback::upswing),
                    rangeMultiplier.or(fallback::rangeMultiplier),
                    movementSpeedMultiplier.or(fallback::movementSpeedMultiplier),
                    cooldownScale.or(fallback::cooldownScale),
                    hastensNext.or(fallback::hastensNext),
                    drag.or(fallback::drag),
                    attributes.isEmpty() ? fallback.attributes() : attributes);
        }

        public AttackOverride applyTo(AttackOverride base, RandomSource random) {
            AttackOverride result = base == null ? AttackOverride.EMPTY : base;
            if (damageMultiplier.isPresent()) {
                result = result.withDamageMultiplier(damageMultiplier.get().sample(random));
            }
            if (angle.isPresent()) {
                result = result.withAngle(angle.get().sample(random));
            }
            if (upswing.isPresent()) {
                result = result.withUpswing(upswing.get().sample(random));
            }
            if (rangeMultiplier.isPresent()) {
                result = result.withRangeMultiplier((float) rangeMultiplier.get().sample(random));
            }
            if (movementSpeedMultiplier.isPresent()) {
                result = result.withMovementSpeedMultiplier(
                        (float) movementSpeedMultiplier.get().sample(random));
            }
            if (cooldownScale.isPresent()) {
                result = result.withCooldownScale(
                        AttackAttribute.flat(cooldownScale.get().sample(random)));
            }
            if (hastensNext.isPresent()) {
                result = result.withHastensNext(
                        AttackAttribute.flat(hastensNext.get().sample(random)));
            }
            if (drag.isPresent()) {
                result = result.withDrag(drag.get().sample(random));
            }
            if (!attributes.isEmpty()) {
                Map<String, AttackAttribute> rolled = new LinkedHashMap<>(result.attributes());
                for (Map.Entry<String, RolledAttribute> entry : attributes.entrySet()) {
                    rolled.put(entry.getKey(), entry.getValue().sample(random));
                }
                result = result.withAttributes(rolled);
            }
            return result;
        }
    }

    public record AffixSlot(Optional<String> id, float chance, int min, int max,
                            List<WeightedAffix> affixes) {

        private static final float DEFAULT_CHANCE = 1.0F;

        public static final Codec<AffixSlot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("id").forGetter(AffixSlot::id),
                        Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE)
                                .forGetter(AffixSlot::chance),
                        Codec.INT.optionalFieldOf("min", 0).forGetter(AffixSlot::min),
                        Codec.INT.optionalFieldOf("max", 1).forGetter(AffixSlot::max),
                        WeightedAffix.CODEC.listOf().optionalFieldOf("affixes", List.of())
                                .forGetter(AffixSlot::affixes)
                ).apply(instance, AffixSlot::new));

        public AffixSlot {
            min = Math.max(0, min);
            max = Math.max(min, max);
            affixes = affixes == null ? List.of() : List.copyOf(affixes);
        }

        public AffixSlot withAffixes(List<WeightedAffix> merged) {
            return new AffixSlot(id, chance, min, max, merged);
        }
    }

    public record WeightedAffix(int weight, List<String> affixes) {

        public static final Codec<WeightedAffix> CODEC = Codec.either(
                        Codec.STRING,
                        RecordCodecBuilder.<WeightedAffix>create(instance -> instance.group(
                                Codec.INT.optionalFieldOf("weight", 1)
                                        .forGetter(WeightedAffix::weight),
                                Codec.STRING.listOf().fieldOf("affixes")
                                        .forGetter(WeightedAffix::affixes)
                        ).apply(instance, WeightedAffix::new)))
                .xmap(either -> either.map(id -> new WeightedAffix(1, List.of(id)), entry -> entry),
                        entry -> entry.weight() == 1 && entry.affixes().size() == 1
                                ? Either.left(entry.affixes().getFirst())
                                : Either.right(entry));
    }

    public record Weighted(int weight, StyleEntry entry, Rolled roll) {

        public static final Codec<Weighted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("weight", 1).forGetter(Weighted::weight),
                StyleEntry.CODEC.fieldOf("entry").forGetter(Weighted::entry),
                Rolled.CODEC.optionalFieldOf("roll", Rolled.NONE).forGetter(Weighted::roll)
        ).apply(instance, Weighted::new));
    }

    public static final Codec<BookRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StyleRarity.CODEC.optionalFieldOf("rarity", StyleRarity.COMMON).forGetter(BookRoll::rarity),
            Codec.INT.optionalFieldOf("min_attacks", DEFAULT_MIN).forGetter(BookRoll::minAttacks),
            Codec.INT.optionalFieldOf("max_attacks", DEFAULT_MAX).forGetter(BookRoll::maxAttacks),
            Weighted.CODEC.listOf().optionalFieldOf("pool", List.of()).forGetter(BookRoll::pool),
            Codec.STRING.listOf().optionalFieldOf("categories", List.of())
                    .forGetter(BookRoll::categories),
            Codec.FLOAT.optionalFieldOf("affix_chance", DEFAULT_AFFIX_CHANCE)
                    .forGetter(BookRoll::affixChance),
            Codec.INT.optionalFieldOf("max_affixes", DEFAULT_MAX_AFFIXES)
                    .forGetter(BookRoll::maxAffixes),
            Codec.BOOL.optionalFieldOf("allow_duplicates", false)
                    .forGetter(BookRoll::allowDuplicates),
            Codec.INT.optionalFieldOf("random_weight", DEFAULT_RANDOM_WEIGHT)
                    .forGetter(BookRoll::randomWeight),
            Rolled.CODEC.optionalFieldOf("roll", Rolled.NONE).forGetter(BookRoll::roll),
            Mode.CODEC.optionalFieldOf("mode", Mode.VARIANT).forGetter(BookRoll::mode),
            Codec.INT.optionalFieldOf("roll_weight", DEFAULT_ROLL_WEIGHT)
                    .forGetter(BookRoll::rollWeight),
            WeightedAffix.CODEC.listOf().optionalFieldOf("affix_pool", List.of())
                    .forGetter(BookRoll::declaredAffixPool),
            AffixSlot.CODEC.listOf().optionalFieldOf("affix_pools", List.of())
                    .forGetter(BookRoll::affixPools)
    ).apply(instance, BookRoll::new));

    public List<WeightedAffix> declaredAffixPool() {
        return affixPool;
    }

    public BookRoll withInjected(BookRoll injector) {
        List<Weighted> mergedPool = new ArrayList<>(pool);
        if (!stylePool().isEmpty()) {
            mergedPool.addAll(injector.stylePool());
        }

        List<WeightedAffix> current = affixPool();
        List<WeightedAffix> mergedAffixes = current.isEmpty()
                ? List.of()
                : merged(current, injector.affixPool());

        List<AffixSlot> mergedSlots = new ArrayList<>();
        for (AffixSlot slot : affixPools) {
            List<WeightedAffix> extra = injector.affixesForSlot(slot.id());
            mergedSlots.add(extra.isEmpty()
                    ? slot
                    : slot.withAffixes(merged(slot.affixes(), extra)));
        }

        return new BookRoll(rarity, minAttacks, maxAttacks,
                List.copyOf(stripAffixOnly(mergedPool)), categories,
                affixChance, maxAffixes, allowDuplicates, randomWeight, roll, mode,
                rollWeight, mergedAffixes, List.copyOf(mergedSlots));
    }

    private List<WeightedAffix> affixesForSlot(Optional<String> slotId) {
        if (slotId.isEmpty()) {
            return List.of();
        }
        for (AffixSlot slot : affixPools) {
            if (slotId.equals(slot.id())) {
                return slot.affixes();
            }
        }
        return List.of();
    }

    public List<Weighted> stylePool() {
        return stripAffixOnly(pool);
    }

    private static List<Weighted> stripAffixOnly(List<Weighted> entries) {
        List<Weighted> styles = new ArrayList<>();
        for (Weighted weighted : entries) {
            if (weighted.entry() == null || !weighted.entry().rawStyleId().isBlank()
                    || weighted.entry().customStyleId().isPresent()
                    || weighted.entry().affixIds().isEmpty()) {
                styles.add(weighted);
            }
        }
        return styles;
    }

    private static List<WeightedAffix> merged(List<WeightedAffix> base,
                                              List<WeightedAffix> extra) {
        List<WeightedAffix> all = new ArrayList<>(base);
        all.addAll(extra);
        return List.copyOf(all);
    }

    public List<WeightedAffix> affixPool() {
        List<WeightedAffix> found = new ArrayList<>(affixPool);
        for (Weighted weighted : pool) {
            if (weighted.entry() != null && weighted.entry().rawStyleId().isBlank()
                    && weighted.entry().customStyleId().isEmpty()
                    && !weighted.entry().affixIds().isEmpty()) {
                found.add(new WeightedAffix(weighted.weight(), weighted.entry().affixIds()));
            }
        }
        return found;
    }
}
