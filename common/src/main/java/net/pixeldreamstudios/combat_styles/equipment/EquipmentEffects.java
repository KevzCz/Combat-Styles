package net.pixeldreamstudios.combat_styles.equipment;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.custom.CustomUse;
import net.pixeldreamstudios.combat_styles.custom.Customs;
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
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), augment,
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withSubstituteStyle(EquipmentEffect effect,
                                                      Optional<String> styleId) {
        return Customs.copy(effect, new EquipmentEffect(styleId, effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withGrantAffixes(EquipmentEffect effect,
                                                   List<String> affixes) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                affixes == null ? List.of() : List.copyOf(affixes),
                effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withSubstitution(EquipmentEffect effect,
                                                   Optional<StyleSubstitution> value) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), value, effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withTuneAffixes(EquipmentEffect effect,
                                                  Map<String, AffixTuning> tuning) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), tuning == null ? Map.of() : Map.copyOf(tuning),
                effect.echo(), effect.attributes(), effect.substitution(),
                effect.summonReplacement(), effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withAttributes(EquipmentEffect effect,
                                                 Map<String, AttributeBonus> attributes) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                attributes == null ? Map.of() : Map.copyOf(attributes),
                effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withOnSwing(EquipmentEffect effect,
                                              Optional<AffixEffects.OnHit> value) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), drop(value), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withOnHit(EquipmentEffect effect,
                                            Optional<AffixEffects.OnHit> value) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), drop(value),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    private static Optional<AffixEffects.OnHit> drop(Optional<AffixEffects.OnHit> value) {
        return value.filter(entry -> !entry.equals(AffixEffects.OnHit.EMPTY) || Customs.has(entry));
    }

    public static EquipmentEffect withCustom(EquipmentEffect effect, List<CustomUse> uses) {
        EquipmentEffect copy = new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(),
                effect.events(), effect.grantAbilities());
        Customs.attach(copy, uses);
        return copy;
    }

    public static EquipmentEffect withEcho(EquipmentEffect effect,
                                           Optional<AffixEffects.Echo> value) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), value,
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withSummonProfile(EquipmentEffect effect,
                                                    Optional<SummonProfile> value) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                value, effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withSummonDive(EquipmentEffect effect,
                                                 Optional<SummonDive> value) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), value, effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withSpells(EquipmentEffect effect, SpellLocks spells) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), spells, effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withResetCombo(EquipmentEffect effect, int repeats) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                repeats > 0 ? Optional.of(repeats) : Optional.empty(),
                effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withEvents(EquipmentEffect effect, EventPayloads events) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(),
                events == null ? EventPayloads.EMPTY : events, effect.grantAbilities()));
    }

    public static EquipmentEffect withDamageBonus(EquipmentEffect effect, List<DamageBonus> bonuses) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(),
                bonuses == null ? List.of() : List.copyOf(bonuses), effect.events(), effect.grantAbilities()));
    }

    public static EquipmentEffect withGrantAbilities(EquipmentEffect effect, Optional<AbilityGrant> grant) {
        return Customs.copy(effect, new EquipmentEffect(effect.substituteStyle(), effect.augment(),
                effect.grantAffixes(), effect.onSwing(), effect.onHit(),
                effect.resetCombo(), effect.tuneAffixes(), effect.echo(),
                effect.attributes(), effect.substitution(), effect.summonReplacement(),
                effect.summonProfile(), effect.summonDive(), effect.spells(), effect.damageBonus(),
                effect.events(), grant == null ? Optional.empty() : grant));
    }
}
