package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.combat_styles.client.ScreenScaleState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiGraphics.class)
public abstract class ScissorScaleMixin {

    @WrapMethod(method = "enableScissor")
    private void combat_styles$scaleScissor(int minX, int minY, int maxX, int maxY, Operation<Void> original) {
        if (!ScreenScaleState.isActive()) {
            original.call(minX, minY, maxX, maxY);
            return;
        }
        float scale = ScreenScaleState.activeScale();
        original.call(Mth.floor(minX * scale), Mth.floor(minY * scale), Mth.ceil(maxX * scale), Mth.ceil(maxY * scale));
    }
}
