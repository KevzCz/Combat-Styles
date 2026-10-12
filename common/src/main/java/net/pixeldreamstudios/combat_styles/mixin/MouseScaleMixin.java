package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.pixeldreamstudios.combat_styles.client.ScaledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseScaleMixin {

    @WrapOperation(method = {"onPress", "onScroll", "onMove"}, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
    private int combat_styles$virtualWidth(Window window, Operation<Integer> original) {
        int real = original.call(window);
        return Minecraft.getInstance().screen instanceof ScaledScreen scaled && scaled.isScaledDown() ? scaled.virtualWidth() : real;
    }

    @WrapOperation(method = {"onPress", "onScroll", "onMove"}, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int combat_styles$virtualHeight(Window window, Operation<Integer> original) {
        int real = original.call(window);
        return Minecraft.getInstance().screen instanceof ScaledScreen scaled && scaled.isScaledDown() ? scaled.virtualHeight() : real;
    }
}
