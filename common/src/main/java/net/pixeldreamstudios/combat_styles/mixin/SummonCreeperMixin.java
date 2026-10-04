package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(Creeper.class)
public abstract class SummonCreeperMixin {

    @ModifyArg(method = "explodeCreeper", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"),
            index = 5)
    private Level.ExplosionInteraction combat_styles$noTerrainDamage(Level.ExplosionInteraction interaction) {
        return SummonOwner.isSummon((Creeper) (Object) this) ? Level.ExplosionInteraction.NONE : interaction;
    }
}
