package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Projectile.class)
public interface ProjectileHitInvoker {

    @Invoker("onHit")
    void combat_styles$onHit(HitResult result);

    @Invoker("canHitEntity")
    boolean combat_styles$canHitEntity(Entity target);
}
