package net.pixeldreamstudios.combat_styles.mixin;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.ClaimedHits;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.style.HitboxClaims;

@Mixin(LivingEntity.class)
public abstract class HitDamageMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 1)
    private float combat_styles$foldExecute(float amount, DamageSource source) {
        if (DamageDealer.isDealing() || amount <= 0.0F) {
            return amount;
        }
        if (!(source.getEntity() instanceof Player player) || !source.is(DamageTypes.PLAYER_ATTACK)) {
            return amount;
        }
        LivingEntity self = (LivingEntity) (Object) this;

        float base = amount;
        DamageSpec claimed = HitboxClaims.specFor(player, self);
        if (claimed != null) {
            base = claimed.amountFor(player, amount, self);
        }
        base = ClaimedHits.scaledByClaimedBox(player, self, base);

        DamageDealer.recordBaseDamage(base);
        return base
                + CombatEffectDealer.executeBonusFor(player, self, base)
                + CombatEffectDealer.amplifyBonusFor(player, self, base);
    }

    @Inject(method = "hurt", at = @At("RETURN"), require = 1)
    private void combat_styles$recordConnected(DamageSource source, float amount,
                                               CallbackInfoReturnable<Boolean> callback) {
        if (DamageDealer.isDealing()
                || !(source.getEntity() instanceof Player)
                || !source.is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        DamageDealer.recordConnected(Boolean.TRUE.equals(callback.getReturnValue()));
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void combat_styles$convertPhysical(DamageSource source, float amount,
                                               CallbackInfoReturnable<Boolean> callback) {
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }
        if (!source.is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        CombatState.recordDealtType(player, source);
        if (!DamageDealer.isDealing()) {
            CombatEffectDealer.recordLanded(player, amount);
        }
        if (DamageDealer.isDealing()) {
            return;
        }
        DamageDealer.recordDealtDamage(DamageDealer.baseDamage(amount));

        LivingEntity self = (LivingEntity) (Object) this;
        if (ClaimedHits.retypeClaimedHit(player, self, source, amount)) {
            callback.setReturnValue(true);
            return;
        }
        if (player.level().isClientSide() || !CombatEffectDealer.convertsHit(player, self, amount)) {
            return;
        }
        CombatEffectDealer.onPlayerHit(player, self, amount, true);
        callback.setReturnValue(true);
    }
}
