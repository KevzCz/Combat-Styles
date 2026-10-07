package net.pixeldreamstudios.combat_styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.client.ClientBeams;

@Mixin(EntityRenderDispatcher.class)
public abstract class BeamRenderMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void combat_styles$keepBeamSource(E entity, Frustum frustum, double camX,
                                                                 double camY, double camZ,
                                                                 CallbackInfoReturnable<Boolean> callback) {
        if (ClientBeams.has(entity)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private <E extends Entity> void combat_styles$drawBeams(E entity, double x, double y, double z,
                                                            float rotationYaw, float partialTick,
                                                            PoseStack poses, MultiBufferSource buffers,
                                                            int light, CallbackInfo callback) {
        ClientBeams.draw(entity, x, y, z, partialTick, poses, buffers);
    }
}
