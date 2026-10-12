package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.pixeldreamstudios.combat_styles.client.ScaledScreen;
import net.pixeldreamstudios.combat_styles.client.ScreenScaleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class ScreenScaleMixin {

    @WrapMethod(method = "init(Lnet/minecraft/client/Minecraft;II)V")
    private void combat_styles$initInVirtualSpace(Minecraft minecraft, int width, int height, Operation<Void> original) {
        if (!((Object) this instanceof ScaledScreen scaled)) {
            original.call(minecraft, width, height);
            return;
        }
        scaled.fitTo(width, height);
        original.call(minecraft, scaled.virtualWidth(), scaled.virtualHeight());
    }

    @WrapOperation(method = "renderWithTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void combat_styles$renderScaled(Screen screen, GuiGraphics graphics, int mouseX, int mouseY,
                                            float partialTick, Operation<Void> original) {
        if (!(screen instanceof ScaledScreen scaled) || !scaled.isScaledDown()) {
            original.call(screen, graphics, mouseX, mouseY, partialTick);
            return;
        }
        float scale = scaled.uiScale();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, ScaledScreen.FULL_SCALE);
        ScreenScaleState.begin(scale);
        try {
            original.call(screen, graphics, (int) (mouseX / scale), (int) (mouseY / scale), partialTick);
        } finally {
            ScreenScaleState.end();
            graphics.pose().popPose();
        }
    }
}
