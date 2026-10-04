package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.pixeldreamstudios.combat_styles.client.ClientSummons;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class OwnSummonPickMixin {

    @ModifyArg(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
            index = 4)
    private Predicate<Entity> combat_styles$pickThroughOwnSummons(Predicate<Entity> pickable) {
        return pickable.and(entity -> !ClientSummons.ownedByLocalPlayer(entity));
    }
}
