package net.pixeldreamstudios.combat_styles.mixin;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;
import net.pixeldreamstudios.combat_styles.entity.SummonData;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileImpactScope;

@Mixin(LivingEntity.class)
public abstract class ProjectileDamageMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void combat_styles$retypeProjectile(DamageSource source, float amount,
                                                CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || DamageDealer.isDealing() || amount <= 0.0F) {
            return;
        }
        Projectile landing = ProjectileImpactScope.landingFor(source);
        Entity direct = landing != null ? landing : source.getDirectEntity();
        ProjectileUpkeep.SpellShotSpec spellShot = ProjectileUpkeep.spellShotFor(direct);
        boolean unowned = false;
        if (spellShot == null) {
            spellShot = ProjectileUpkeep.spellShotFor(ProjectileImpactScope.carrierFor(source));
            unowned = spellShot != null;
        }
        if (spellShot == null) {
            spellShot = ProjectileUpkeep.castingShotFor(source);
            unowned = spellShot != null;
        }
        if (spellShot != null) {
            if (CombatDebug.enabled(spellShot.owner())) {
                CombatDebug.note(spellShot.owner(), "spellshot — hurt " + self.getName().getString()
                        + " amount " + amount
                        + " path " + (spellShot.retypes() ? "retype" : "iframe-clear")
                        + " invulnerable " + self.invulnerableTime
                        + " lastHurt " + ((LivingEntityHurtAccessor) self).combat_styles$getLastHurt());
            }
        }
        if (spellShot != null && spellShot.retypes()) {
            DamageDealer.beginDealing();
            try {
                DamageDealer.dealSpec(spellShot.owner(), self, spellShot.spec(), amount, source);
            } finally {
                DamageDealer.endDealing();
            }
            callback.setReturnValue(true);
            return;
        }
        if (unowned) {
            if (spellShot.ignoresIframes()) {
                DamageHurt.clearIFrames(self);
            }
            return;
        }
        DamageSpec spec = ProjectileUpkeep.specFor(direct);
        if (spec == null) {
            if (ProjectileUpkeep.ignoresIframes(direct)) {
                DamageHurt.clearIFrames(self);
            }
            return;
        }
        if (direct instanceof SummonData carried
                && carried.combat_styles$summonOwner() != null) {
            return;
        }
        Player owner = ProjectileUpkeep.ownerFor(direct);
        ProjectileUpkeep.untrack(direct);
        float resolved = spec.isEmpty()
                ? amount
                : spec.amountFor(owner, amount, self);
        DamageDealer.dealSpec(owner, self, spec, resolved, source);
        direct.discard();
        callback.setReturnValue(true);
    }
}
