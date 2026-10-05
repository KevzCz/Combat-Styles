package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SlotTuning(List<Integer> slots, List<String> affixes,
                         Optional<AttackOverride> attack,
                         Map<String, AffixTuning> affixTuning,
                         String mode) {

    public static final String MODE_ADD = "add";
    public static final String MODE_REPLACE = "replace";

    private static final Codec<List<Integer>> SLOTS_CODEC = Codec.either(
                    Codec.INT, Codec.INT.listOf())
            .xmap(either -> either.map(List::of, list -> list),
                    list -> list.size() == 1
                            ? Either.left(list.getFirst())
                            : Either.right(list));

    private static final Codec<List<String>> AFFIX_CODEC = Codec.either(
                    Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(List::of, list -> list),
                    list -> list.size() == 1
                            ? Either.left(list.getFirst())
                            : Either.right(list));

    public static final Codec<SlotTuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SLOTS_CODEC.optionalFieldOf("slot", List.of()).forGetter(SlotTuning::slots),
            AFFIX_CODEC.optionalFieldOf("affixes", List.of()).forGetter(SlotTuning::affixes),
            AttackOverride.CODEC.optionalFieldOf("attack").forGetter(SlotTuning::attack),
            AffixTuning.MAP_CODEC.optionalFieldOf("affix_tuning", Map.of())
                    .forGetter(SlotTuning::affixTuning),
            Codec.STRING.optionalFieldOf("mode", MODE_ADD).forGetter(SlotTuning::mode)
    ).apply(instance, SlotTuning::new));

    public SlotTuning {
        slots = slots == null ? List.of() : List.copyOf(slots);
        affixes = affixes == null ? List.of() : List.copyOf(affixes);
        affixTuning = affixTuning == null ? Map.of() : Map.copyOf(affixTuning);
        mode = mode == null ? MODE_ADD : mode;
    }

    public boolean replaces() {
        return MODE_REPLACE.equalsIgnoreCase(mode);
    }

    public boolean matches(int index, int size) {
        if (slots.isEmpty()) {
            return false;
        }
        for (int slot : slots) {
            if (resolve(slot, size) == index) {
                return true;
            }
        }
        return false;
    }

    public static int resolve(int slot, int size) {
        return slot < 0 ? size + slot : slot;
    }

    public StyleEntry applyTo(StyleEntry entry) {
        StyleEntry result = entry;
        if (!affixes.isEmpty()) {
            List<String> merged = new ArrayList<>(replaces() ? List.of() : result.affixIds());
            for (String affix : affixes) {
                if (!merged.contains(affix)) {
                    merged.add(affix);
                }
            }
            result = result.withAffixes(StyleEntry.sorted(merged));
        }
        if (attack.isPresent()) {
            result = result.withAttackOverride(result.attackOverride()
                    .map(existing -> replaces() ? attack.get() : merge(existing, attack.get()))
                    .orElse(attack.get()));
        }
        if (!affixTuning.isEmpty()) {
            Map<String, AffixTuning> merged = new LinkedHashMap<>(
                    replaces() ? Map.of() : result.affixTuning());
            merged.putAll(affixTuning);
            result = result.withAffixTuning(merged);
        }
        return result;
    }

    private AttackOverride merge(AttackOverride base, AttackOverride extra) {
        Map<String, AttackAttribute> attributes = new LinkedHashMap<>(base.attributes());
        for (Map.Entry<String, AttackAttribute> entry : extra.attributes().entrySet()) {
            AttackAttribute existing = attributes.get(entry.getKey());
            AttackAttribute added = entry.getValue();
            if (existing == null || existing.operation() != added.operation()) {
                attributes.put(entry.getKey(), added);
                continue;
            }
            attributes.put(entry.getKey(), new AttackAttribute(
                    existing.amount() + added.amount(), existing.operation()));
        }
        return new AttackOverride(
                extra.damageMultiplier().or(base::damageMultiplier),
                extra.angle().or(base::angle),
                extra.upswing().or(base::upswing),
                extra.rangeMultiplier().or(base::rangeMultiplier),
                extra.movementSpeedMultiplier().or(base::movementSpeedMultiplier),
                extra.hitbox().or(base::hitbox),
                extra.animation().or(base::animation),
                extra.swingSound().or(base::swingSound),
                extra.rarity().or(base::rarity),
                extra.costOverride().or(base::costOverride),
                extra.disabled().or(base::disabled),
                attributes,
                extra.ignoreAffinity().or(base::ignoreAffinity),
                extra.cooldownScale().or(base::cooldownScale),
                extra.hastensNext().or(base::hastensNext),
                extra.drag().or(base::drag),
                extra.behaviour().or(base::behaviour),
                extra.damageType().or(base::damageType),
                extra.affixes().isEmpty() ? base.affixes() : extra.affixes(),
                extra.affixTuning().isEmpty() ? base.affixTuning() : extra.affixTuning(),
                extra.scaleAnimation().or(base::scaleAnimation),
                extra.conditions().or(base::conditions),
                extra.stylePage().or(base::stylePage));
    }

}
