package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;

import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StyleCost {

    private static final double BASELINE_DAMAGE = 0.75D;
    private static final double DAMAGE_WEIGHT = 10.0D;
    private static final double ARC_WEIGHT = 3.0D;
    private static final double FULL_CIRCLE = 360.0D;
    private static final double CONDITIONAL_DISCOUNT = 1.0D;
    private static final double MINIMUM_COST = 0.5D;
    private static final double STEP = 2.0D;
    private static final double MINIMUM_BUDGET = 6.0D;
    private static final double REPEAT_STEP = 0.5D;
    public static final double TOLERANCE = 0.001D;
    private static final double BUDGET_HEADROOM = 1.0D;

    private StyleCost() {
    }

    public static double of(WeaponAttributes.Attack attack) {
        if (attack == null) {
            return MINIMUM_COST;
        }
        double cost = DAMAGE_WEIGHT * (attack.damageMultiplier() - BASELINE_DAMAGE)
                + ARC_WEIGHT * (Math.abs(attack.angle()) / FULL_CIRCLE)
                + DAMAGE_WEIGHT * (attack.rangeMultiplier() - 1.0F);
        if (attack.conditions() != null && attack.conditions().length > 0) {
            cost -= CONDITIONAL_DISCOUNT;
        }
        return Math.max(MINIMUM_COST, Math.round(cost * STEP) / STEP);
    }

    public static double of(StylePage page) {
        return page == null ? MINIMUM_COST : of(page.attack());
    }

    private static double of(StylePage page, AttackOverride override) {
        if (override == null) {
            return of(page);
        }
        return override.costOverride()
                .map(cost -> Math.max(MINIMUM_COST, cost))
                .orElseGet(() -> of(AttackOverrides.applyTo(page, override)));
    }

    public static double onWeapon(StylePage page, String weaponCategory) {
        if (page == null) {
            return MINIMUM_COST;
        }
        double multiplier = CategoryAffinity.costMultiplier(page.category(), weaponCategory);
        return Math.max(MINIMUM_COST, Math.round(of(page.attack()) * multiplier * STEP) / STEP);
    }

    public static double budgetOf(WeaponAttributes attributes) {
        return budgetOf(attributes, CombatStylesConfig.get().maxAttacksPerSequence());
    }

    public static double of(StylePage page, String affixId) {
        return of(page, affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public static double of(StylePage page, List<String> affixIds) {
        return of(page) * ResolvedAffixes.of(affixIds).costMultiplier();
    }

    public static double of(StylePage page, List<String> affixIds, String weaponCategory) {
        return onWeapon(page, weaponCategory) * ResolvedAffixes.of(affixIds).costMultiplier();
    }

    public static double of(StyleEntry entry) {
        if (entry == null) {
            return MINIMUM_COST;
        }
        StylePage page = pageOf(entry);
        return page == null
                ? MINIMUM_COST
                : of(page) * entry.resolveAffixes().costMultiplier();
    }

    private static StylePage pageOf(StyleEntry entry) {
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null || entry.attackOverride().isEmpty()) {
            return page;
        }
        return AttackOverrides.applyTo(page, entry.attackOverride().get());
    }

    public static double totalOf(StyleSequence sequence) {
        return totalOf(sequence, null);
    }

    public static double totalOf(StyleSequence sequence, String weaponCategory) {
        return totalOf(sequence, weaponCategory, Set.of());
    }

    public static double totalOf(StyleSequence sequence, String weaponCategory,
                                 Set<String> affinityExempt) {
        return totalOf(sequence, weaponCategory, affinityExempt, Map.of());
    }

    public static double totalOf(StyleSequence sequence, String weaponCategory,
                                 Set<String> affinityExempt,
                                 Map<String, AttackOverride> slotOverrides) {
        Map<String, Integer> seen = new HashMap<>();
        double total = 0.0D;
        List<StyleEntry> entries = sequence.entries();
        for (int index = 0; index < entries.size(); index++) {
            StyleEntry entry = entries.get(index);
            StylePage page = pageOf(entry);
            if (page == null) {
                continue;
            }
            int repeat = seen.merge(entry.styleId(), 1, Integer::sum) - 1;
            String category = isExempt(affinityExempt, entry, page) ? null : weaponCategory;
            total += of(styleCategoryOf(entry, page), page, entry.resolveAffixes(), category,
                    slotOverrides.get(String.valueOf(index))) * repeatMultiplier(repeat);
        }
        return total;
    }

    private static String styleCategoryOf(StyleEntry entry, StylePage page) {
        return CustomStyles.categoryOf(entry, page);
    }

    private static double of(String styleCategory, StylePage page, ResolvedAffixes affixes,
                             String weaponCategory, AttackOverride override) {
        double base = of(page, override);
        if (weaponCategory == null) {
            return base * affixes.costMultiplier();
        }
        double multiplier = CategoryAffinity.costMultiplier(styleCategory, weaponCategory);
        return Math.max(MINIMUM_COST, Math.round(base * multiplier * STEP) / STEP)
                * affixes.costMultiplier();
    }

    private static boolean isExempt(Set<String> affinityExempt, StyleEntry entry, StylePage page) {
        if (entry.attackOverride().flatMap(AttackOverride::ignoreAffinity).orElse(false)) {
            return true;
        }
        return affinityExempt.contains(entry.styleId()) || affinityExempt.contains(page.styleId());
    }

    public static double costAt(StyleSequence sequence, int index) {
        return costAt(sequence, index, null, Set.of());
    }

    public static double costAt(StyleSequence sequence, int index, String weaponCategory,
                                Set<String> affinityExempt) {
        return costAt(sequence, index, weaponCategory, affinityExempt, Map.of());
    }

    public static double costAt(StyleSequence sequence, int index, String weaponCategory,
                                Set<String> affinityExempt,
                                Map<String, AttackOverride> slotOverrides) {
        StyleEntry entry = sequence.entry(index);
        StylePage page = entry == null ? null : pageOf(entry);
        if (page == null) {
            return MINIMUM_COST;
        }
        int repeat = 0;
        for (int i = 0; i < index; i++) {
            StyleEntry previous = sequence.entry(i);
            if (previous != null && previous.styleId().equals(entry.styleId())) {
                repeat++;
            }
        }
        String category = isExempt(affinityExempt, entry, page) ? null : weaponCategory;
        return of(styleCategoryOf(entry, page), page, entry.resolveAffixes(), category,
                slotOverrides.get(String.valueOf(index))) * repeatMultiplier(repeat);
    }

    public static double repeatMultiplier(int previousUses) {
        return 1.0D + (REPEAT_STEP * Math.max(0, previousUses));
    }

    public static double costOfAdding(StyleSequence sequence, StylePage page, String affixId) {
        if (page == null) {
            return MINIMUM_COST;
        }
        int repeat = 0;
        for (StyleEntry entry : sequence.entries()) {
            if (entry.styleId().equals(page.styleId())) {
                repeat++;
            }
        }
        return of(page, affixId) * repeatMultiplier(repeat);
    }

    public static boolean fitsBudget(StyleSequence sequence, WeaponAttributes attributes) {
        String category = attributes == null ? null : attributes.category();
        return totalOf(sequence, category) <= budgetOf(attributes) + TOLERANCE;
    }

    public static double budgetOf(WeaponAttributes attributes, int slotLimit) {
        if (attributes == null || attributes.attacks() == null
                || attributes.attacks().length == 0) {
            return MINIMUM_BUDGET;
        }
        double total = 0.0D;
        for (WeaponAttributes.Attack attack : attributes.attacks()) {
            total += of(attack);
        }
        double perAttack = total / attributes.attacks().length;
        int slots = Math.max(attributes.attacks().length, slotLimit);
        return Math.max(MINIMUM_BUDGET, perAttack * slots + BUDGET_HEADROOM);
    }

    public static boolean fitsBudget(StyleSequence sequence, WeaponAttributes attributes,
                                     int slotLimit) {
        return fitsBudget(sequence, attributes, slotLimit, Set.of());
    }

    public static boolean fitsBudget(StyleSequence sequence, WeaponAttributes attributes,
                                     int slotLimit, Set<String> affinityExempt) {
        String category = attributes == null ? null : attributes.category();
        return totalOf(sequence, category, affinityExempt)
                <= budgetOf(attributes, slotLimit) + TOLERANCE;
    }
}
