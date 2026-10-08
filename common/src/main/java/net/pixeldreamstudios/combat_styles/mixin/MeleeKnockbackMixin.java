package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.attribute.MeleeKnockback;

@Mixin(Player.class)
public abstract class MeleeKnockbackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void combat_styles$beginKnockbackScale(Entity target, CallbackInfo callback) {
        MeleeKnockback.begin((Player) (Object) this);
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void combat_styles$endKnockbackScale(Entity target, CallbackInfo callback) {
        MeleeKnockback.end();
    }
}
