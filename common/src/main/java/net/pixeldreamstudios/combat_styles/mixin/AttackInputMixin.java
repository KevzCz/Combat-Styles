package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.client.AttackInput;

@Mixin(value = Minecraft.class, priority = 1500)
public class AttackInputMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), require = 1)
    private void combat_styles$markClick(CallbackInfoReturnable<Boolean> callback) {
        AttackInput.markClick();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), require = 1)
    private void combat_styles$markHold(boolean attacking, CallbackInfo callback) {
        if (attacking) {
            AttackInput.markHold();
        }
    }
}
