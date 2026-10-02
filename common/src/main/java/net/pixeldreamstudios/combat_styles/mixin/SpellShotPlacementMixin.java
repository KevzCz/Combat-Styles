package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import net.pixeldreamstudios.combat_styles.affix.SpellShotPlacement;

@Pseudo
@Mixin(targets = "net.spell_engine.internals.delivery.ProjectileLauncher", remap = false)
public abstract class SpellShotPlacementMixin {

    @WrapOperation(
            method = "shootProjectile*",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = true),
            remap = false)
    private static boolean combat_styles$placeShot(Level level, Entity shot,
                                                   Operation<Boolean> original) {
        SpellShotPlacement.place(shot);
        return original.call(level, shot);
    }
}
