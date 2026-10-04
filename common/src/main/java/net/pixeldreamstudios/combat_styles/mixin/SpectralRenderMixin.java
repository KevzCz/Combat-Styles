package net.pixeldreamstudios.combat_styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.client.ClientSummons;

@Mixin(LivingEntityRenderer.class)
public abstract class SpectralRenderMixin {

    @Unique
    private static final int SPECTRAL_COLOR = 0x73DDEEFF;

    @Unique
    private LivingEntity combat_styles$rendering;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void combat_styles$remember(LivingEntity entity, float yaw, float partialTick,
                                        PoseStack pose, MultiBufferSource buffers, int light,
                                        CallbackInfo callback) {
        combat_styles$rendering = entity;
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void combat_styles$spectralType(LivingEntity entity, boolean bodyVisible,
                                            boolean translucent, boolean glowing,
                                            CallbackInfoReturnable<RenderType> callback) {
        if (ClientSummons.spectral(entity)) {
            callback.setReturnValue(RenderType.itemEntityTranslucentCull(
                    ((EntityRenderer) (Object) this).getTextureLocation(entity)));
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int combat_styles$spectralColor(int color) {
        return ClientSummons.spectral(combat_styles$rendering) ? SPECTRAL_COLOR : color;
    }
}
