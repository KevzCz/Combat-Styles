package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.debug.SpellShotTrace;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileImpactScope;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {

    @Inject(method = "onHit", at = @At("HEAD"), require = 1)
    private void combat_styles$enterImpact(HitResult result, CallbackInfo callback) {
        Projectile self = (Projectile) (Object) this;
        ProjectileImpactScope.enter(self);
        SpellShotTrace.impact(self, result);
    }

    @Inject(method = "onHit", at = @At("RETURN"), require = 1)
    private void combat_styles$exitImpact(HitResult result, CallbackInfo callback) {
        ProjectileImpactScope.exit((Projectile) (Object) this);
    }
}
