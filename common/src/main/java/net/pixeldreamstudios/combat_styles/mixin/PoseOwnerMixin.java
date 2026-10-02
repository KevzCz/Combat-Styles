package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.client.PoseOwner;

@Mixin(AbstractClientPlayer.class)
public class PoseOwnerMixin {

    @Inject(method = "updateAnimationsOnTick", at = @At("HEAD"), remap = false, require = 1)
    private void combat_styles$markPoseOwner(CallbackInfo callback) {
        PoseOwner.push((AbstractClientPlayer) (Object) this);
    }

    @Inject(method = "updateAnimationsOnTick", at = @At("RETURN"), remap = false, require = 1)
    private void combat_styles$clearPoseOwner(CallbackInfo callback) {
        PoseOwner.pop();
    }
}
