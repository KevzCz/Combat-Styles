package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EquipmentEffect(Optional<String> substituteStyle,
                         Optional<AttackOverride> augment,
                         List<String> grantAffixes,
                         Optional<AffixEffects.OnHit> onSwing,
                         Optional<AffixEffects.OnHit> onHit,
                         Optional<Integer> resetCombo,
                         Map<String, AffixTuning> tuneAffixes,
                         Optional<AffixEffects.Echo> echo,
                         Map<String, AttributeBonus> attributes,
                         Optional<StyleSubstitution> substitution,
                         Optional<SummonReplacement> summonReplacement,
                         Optional<SummonProfile> summonProfile,
                         Optional<SummonDive> summonDive,
                         SpellLocks spells,
                         List<DamageBonus> damageBonus) {

    private static final Codec<Integer> REPEAT_CODEC = Codec.either(Codec.INT, Codec.BOOL)
            .xmap(either -> either.map(count -> count, flag -> flag ? 1 : 0),
                    count -> count == 1 ? Either.right(Boolean.TRUE) : Either.left(count));

    public static final String KEY_SUMMON_ORDER = "summon_order";

    public static final Codec<EquipmentEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("substitute_style").forGetter(EquipmentEffect::substituteStyle),
            AttackOverride.CODEC.optionalFieldOf("augment").forGetter(EquipmentEffect::augment),
            Codec.STRING.listOf().optionalFieldOf("grant_affixes", List.of())
                    .forGetter(EquipmentEffect::grantAffixes),
            AffixEffects.OnHit.CODEC.optionalFieldOf("on_swing").forGetter(EquipmentEffect::onSwing),
            AffixEffects.OnHit.CODEC.optionalFieldOf("on_hit").forGetter(EquipmentEffect::onHit),
            REPEAT_CODEC.optionalFieldOf("reset_combo").forGetter(EquipmentEffect::resetCombo),
            AffixTuning.MAP_CODEC.optionalFieldOf("tune_affixes", Map.of())
                    .forGetter(EquipmentEffect::tuneAffixes),
            AffixEffects.Echo.CODEC.optionalFieldOf("echo").forGetter(EquipmentEffect::echo),
            Codec.unboundedMap(Codec.STRING, AttributeBonus.CODEC)
                    .optionalFieldOf("attributes", Map.of()).forGetter(EquipmentEffect::attributes),
            StyleSubstitution.CODEC.optionalFieldOf("substitute")
                    .forGetter(EquipmentEffect::substitution),
            SummonReplacement.CODEC.optionalFieldOf("summon_replacement")
                    .forGetter(EquipmentEffect::summonReplacement),
            SummonProfile.CODEC.optionalFieldOf("summon_profile")
                    .forGetter(EquipmentEffect::summonProfile),
            SummonDive.CODEC.optionalFieldOf(KEY_SUMMON_ORDER)
                    .forGetter(EquipmentEffect::summonDive),
            SpellLocks.CODEC.optionalFieldOf("spells", SpellLocks.NONE)
                    .forGetter(EquipmentEffect::spells),
            DamageBonus.LIST_CODEC.optionalFieldOf(DamageBonus.KEY, List.of())
                    .forGetter(EquipmentEffect::damageBonus)
    ).apply(instance, EquipmentEffect::new));

    public EquipmentEffect {
        grantAffixes = grantAffixes == null ? List.of() : List.copyOf(grantAffixes);
        tuneAffixes = tuneAffixes == null ? Map.of() : Map.copyOf(tuneAffixes);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        spells = spells == null ? SpellLocks.NONE : spells;
        damageBonus = damageBonus == null ? List.of() : List.copyOf(damageBonus);
    }

    public static final EquipmentEffect EMPTY = new EquipmentEffect(Optional.empty(),
            Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
            Map.of(), Optional.empty(), Map.of(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), SpellLocks.NONE, List.of());

    public EquipmentEffect mergedWith(EquipmentEffect other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        List<String> grants = new ArrayList<>(grantAffixes);
        for (String affix : other.grantAffixes()) {
            if (!grants.contains(affix)) {
                grants.add(affix);
            }
        }
        Map<String, AffixTuning> tuning = new LinkedHashMap<>(tuneAffixes);
        for (Map.Entry<String, AffixTuning> entry : other.tuneAffixes().entrySet()) {
            tuning.merge(entry.getKey(), entry.getValue(),
                    (existing, added) -> added.mergedUnder(existing));
        }
        Map<String, AttributeBonus> merged = new LinkedHashMap<>(attributes);
        merged.putAll(other.attributes());
        List<DamageBonus> bonuses = new ArrayList<>(damageBonus);
        bonuses.addAll(other.damageBonus());
        return new EquipmentEffect(substituteStyle.or(other::substituteStyle),
                augment.or(other::augment), grants,
                onSwing.or(other::onSwing), onHit.or(other::onHit),
                resetCombo.or(other::resetCombo), tuning,
                echo.or(other::echo), merged, substitution.or(other::substitution),
                summonReplacement.or(other::summonReplacement),
                summonProfile.or(other::summonProfile),
                summonDive.or(other::summonDive), spells.mergedWith(other.spells()), bonuses);
    }

    public boolean hasAlwaysOnParts() {
        return !attributes.isEmpty() || !spells.isEmpty() || EquipmentDefinition.substitutes(this)
                || summonProfile.isPresent() || summonReplacement.isPresent()
                || !damageBonus.isEmpty();
    }

    public boolean rewritesAttack() {
        return substituteStyle.isPresent() || augment.isPresent() || !grantAffixes.isEmpty()
                || !tuneAffixes.isEmpty();
    }

    public boolean isEmpty() {
        return substituteStyle.isEmpty() && augment.isEmpty() && grantAffixes.isEmpty()
                && onSwing.isEmpty() && onHit.isEmpty() && resetCombo.isEmpty()
                && tuneAffixes.isEmpty() && echo.isEmpty() && attributes.isEmpty()
                && substitution.isEmpty() && summonReplacement.isEmpty()
                && summonProfile.isEmpty() && summonDive.isEmpty() && spells.isEmpty()
                && damageBonus.isEmpty();
    }
}
