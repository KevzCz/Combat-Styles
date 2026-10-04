package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(Zombie.class)
public abstract class SummonZombieMixin {

    @Inject(method = "killedEntity", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noVictimConversion(ServerLevel level, LivingEntity victim,
                                                  CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Zombie) (Object) this)) {
            callback.setReturnValue(true);
        }
    }
}
