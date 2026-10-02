package net.pixeldreamstudios.combat_styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.client.ExtraHitDebugRenderer;

@Mixin(DebugRenderer.class)
public class ExtraHitDebugMixin {

    @Unique
    private static boolean combat_styles$mutedBaseCollider;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void combat_styles$renderExtraHitboxes(PoseStack poses,
                                                   MultiBufferSource.BufferSource buffers,
                                                   double cameraX, double cameraY, double cameraZ,
                                                   CallbackInfo callback) {
        ExtraHitDebugRenderer.render(poses, buffers, cameraX, cameraY, cameraZ);
        combat_styles$mutedBaseCollider = ExtraHitDebugRenderer.muteBaseCollider();
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void combat_styles$restoreBaseCollider(PoseStack poses,
                                                   MultiBufferSource.BufferSource buffers,
                                                   double cameraX, double cameraY, double cameraZ,
                                                   CallbackInfo callback) {
        if (combat_styles$mutedBaseCollider) {
            combat_styles$mutedBaseCollider = false;
            ExtraHitDebugRenderer.unmuteBaseCollider();
        }
    }
}
