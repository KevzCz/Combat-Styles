package net.pixeldreamstudios.combat_styles.equipment;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;

import net.pixeldreamstudios.combat_styles.damage.DamageBonus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EquipmentEffects {

    private EquipmentEffects() {
    }

    public static EquipmentEffect withAugment(EquipmentEffect effect,
                                              Optional<AttackOverride> augment) {
        return new EquipmentEffect(effect.substituteStyle(), augment,
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withSubstituteStyle(EquipmentEffect effect,
                                                      Optional<String> styleId) {
        return new EquipmentEffect(styleId, effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withGrantAffixes(EquipmentEffect effect,
                                                   List<String> affixes) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                affixes == null ? List.of() : List.copyOf(affixes),
                effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withSubstitution(EquipmentEffect effect,
                                                   Optional<StyleSubstitution> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), value, effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withTuneAffixes(EquipmentEffect effect,
                                                  Map<String, AffixTuning> tuning) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), tuning == null ? Map.of() : Map.copyOf(tuning),
                effect.echo(), effect.attributes(), effect.substitution(),
                effect.summonReplacement(), effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withAttributes(EquipmentEffect effect,
                                                 Map<String, AttributeBonus> attributes) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                attributes == null ? Map.of() : Map.copyOf(attributes),
                effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withOnSwing(EquipmentEffect effect,
                                              Optional<AffixEffects.OnHit> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), drop(value), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withOnHit(EquipmentEffect effect,
                                            Optional<AffixEffects.OnHit> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), drop(value),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    private static Optional<AffixEffects.OnHit> drop(Optional<AffixEffects.OnHit> value) {
        return value.filter(entry -> !entry.equals(AffixEffects.OnHit.EMPTY));
    }

    public static EquipmentEffect withEcho(EquipmentEffect effect,
                                           Optional<AffixEffects.Echo> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), value,
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withSummonReplacement(EquipmentEffect effect,
                                                        Optional<SummonReplacement> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), value,
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withSummonProfile(EquipmentEffect effect,
                                                    Optional<SummonProfile> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                value, effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withSummonDive(EquipmentEffect effect,
                                                 Optional<SummonDive> value) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), value, effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withSpells(EquipmentEffect effect, SpellLocks spells) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), spells, effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withResetCombo(EquipmentEffect effect, int repeats) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                repeats > 0 ? Optional.of(repeats) : Optional.empty(),
                effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events());
    }

    public static EquipmentEffect withEvents(EquipmentEffect effect, EventPayloads events) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(),
                events == null ? EventPayloads.EMPTY : events);
    }

    public static EquipmentEffect withDamageBonus(EquipmentEffect effect, List<DamageBonus> bonuses) {
        return new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(),
                bonuses == null ? List.of() : List.copyOf(bonuses), effect.events());
    }
}
