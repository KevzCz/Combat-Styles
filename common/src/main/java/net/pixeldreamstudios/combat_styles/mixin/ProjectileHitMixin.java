package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;
import net.pixeldreamstudios.combat_styles.custom.CustomScopes;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentActivation;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileImpactScope;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileOrigins;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileHitEvent;

@Mixin(LivingEntity.class)
public abstract class ProjectileHitMixin {

    @Inject(method = "hurt", at = @At("RETURN"), require = 1)
    private void combat_styles$onProjectileLanded(DamageSource source, float amount,
                                                  CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        Projectile shot = ProjectileImpactScope.landingFor(source);
        ProjectileUpkeep.SpellShotSpec spellShot = ProjectileUpkeep.spellShotFor(shot);
        if (spellShot != null) {
            if (CombatDebug.enabled(spellShot.owner())) {
                CombatDebug.note(spellShot.owner(), "spellshot — landed " + callback.getReturnValue()
                        + " on " + self.getName().getString() + " health " + self.getHealth());
            }
        }
        if (!Boolean.TRUE.equals(callback.getReturnValue()) || shot == null) {
            return;
        }
        Player owner = EquipmentActivation.ownerOf(shot);
        if (owner == null || owner == self) {
            return;
        }
        Object box = ProjectileUpkeep.boxFor(shot);
        if (box != null) {
            Customs.hit(CustomScopes.PROJECTILE, box, owner, shot, self);
        }
        EquipmentActivation.projectileHit(owner, new ProjectileHitEvent(shot, self, source,
                amount, ProjectileOrigins.travelled(shot),
                !(shot.getOwner() instanceof Player)));
    }
}
