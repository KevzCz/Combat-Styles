package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentContext;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EquipmentComposer {
    private EquipmentComposer() {
    }

    public static StyleEntry rewrite(StyleEntry entry, int index, int size) {
        if (entry == null || EquipmentRegistry.isEmpty()) {
            return entry;
        }
        Player player = SwingHolder.current() instanceof Player holder ? holder : null;
        if (player == null) {
            return entry;
        }
        StylePage page = StyleRegistry.get(entry.styleId());
        EquipmentContext context = new EquipmentContext(player, entry,
                page == null ? null : page.attack(), index, size,
                EquipmentRuntime.swingCount(player));

        StyleEntry rewritten = entry;
        Set<String> substituted = new HashSet<>();
        for (EquipmentRuntime.Match equipment : EquipmentRuntime.matching(player, context)) {
            boolean substitutes = equipment.effect().substituteStyle().isPresent();
            if (substitutes
                    && !EquipmentRuntime.charged(player, equipment.definition())) {
                continue;
            }
            if (!EquipmentRuntime.rewriteAllowed(player, equipment)) {
                continue;
            }
            boolean defining = substitutes && !substituted.add(equipment.id());
            rewritten = apply(rewritten, equipment, defining);
        }
        return rewritten;
    }

    private static StyleEntry apply(StyleEntry entry, EquipmentRuntime.Match equipment,
                                    boolean definingCopy) {
        StyleEntry result = entry;

        String substitute = equipment.effect().substituteStyle().orElse(null);
        if (substitute != null) {
            String resolved = CustomStyles.styleIdOf(substitute);
            boolean custom = resolved != null && !resolved.isBlank()
                    && StyleRegistry.contains(resolved);
            if (custom) {
                result = new StyleEntry(resolved, result.soundId(), result.affixIds(),
                        result.attackOverride(), result.name(), result.nameKey(),
                        result.affixTuning(), Optional.of(substitute));
            } else if (StyleRegistry.contains(substitute)) {
                result = new StyleEntry(substitute, result.soundId(), result.affixIds(),
                        result.attackOverride(), result.name(), result.nameKey(),
                        result.affixTuning(), result.customStyleId());
            }
        }

        if (!equipment.effect().grantAffixes().isEmpty()) {
            List<String> merged = new ArrayList<>(result.affixIds());
            for (String affix : equipment.effect().grantAffixes()) {
                if (!merged.contains(affix)) {
                    merged.add(affix);
                }
            }
            result = result.withAffixes(StyleEntry.sorted(merged));
        }

        AttackOverride augment = definingCopy ? null : equipment.effect().augment().orElse(null);
        if (augment != null) {
            AttackOverride scaled = scaledOnto(result, augment);
            result = result.withAttackOverride(
                    result.attackOverride().map(existing -> merge(existing, scaled))
                            .orElse(scaled));
        }
        return result;
    }

    private static AttackOverride scaledOnto(StyleEntry entry, AttackOverride augment) {
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null) {
            return augment;
        }
        WeaponAttributes.Attack attack = page.attack();
        Optional<Double> damage = augment.damageMultiplier()
                .map(ratio -> attack.damageMultiplier() * ratio);
        Optional<Float> range = augment.rangeMultiplier()
                .map(ratio -> attack.rangeMultiplier() * ratio);
        if (damage.isEmpty() && range.isEmpty()) {
            return augment;
        }
        return new AttackOverride(
                damage.or(augment::damageMultiplier),
                augment.angle(),
                augment.upswing(),
                range.or(augment::rangeMultiplier),
                augment.movementSpeedMultiplier(),
                augment.hitbox(),
                augment.animation(),
                augment.swingSound(),
                augment.rarity(),
                augment.costOverride(),
                augment.disabled(),
                augment.attributes(),
                augment.ignoreAffinity(),
                augment.cooldownScale(),
                augment.hastensNext(),
                augment.drag(),
                augment.behaviour(),
                augment.damageType(),
                augment.affixes(),
                augment.affixTuning(),
                augment.scaleAnimation(),
                augment.conditions(),
                augment.stylePage());
    }

    public static AttackOverride mergeAugments(AttackOverride base, AttackOverride extra) {
        return merge(base, extra);
    }

    private static AttackOverride merge(AttackOverride base, AttackOverride extra) {
        return base.mergedWith(extra, MergeMode.MULTIPLY);
    }
}
