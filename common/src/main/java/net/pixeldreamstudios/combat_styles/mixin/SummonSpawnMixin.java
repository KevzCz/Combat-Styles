package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonLifecycle;

@Mixin(ServerLevel.class)
public abstract class SummonSpawnMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void combat_styles$stampSummonedSpawns(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        SummonLifecycle.freshlySpawned(entity);
    }
}
