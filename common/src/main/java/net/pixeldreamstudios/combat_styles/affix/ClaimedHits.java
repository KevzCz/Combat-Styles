package net.pixeldreamstudios.combat_styles.affix;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackShaping;
import net.pixeldreamstudios.combat_styles.style.HitboxClaims;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;

import java.util.Optional;

public final class ClaimedHits {

    private ClaimedHits() {
    }

    public static float scaledByClaimedBox(Player player, LivingEntity target, float amount) {
        HitboxSet.Shape claimed = HitboxClaims.shapeFor(player, target);
        if (claimed == null) {
            return amount;
        }
        Double own = claimed.overrideOrEmpty().damageMultiplier().orElse(null);
        if (own == null) {
            return amount;
        }
        WeaponAttributes.Attack swung = CombatState.swungAttack(player);
        double parent = swung == null ? 1.0D : swung.damageMultiplier();
        if (parent <= 0.0D) {
            return amount;
        }
        return (float) (amount * (own / parent));
    }

    private static String declaredDamageType(Player player, LivingEntity target) {
        HitboxSet.Shape claimed = HitboxClaims.shapeFor(player, target);
        if (claimed != null) {
            String boxType = claimed.overrideOrEmpty().damageType().orElse(null);
            if (boxType != null && !boxType.isEmpty()) {
                return boxType;
            }
        }
        AttackOverride override = AttackShaping.overrideFor(player,
                CombatState.swungAttack(player));
        if (override == null) {
            return null;
        }
        String declared = override.damageType().orElse(null);
        return declared == null || declared.isEmpty() ? null : declared;
    }

    public static boolean retypeClaimedHit(Player player, LivingEntity target,
                                           DamageSource source, float amount) {
        if (player == null || target == null || player.level().isClientSide()
                || amount < AffixImpacts.MINIMUM_DAMAGE) {
            return false;
        }
        DamageSpec claimed = HitboxClaims.specFor(player, target);
        DamageSpec spec = claimed;
        if (spec == null || !spec.retypes()) {
            String declared = declaredDamageType(player, target);
            if (declared == null) {
                return false;
            }
            spec = (claimed == null ? DamageSpec.NONE : claimed)
                    .withDamageType(Optional.of(declared));
        }
        DamageSpec resolved = spec;
        if (!SwingTotals.defer(player, target, amount, false)) {
            CombatEffectDealer.onPlayerHit(player, target, amount);
        }
        DamageDealer.beginDealing();
        try {
            CombatEffectDealer.withAttackAffixes(
                    CombatEffectDealer.affixesForCurrentAttack(player,
                            CombatEffectDealer.swingingWeapon(player)),
                    () -> DamageDealer.dealSpec(player, target, resolved, amount, source));
        } finally {
            DamageDealer.endDealing();
        }
        return true;
    }
}
