package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;

@Pseudo
@Mixin(targets = "net.spell_engine.entity.SpellProjectile", remap = false)
public abstract class SpellHitboxCoverageMixin {

    @ModifyVariable(method = "performVolumetricEntityCollision", at = @At("STORE"),
            name = "obbLength", remap = false)
    private float combat_styles$coverTravel(float length) {
        Projectile self = (Projectile) (Object) this;
        if (ProjectileUpkeep.spellShotFor(self) == null) {
            return length;
        }
        return (float) Math.max(length, self.getDeltaMovement().length());
    }
}
