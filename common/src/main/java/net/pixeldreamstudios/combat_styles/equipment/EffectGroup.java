package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public record EffectGroup(float chance, List<Entry> entries,
                          Optional<Integer> picks,
                          Optional<Integer> minPicks,
                          Optional<Integer> maxPicks,
                          EquipmentTrigger trigger,
                          EffectCooldown cooldown,
                          String tooltipHeader,
                          Optional<Float> nothingChance) {

    public static final float ALWAYS = 1.0F;
    public static final int ALL_PICKS = 0;
    public static final float MIN_SHARE = 0.0F;
    private static final float PICK_ROLL_CEILING = 0.99999F;
    private static final int EXACT_MEMBER_LIMIT = 12;
    private static final Map<EffectGroup, Map<String, Double>> ODDS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public record Entry(String effect, float chance) {

        public static final Codec<Entry> CODEC = Customs.wrap(Entry.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("effect").forGetter(Entry::effect),
                Codec.FLOAT.optionalFieldOf("chance", ALWAYS).forGetter(Entry::chance)
        ).apply(instance, Entry::new)));

        public float share() {
            return Math.max(MIN_SHARE, chance);
        }
    }

    public static final Codec<EffectGroup> CODEC = Customs.wrap(EffectGroup.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("chance", ALWAYS).forGetter(EffectGroup::chance),
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of())
                    .forGetter(EffectGroup::entries),
            Codec.INT.optionalFieldOf("picks").forGetter(EffectGroup::picks),
            Codec.INT.optionalFieldOf("min_picks").forGetter(EffectGroup::minPicks),
            Codec.INT.optionalFieldOf("max_picks").forGetter(EffectGroup::maxPicks),
            EquipmentTrigger.CODEC.optionalFieldOf("trigger", EquipmentTrigger.ALWAYS)
                    .forGetter(EffectGroup::trigger),
            EffectCooldown.CODEC.optionalFieldOf("cooldown", EffectCooldown.NONE)
                    .forGetter(EffectGroup::cooldown),
            Codec.STRING.optionalFieldOf(TriggeredEffect.TOOLTIP_HEADER, "")
                    .forGetter(EffectGroup::tooltipHeader),
            Codec.FLOAT.optionalFieldOf("nothing_chance").forGetter(EffectGroup::nothingChance)
    ).apply(instance, EffectGroup::new)));

    public static final EffectGroup EMPTY = new EffectGroup(ALWAYS, List.of(),
            Optional.empty(), Optional.empty(), Optional.empty(),
            EquipmentTrigger.ALWAYS, EffectCooldown.NONE);

    public static final EffectGroup SAMPLE = new EffectGroup(0.5F,
            List.of(new Entry("", ALWAYS)),
            Optional.of(1), Optional.of(1), Optional.of(2),
            EquipmentTrigger.ALWAYS, EffectCooldown.NONE);

    public EffectGroup(float chance, List<Entry> entries, Optional<Integer> picks,
                       Optional<Integer> minPicks, Optional<Integer> maxPicks) {
        this(chance, entries, picks, minPicks, maxPicks,
                EquipmentTrigger.ALWAYS, EffectCooldown.NONE);
    }

    public EffectGroup(float chance, List<Entry> entries, Optional<Integer> picks,
                       Optional<Integer> minPicks, Optional<Integer> maxPicks,
                       EquipmentTrigger trigger, EffectCooldown cooldown) {
        this(chance, entries, picks, minPicks, maxPicks, trigger, cooldown, "");
    }

    public EffectGroup(float chance, List<Entry> entries, Optional<Integer> picks,
                       Optional<Integer> minPicks, Optional<Integer> maxPicks,
                       EquipmentTrigger trigger, EffectCooldown cooldown, String tooltipHeader) {
        this(chance, entries, picks, minPicks, maxPicks, trigger, cooldown, tooltipHeader,
                Optional.empty());
    }

    public EffectGroup {
        entries = entries == null ? List.of() : List.copyOf(entries);
        trigger = trigger == null ? EquipmentTrigger.ALWAYS : trigger;
        chance = trigger.chance().orElse(chance);
        cooldown = cooldown == null ? EffectCooldown.NONE : cooldown;
        tooltipHeader = tooltipHeader == null ? "" : tooltipHeader;
        nothingChance = nothingChance == null
                ? Optional.empty()
                : nothingChance.map(value -> Math.clamp(value, 0.0F, ALWAYS));
    }

    public boolean gated() {
        return !trigger.isAlways();
    }

    public boolean onCooldown() {
        return cooldown.ticks() > 0;
    }

    public boolean windowed() {
        return cooldown.windowed();
    }

    public boolean limited() {
        return cooldown.limited();
    }

    public int duration() {
        return cooldown.span();
    }

    public int uses() {
        return cooldown.uses();
    }

    public boolean holds(String effect) {
        for (Entry entry : entries) {
            if (entry.effect().equals(effect)) {
                return true;
            }
        }
        return false;
    }

    public Entry entryFor(String effect) {
        for (Entry entry : entries) {
            if (entry.effect().equals(effect)) {
                return entry;
            }
        }
        return null;
    }

    public EffectGroup withEntries(List<Entry> replaced) {
        return new EffectGroup(chance, replaced, picks, minPicks, maxPicks, trigger, cooldown,
                tooltipHeader, nothingChance);
    }

    public boolean armsOnSwing() {
        return windowed() && !trigger.isEvent() && trigger.swingDriven();
    }

    public boolean draws() {
        return fixed() || ranged() || chance < ALWAYS;
    }

    public boolean fixed() {
        return picks.isPresent() && picks.get() > 0;
    }

    public boolean ranged() {
        return !fixed() && (minPicks.isPresent() || maxPicks.isPresent());
    }

    public int lowestPicks() {
        return Math.max(0, minPicks.orElse(ALL_PICKS));
    }

    public int highestPicks() {
        return Math.max(lowestPicks(), maxPicks.orElse(entries.size()));
    }

    public boolean canPickNothing() {
        return nothingChance.isPresent() || (ranged() && lowestPicks() == 0);
    }

    public float noneChance() {
        if (!canPickNothing()) {
            return 0.0F;
        }
        if (nothingChance.isPresent()) {
            return nothingChance.get();
        }
        float none = 1.0F;
        for (Entry entry : entries) {
            if (entry.share() > 0.0F) {
                none *= 1.0F - Math.min(ALWAYS, entry.share());
            }
        }
        return none;
    }

    public Map<String, Double> odds() {
        Map<String, Double> cached = ODDS.get(this);
        if (cached != null) {
            return cached;
        }
        List<Entry> alive = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.share() > 0.0F) {
                alive.add(entry);
            }
        }
        double[] weights = new double[alive.size()];
        for (int index = 0; index < weights.length; index++) {
            weights[index] = alive.get(index).share();
        }
        int low = picksFor(alive.size(), 0.0F);
        int high = picksFor(alive.size(), PICK_ROLL_CEILING);
        double[] summed = new double[alive.size()];
        for (int picked = low; picked <= high; picked++) {
            double[] chances = inclusion(weights, picked);
            for (int index = 0; index < summed.length; index++) {
                summed[index] += chances[index] * (1.0D - noneChance()) / (high - low + 1);
            }
        }
        Map<String, Double> odds = new LinkedHashMap<>();
        for (Entry entry : entries) {
            odds.put(entry.effect(), 0.0D);
        }
        for (int index = 0; index < summed.length; index++) {
            odds.put(alive.get(index).effect(), summed[index]);
        }
        Map<String, Double> frozen = Collections.unmodifiableMap(odds);
        ODDS.put(this, frozen);
        return frozen;
    }

    private static double[] inclusion(double[] weights, int picks) {
        if (weights.length > EXACT_MEMBER_LIMIT) {
            return approximateInclusion(weights, picks);
        }
        return inclusionFrom(weights, 0, picks, new HashMap<>());
    }

    private static double[] inclusionFrom(double[] weights, int taken, int remaining,
                                          Map<Integer, double[]> memo) {
        double[] result = new double[weights.length];
        if (remaining <= 0) {
            return result;
        }
        double[] known = memo.get(taken);
        if (known != null) {
            return known;
        }
        double pool = 0.0D;
        for (int index = 0; index < weights.length; index++) {
            if ((taken & (1 << index)) == 0) {
                pool += weights[index];
            }
        }
        if (pool > 0.0D) {
            for (int index = 0; index < weights.length; index++) {
                if ((taken & (1 << index)) != 0) {
                    continue;
                }
                double first = weights[index] / pool;
                double[] after = inclusionFrom(weights, taken | (1 << index), remaining - 1, memo);
                result[index] += first;
                for (int other = 0; other < weights.length; other++) {
                    result[other] += first * after[other];
                }
            }
        }
        memo.put(taken, result);
        return result;
    }

    private static double[] approximateInclusion(double[] weights, int picks) {
        double pool = 0.0D;
        for (double weight : weights) {
            pool += weight;
        }
        double[] result = new double[weights.length];
        for (int index = 0; index < weights.length && pool > 0.0D; index++) {
            result[index] = Math.min(1.0D, picks * weights[index] / pool);
        }
        return result;
    }

    public int picksFor(int available, float roll) {
        if (available <= 0) {
            return 0;
        }
        if (fixed()) {
            return Math.min(available, picks.get());
        }
        if (!ranged()) {
            return available;
        }
        int low = Math.min(ranged() && lowestPicks() == 0 ? 1 : lowestPicks(), available);
        int high = Math.min(highestPicks(), available);
        if (high <= low) {
            return low;
        }
        int span = high - low + 1;
        int step = (int) (roll * span);
        return low + Math.min(span - 1, Math.max(0, step));
    }
}
