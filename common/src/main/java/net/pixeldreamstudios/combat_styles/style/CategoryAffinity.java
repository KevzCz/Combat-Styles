package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CategoryAffinity {

    public enum Tier {
        NATIVE,
        RELATED,
        ALIEN
    }

    private CategoryAffinity() {
    }

    public static Tier tierOf(String nodeCategory, String weaponCategory) {
        if (nodeCategory == null || weaponCategory == null) {
            return Tier.NATIVE;
        }
        String node = nodeCategory.toLowerCase(Locale.ROOT);
        String weapon = weaponCategory.toLowerCase(Locale.ROOT);
        if (node.equals(weapon)) {
            return Tier.NATIVE;
        }

        Map<String, Set<String>> relations = CombatStylesConfig.get().categoryRelations();
        Set<String> fromWeapon = relations.get(weapon);
        if (fromWeapon != null && fromWeapon.contains(node)) {
            return Tier.RELATED;
        }
        Set<String> fromNode = relations.get(node);
        if (fromNode != null && fromNode.contains(weapon)) {
            return Tier.RELATED;
        }
        return Tier.ALIEN;
    }

    public static CombatStylesConfig.AffinityTier multipliersFor(String nodeCategory,
                                                                 String weaponCategory) {
        CombatStylesConfig config = CombatStylesConfig.get();
        if (!config.categoryAffinityEnabled()) {
            return CombatStylesConfig.AffinityTier.NEUTRAL;
        }
        return switch (tierOf(nodeCategory, weaponCategory)) {
            case NATIVE -> CombatStylesConfig.AffinityTier.NEUTRAL;
            case RELATED -> config.relatedMultipliers();
            case ALIEN -> config.alienMultipliers();
        };
    }

    public static double costMultiplier(String nodeCategory, String weaponCategory) {
        return multipliersFor(nodeCategory, weaponCategory).cost();
    }

    public static StylePage scaled(StylePage page, String weaponCategory) {
        if (page == null) {
            return null;
        }
        CombatStylesConfig.AffinityTier tier = multipliersFor(page.category(), weaponCategory);
        if (tier == CombatStylesConfig.AffinityTier.NEUTRAL) {
            return page;
        }
        boolean preserveArc = CombatStylesConfig.get().preserveArc();
        WeaponAttributes.Attack attack = page.attack();
        WeaponAttributes.Attack scaled = new WeaponAttributes.Attack(
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
        return new StylePage(page.styleId(), page.category(), page.attackIndex(), scaled,
                page.sourceWeapon(), page.rarity());
    }
}
