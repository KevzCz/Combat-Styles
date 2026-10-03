package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SequenceComposer {

    private SequenceComposer() {
    }

    private static List<StyleEntry> collapseForSubstitute(List<StyleEntry> entries,
                                                          ItemStack weapon, String sequenceName) {
        if (SequenceService.SUBSTITUTED.equals(sequenceName)) {
            return entries;
        }
        Player player = SwingHolder.current() instanceof Player holder ? holder : null;
        EquipmentDefinition substituting = SubstitutionScope.activeFor(player, weapon);
        if (substituting == null || entries.isEmpty()) {
            return entries;
        }

        StyleSubstitution substitution = substituting.substitutingEffect(player).substitution().orElse(null);
        if (substitution == null || substitution.isEmpty()) {
            return List.of(entries.get(0));
        }

        List<StyleEntry> replacement = new ArrayList<>();
        for (String styleId : substitution.styles()) {
            StyleEntry entry = SequenceService.entryFor(styleId);
            if (entry != null) {
                replacement.add(entry);
            }
        }
        if (replacement.isEmpty()) {
            return entries;
        }

        return switch (substitution.mode()) {
            case StyleSubstitution.MODE_APPEND -> concat(entries, replacement);
            case StyleSubstitution.MODE_PREPEND -> concat(replacement, entries);
            case StyleSubstitution.MODE_REPLACE_SLOT -> replaceSlots(entries, replacement,
                    substitution);
            default -> replacement;
        };
    }

    private static List<StyleEntry> concat(List<StyleEntry> first, List<StyleEntry> second) {
        List<StyleEntry> merged = new ArrayList<>(first.size() + second.size());
        merged.addAll(first);
        merged.addAll(second);
        return merged;
    }

    private static List<StyleEntry> replaceSlots(List<StyleEntry> entries,
                                                 List<StyleEntry> replacement,
                                                 StyleSubstitution substitution) {
        List<StyleEntry> result = new ArrayList<>(entries);
        int taken = 0;
        for (int index = 0; index < result.size(); index++) {
            if (!substitution.targets(index, result.size())) {
                continue;
            }
            result.set(index, replacement.get(Math.min(taken, replacement.size() - 1)));
            taken++;
        }
        return result;
    }

    public static WeaponAttributes compose(WeaponAttributes base, StyleSequence sequence) {
        return compose(base, sequence, Map.of(), Set.of());
    }

    public static WeaponAttributes compose(WeaponAttributes base, StyleSequence sequence,
                                           Map<String, AttackOverride> perItem) {
        return compose(base, sequence, perItem, Set.of());
    }

    private static int collapsedSlotOf(List<StyleEntry> original, List<StyleEntry> merged,
                                       int index) {
        if (original == merged || index < 0 || index >= merged.size()) {
            return StyleSubstitution.NO_ORIGINAL_SLOT;
        }
        StyleEntry entry = merged.get(index);
        for (int slot = 0; slot < original.size(); slot++) {
            if (original.get(slot) == entry) {
                return slot;
            }
        }
        return StyleSubstitution.NO_ORIGINAL_SLOT;
    }

    public static WeaponAttributes compose(WeaponAttributes base, StyleSequence sequence,
                                           Map<String, AttackOverride> perItem,
                                           Set<String> affinityExempt) {
        if (base == null || sequence == null || sequence.isEmpty()) {
            return base;
        }
        List<WeaponAttributes.Attack> composedList = new ArrayList<>(sequence.size());
        List<StyleEntry> original = sequence.entries();
        List<StyleEntry> entries = collapseForSubstitute(original,
                SwingHolder.currentWeapon(), sequence.name());
        boolean substituted = SequenceService.SUBSTITUTED.equals(sequence.name())
                || entries != original;
        for (int index = 0; index < entries.size(); index++) {
            StyleEntry entry = EquipmentComposer.rewrite(entries.get(index), index, entries.size());
            StylePage page = StyleRegistry.get(entry.styleId());
            if (page == null) {
                continue;
            }
            int itemSlot = substituted
                    ? collapsedSlotOf(original, entries, index)
                    : index;
            AttackOverride fromItem = perItem == null || itemSlot < 0
                    ? null
                    : perItem.get(String.valueOf(itemSlot));
            AttackOverride entryOverride = entry.attackOverride().orElse(null);
            AttackOverride override = entryOverride == null
                    ? fromItem
                    : entryOverride.overriddenBy(fromItem);
            if (override != null) {
                page = AttackOverrides.applyTo(page, override);
            }
            WeaponAttributes.Sound sound = entry.soundId()
                    .map(StyleRegistry::swingSound)
                    .orElse(null);
            boolean exempt = affinityExempt.contains(entry.styleId())
                    || affinityExempt.contains(page.styleId())
                    || ignoresAffinity(entry, override, page.styleId());
            WeaponAttributes.Attack copied = copyOf(page.attack(), sound);
            composedList.add(exempt
                    ? copied
                    : withAffinity(copied, page.category(), base.category()));
        }
        if (composedList.isEmpty()) {
            return base;
        }
        WeaponAttributes.Attack[] composed = composedList.toArray(new WeaponAttributes.Attack[0]);

        return new WeaponAttributes(
                base.attackRange(),
                base.rangeBonus(),
                base.pose(),
                base.offHandPose(),
                base.two_handed(),
                base.category(),
                composed,
                base.trailAppearance());
    }

    public static boolean ignoresAffinity(StyleEntry entry, AttackOverride override, String styleId) {
        if (override != null && override.ignoreAffinity().orElse(false)) {
            return true;
        }
        if (entry != null && entry.attackOverride()
                .flatMap(AttackOverride::ignoreAffinity)
                .orElse(false)) {
            return true;
        }
        AttackOverride fromStyle = styleId == null ? null : AttackOverrides.get(styleId);
        return fromStyle != null && fromStyle.ignoreAffinity().orElse(false);
    }

    private static WeaponAttributes.Attack withAffinity(WeaponAttributes.Attack attack,
                                                        String nodeCategory,
                                                        String weaponCategory) {
        CombatStylesConfig.AffinityTier tier =
                CategoryAffinity.multipliersFor(nodeCategory, weaponCategory);
        if (tier == CombatStylesConfig.AffinityTier.NEUTRAL) {
            return attack;
        }

        boolean preserveArc = CombatStylesConfig.get().preserveArc();
        return new WeaponAttributes.Attack(
                attack.conditions(),
                attack.hitbox(),
                attack.damageMultiplier() * tier.damage(),
                attack.movementSpeedMultiplier(),
                (float) (attack.rangeMultiplier() * tier.range()),
                preserveArc ? attack.angle() : attack.angle() * tier.range(),
                attack.upswing() * tier.upswing(),
                attack.animation(),
                attack.swingSound(),
                attack.impactSound(),
                attack.trailParticles());
    }

    private static WeaponAttributes.Attack copyOf(WeaponAttributes.Attack source,
                                                  WeaponAttributes.Sound swingOverride) {
        return new WeaponAttributes.Attack(
                source.conditions(),
                source.hitbox(),
                source.damageMultiplier(),
                source.movementSpeedMultiplier(),
                source.rangeMultiplier(),
                source.angle(),
                source.upswing(),
                source.animation(),
                swingOverride == null ? source.swingSound() : swingOverride,
                source.impactSound(),
                source.trailParticles());
    }

    public static SequenceStats stats(StyleSequence sequence) {
        List<StylePage> pages = sequence.resolve();
        if (pages.isEmpty()) {
            return SequenceStats.EMPTY;
        }
        double damage = 0.0D;
        double upswing = 0.0D;
        double range = 0.0D;
        double movement = 0.0D;
        double angle = 0.0D;
        for (StylePage page : pages) {
            if (page == null) {
                continue;
            }
            damage += page.damageMultiplier();
            upswing += page.upswing();
            range += page.rangeMultiplier();
            movement += page.movementSpeedMultiplier();
            angle += page.angle();
        }
        int count = pages.size();
        return new SequenceStats(count,
                damage / count,
                upswing / count,
                range / count,
                movement / count,
                angle / count);
    }

    public record SequenceStats(int attacks,
                                double averageDamage,
                                double averageUpswing,
                                double averageRange,
                                double averageMovement,
                                double averageAngle) {

        public static final SequenceStats EMPTY = new SequenceStats(0, 1.0D, 0.5D, 1.0D, 1.0D, 0.0D);
    }
}
