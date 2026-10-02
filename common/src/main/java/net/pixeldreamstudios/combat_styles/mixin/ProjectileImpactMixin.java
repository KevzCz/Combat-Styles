package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileImpactScope;

import java.util.Locale;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {

    @Inject(method = "onHit", at = @At("HEAD"), require = 1)
    private void combat_styles$enterImpact(HitResult result, CallbackInfo callback) {
        Projectile self = (Projectile) (Object) this;
        ProjectileImpactScope.enter(self);
        if (self.level().isClientSide()) {
            return;
        }
        ProjectileUpkeep.SpellShotSpec shot = ProjectileUpkeep.spellShotFor(self);
        if (shot != null) {
            String struck = result instanceof EntityHitResult entityHit
                    ? " " + entityHit.getEntity().getName().getString()
                    : "";
            if (CombatDebug.enabled(shot.owner())) {
                CombatDebug.note(shot.owner(), "spellshot — onHit " + result.getType() + struck
                        + " travelled " + String.format(Locale.ROOT, TRAVEL_FORMAT, self.moveDist));
            }
        }
    }

    private static final String TRAVEL_FORMAT = "%.1f";

    @Inject(method = "onHit", at = @At("RETURN"), require = 1)
    private void combat_styles$exitImpact(HitResult result, CallbackInfo callback) {
        ProjectileImpactScope.exit((Projectile) (Object) this);
    }
}
