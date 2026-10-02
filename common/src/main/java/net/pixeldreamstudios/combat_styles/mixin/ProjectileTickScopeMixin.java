package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;
import net.pixeldreamstudios.combat_styles.affix.SpellShotSweep;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileImpactScope;

import java.util.Locale;

@Mixin(ServerLevel.class)
public abstract class ProjectileTickScopeMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), require = 1)
    private void combat_styles$enterProjectileTick(Entity entity, CallbackInfo callback) {
        if (entity instanceof Projectile projectile) {
            ProjectileImpactScope.enter(projectile);
        } else if (SpellEngineCompat.isSpellCloud(entity)) {
            ProjectileImpactScope.enterCarrier(entity);
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"), require = 1)
    private void combat_styles$exitProjectileTick(Entity entity, CallbackInfo callback) {
        if (!(entity instanceof Projectile) && SpellEngineCompat.isSpellCloud(entity)) {
            ProjectileImpactScope.exitCarrier(entity);
            return;
        }
        if (entity instanceof Projectile projectile) {
            ProjectileImpactScope.exit(projectile);
            SpellShotSweep.sweep(projectile);
            if (projectile.isRemoved()) {
                ProjectileUpkeep.SpellShotSpec shot = ProjectileUpkeep.spellShotFor(projectile);
                if (shot != null) {
                    if (CombatDebug.enabled(shot.owner())) {
                        CombatDebug.note(shot.owner(), "spellshot — removed " + projectile.getRemovalReason()
                                + " travelled " + String.format(Locale.ROOT, TRAVEL_FORMAT,
                                projectile.moveDist) + " age " + projectile.tickCount);
                    }
                }
            }
        }
    }

    private static final String TRAVEL_FORMAT = "%.1f";
}
