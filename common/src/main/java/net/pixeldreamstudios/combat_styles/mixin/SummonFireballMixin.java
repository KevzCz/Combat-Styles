package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(LargeFireball.class)
public abstract class SummonFireballMixin {

    @ModifyArg(method = "onHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"),
            index = 5)
    private boolean combat_styles$noSummonFire(boolean fire) {
        return !SummonOwner.isSummon(((LargeFireball) (Object) this).getOwner()) && fire;
    }

    @ModifyArg(method = "onHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"),
            index = 6)
    private Level.ExplosionInteraction combat_styles$noSummonTerrain(Level.ExplosionInteraction interaction) {
        return SummonOwner.isSummon(((LargeFireball) (Object) this).getOwner())
                ? Level.ExplosionInteraction.NONE
                : interaction;
    }
}
