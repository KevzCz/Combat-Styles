package net.pixeldreamstudios.combat_styles;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.EffectCooldown;
import net.pixeldreamstudios.combat_styles.equipment.EffectGroup;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffect;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.loot.BookRoll;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.loot.WeaponRoll;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.bettercombat.api.WeaponAttributes;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DataIntegrity {

    private DataIntegrity() {
    }

    private static void reportCustomStyles() {
        List<String> broken = new ArrayList<>();
        for (CustomStyle style : CustomStyles.snapshot().values()) {
            String animation = style.entry().styleId();
            if (StyleRegistry.get(animation) != null) {
                continue;
            }
            broken.add(style.id() + "  wants [" + animation + "]  reason: " + whyMissing(animation));
        }
        if (broken.isEmpty()) {
            CombatStyles.LOGGER.info("All {} custom styles resolved against {} indexed pages",
                    CustomStyles.snapshot().size(), StyleRegistry.all().size());
            return;
        }
        CombatStyles.LOGGER.warn("{} of {} custom styles FAILED ({} pages indexed):",
                broken.size(), CustomStyles.snapshot().size(), StyleRegistry.all().size());
        for (String entry : broken) {
            CombatStyles.LOGGER.warn("  {}", entry);
        }
    }

    private static String whyMissing(String animation) {
        Map<ResourceLocation, WeaponAttributes> sources = WeaponRegistryAccess.indexable();
        List<String> owners = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        for (Map.Entry<ResourceLocation, WeaponAttributes> entry : sources.entrySet()) {
            WeaponAttributes attributes = entry.getValue();
            if (attributes == null || attributes.attacks() == null) {
                continue;
            }
            for (WeaponAttributes.Attack attack : attributes.attacks()) {
                if (attack != null && animation.equals(attack.animation())) {
                    owners.add(entry.getKey().toString());
                    if (attributes.category() != null && !categories.contains(attributes.category())) {
                        categories.add(attributes.category());
                    }
                    break;
                }
            }
        }
        if (owners.isEmpty()) {
            return "NOT PRESENT in any of the " + sources.size()
                    + " weapon entries BetterCombat exposes";
        }
        return "present on " + owners.size() + " weapon(s) " + owners
                + " category " + categories
                + " but no StylePage was indexed for it (dedup or override dropped it)";
    }

    public static void verify() {
        List<String> problems = new ArrayList<>();

        for (EquipmentDefinition equipment : EquipmentRegistry.all()) {
            for (Map.Entry<String, TriggeredEffect> named : equipment.effects().entrySet()) {
                String where = equipment.id() + "." + named.getKey();
                EquipmentEffect effect = named.getValue().effect();
                for (String affixId : effect.grantAffixes()) {
                    if (AffixRegistry.get(affixId) == null) {
                        problems.add(where + " grants unknown affix " + affixId);
                    }
                }
                for (String affixId : effect.tuneAffixes().keySet()) {
                    if (AffixRegistry.get(affixId) == null) {
                        problems.add(where + " tunes unknown affix " + affixId);
                    }
                }
                effect.substituteStyle().ifPresent(styleId -> {
                    if (!knownStyle(styleId)) {
                        problems.add(where + " substitutes unknown style " + styleId);
                    }
                });
                effect.substitution().ifPresent(substitution -> {
                    for (String styleId : substitution.styles()) {
                        if (!knownStyle(styleId)) {
                            problems.add(where + " substitutes unknown style " + styleId);
                        }
                    }
                });
                problems.addAll(dependencyProblems(where, equipment, named.getValue()));
                if (rewritesOnHitOnly(named.getValue())) {
                    problems.add(where + " augments the attack, but its trigger only"
                            + " fires on a hit or an event with no uses/duration, so the"
                            + " attack is already built and the change never applies");
                }
                if (nestedChance(named.getValue().trigger())) {
                    problems.add(where + " sets chance inside any_of or all_of, which is"
                            + " ignored; put it on the top-level trigger");
                }
            }
            for (Map.Entry<String, EffectGroup> group : equipment.groups().entrySet()) {
                EquipmentTrigger groupTrigger = group.getValue().trigger();
                EffectCooldown groupCooldown = group.getValue().cooldown();
                if (groupTrigger.isAlways() || groupCooldown.windowed()
                        || (!groupTrigger.needsArmingAtRest() && groupCooldown.holdWhileActive())) {
                    continue;
                }
                String reason = groupTrigger.needsArmingAtRest()
                        ? " has an event trigger with no uses/duration, so "
                        : " has Hold While Active off and no duration, so ";
                for (Map.Entry<String, TriggeredEffect> named : equipment.effects().entrySet()) {
                    if (group.getValue().holds(named.getKey())
                            && named.getValue().effect().hasAlwaysOnParts()) {
                        problems.add(equipment.id() + " group " + group.getKey() + reason
                                + named.getKey() + "'s attributes, spells, summon profiles or"
                                + " substitution never turn on");
                    }
                }
            }
        }

        for (String ability : AbilityConfig.all().keySet()) {
            if (!AbilityRegistry.has(ability)) {
                problems.add("ability " + ability
                        + " is configured but no behaviour is registered for it");
            }
        }
        for (String ability : AbilityRegistry.registered()) {
            if (AbilityConfig.declared(ability) == null
                    && !AbilityConfig.all().containsKey(ability)) {
                problems.add("ability " + ability
                        + " has a behaviour but ships no defaults");
            }
        }

        for (StyleAffix affix : AffixRegistry.all()) {
            for (AffixEffects.OnHit effects : List.of(affix.onHit(), affix.onSwing())) {
                for (AffixEffects.Summon summon : effects.summons()) {
                    summon.ability()
                            .filter(ability -> !AbilityRegistry.has(ability))
                            .ifPresent(ability -> problems.add("affix " + affix.serializedId()
                                    + " names unknown summon ability " + ability));
                }
            }
        }

        for (WeaponRoll roll : WeaponRolls.all()) {
            for (String affixId : roll.affixes()) {
                if (AffixRegistry.get(affixId) == null) {
                    problems.add("weapon roll grants unknown affix " + affixId);
                }
            }
            for (StyleEntry entry : roll.styles()) {
                if (!knownEntry(entry)) {
                    problems.add("weapon roll references unknown style " + entry.styleId());
                }
            }
        }

        for (BookRoll roll : BookRolls.all()) {
            for (BookRoll.Weighted weighted : roll.pool()) {
                StyleEntry entry = weighted.entry();
                for (String affixId : entry.affixIds()) {
                    if (AffixRegistry.get(affixId) == null) {
                        problems.add("style book " + roll.rarity()
                                + " references unknown affix " + affixId);
                    }
                }
                if (entry.customStyleId().isPresent() && !knownEntry(entry)) {
                    problems.add("style book " + roll.rarity()
                            + " references unknown style " + entry.styleId());
                }
            }
        }

        reportCustomStyles();

        if (problems.isEmpty()) {
            CombatStyles.LOGGER.info("Data integrity check passed");
            return;
        }
        CombatStyles.LOGGER.warn("Data integrity found {} problem(s):", problems.size());
        for (String problem : problems) {
            CombatStyles.LOGGER.warn("  {}", problem);
        }
    }

    private static boolean rewritesOnHitOnly(TriggeredEffect entry) {
        EquipmentEffect effect = entry.effect();
        if (effect.augment().isEmpty()) {
            return false;
        }
        EquipmentTrigger trigger = entry.trigger();
        if (trigger.arms(entry.cooldown())) {
            return false;
        }
        return trigger.isEvent() || trigger.requiresTarget()
                || trigger.inTree(node -> node.phase().wantsHitPass());
    }

    private static boolean nestedChance(EquipmentTrigger trigger) {
        for (EquipmentTrigger nested : trigger.anyOf()) {
            if (nested.chance().isPresent() || nestedChance(nested)) {
                return true;
            }
        }
        for (EquipmentTrigger nested : trigger.allOf()) {
            if (nested.chance().isPresent() || nestedChance(nested)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> dependencyProblems(String where,
                                                   EquipmentDefinition equipment,
                                                   TriggeredEffect triggered) {
        List<String> found = new ArrayList<>();
        EquipmentEffect effect = triggered.effect();

        triggered.effect().summonProfile().ifPresent(profile -> {
            for (String ability : profile.abilities()) {
                if (!AbilityRegistry.has(ability)) {
                    found.add(where + " summon profile names unknown ability " + ability);
                }
            }
            profile.entity().ifPresent(entity -> {
                if (!knownEntity(entity)) {
                    found.add(where + " summons unknown entity " + entity);
                }
            });
            for (String entity : profile.replacedSummonedEntities()) {
                if (!knownEntity(entity)) {
                    found.add(where + " replaces unknown entity " + entity);
                }
            }
            if (profile.replacesSummons() && profile.entity().isEmpty()) {
                found.add(where + " sets replaces_summons but names no become entity");
            }
        });

        effect.summonReplacement().ifPresent(replacement -> {
            if (!knownEntity(replacement.becomeEntity())) {
                found.add(where + " becomes unknown entity " + replacement.becomeEntity());
            }
            for (String ability : replacement.abilities()) {
                if (!AbilityRegistry.has(ability)) {
                    found.add(where + " replacement names unknown ability " + ability);
                }
            }
        });

        effect.summonDive().ifPresent(dive -> {
            for (String entity : dive.entities()) {
                if (!knownEntity(entity)) {
                    found.add(where + " dives unknown entity " + entity);
                }
            }
        });

        if (triggered.trigger().requiresTarget() && !namesHitTrigger(equipment)) {
            found.add(where + " needs a target but no effect on this item triggers on hit");
        }
        if (triggered.needsArming()) {
            found.add(where + " has attributes, spells, summon profiles or a substitution under an"
                    + " event trigger with no uses/duration, so they never turn on");
        }
        if (triggered.needsEdgeWindow()) {
            found.add(where + " has Hold While Active off and no duration, so its attributes,"
                    + " spells, summon profiles or substitution never turn on");
        }
        if (triggered.swingGateIgnored()) {
            found.add(where + " gates a substitution on hitbox/style/affix conditions, which"
                    + " substitutions don't check (slot conditions only pick where it starts)");
        }
        if (setsAttribute(effect) && !chargesAnywhere(equipment)) {
            found.add(where + " uses set attributes but no worn effect declares"
                    + " a substitution cooldown, so it snaps instead of ramping");
        }
        return found;
    }

    private static boolean setsAttribute(EquipmentEffect effect) {
        for (AttributeBonus bonus : effect.attributes().values()) {
            if (bonus != null && bonus.set()) {
                return true;
            }
        }
        return false;
    }

    private static boolean chargesAnywhere(EquipmentDefinition equipment) {
        for (TriggeredEffect entry : equipment.triggered()) {
            if (entry.cooldownTicks() > 0 && entry.effect().substitution().isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean namesHitTrigger(EquipmentDefinition equipment) {
        for (TriggeredEffect entry : equipment.triggered()) {
            if (entry.effect().onHit().isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean knownEntity(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }

    private static boolean knownStyle(String styleId) {
        String resolved = CustomStyles.styleIdOf(styleId);
        return StyleRegistry.contains(styleId)
                || (resolved != null && !resolved.isBlank() && StyleRegistry.contains(resolved));
    }

    private static boolean knownEntry(StyleEntry entry) {
        return entry.customStyleId().isPresent()
                ? knownStyle(entry.customStyleId().get())
                : StyleRegistry.contains(entry.styleId());
    }
}
