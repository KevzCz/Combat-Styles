package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentActivation;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileOrigins;

@Mixin(ServerLevel.class)
public abstract class ProjectileShotMixin {

    @Inject(method = "addFreshEntity", at = @At("RETURN"), require = 1)
    private void combat_styles$onProjectileSpawned(Entity entity,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (!Boolean.TRUE.equals(callback.getReturnValue())) {
            return;
        }
        if (!(entity instanceof Projectile shot)) {
            return;
        }
        Player owner = EquipmentActivation.ownerOf(shot);
        if (owner == null) {
            return;
        }
        ProjectileOrigins.remember(shot);
        EquipmentActivation.shot(owner, shot, !(shot.getOwner() instanceof Player));
    }
}
