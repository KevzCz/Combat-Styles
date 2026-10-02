package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.client.tooltip.StyleTooltipRenderer;

import java.util.List;
import java.util.Optional;

@Mixin(GuiGraphics.class)
public abstract class StyleTooltipRenderMixin {

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void combat_styles$renderStyleTooltip(Font font, ItemStack stack,
                                                  int mouseX, int mouseY,
                                                  CallbackInfo callback) {
        if (!StyleTooltipRenderer.handles(stack)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen == null) {
            return;
        }
        List<Component> lines = stack.getTooltipLines(
                Item.TooltipContext.of(minecraft.level),
                minecraft.player,
                minecraft.options.advancedItemTooltips
                        ? TooltipFlag.Default.ADVANCED
                        : TooltipFlag.Default.NORMAL);

        StyleTooltipRenderer.render((GuiGraphics) (Object) this, font, stack, lines,
                mouseX, mouseY, screen.width, screen.height);
        callback.cancel();
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;"
            + "Ljava/util/Optional;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void combat_styles$renderPlatedTooltip(Font font, List<Component> lines,
                                                   Optional<TooltipComponent> image,
                                                   int mouseX, int mouseY,
                                                   CallbackInfo callback) {
        if (!StyleTooltipRenderer.hasPlates(lines)) {
            return;
        }
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }
        StyleTooltipRenderer.renderLines((GuiGraphics) (Object) this, font, lines, image,
                mouseX, mouseY, screen.width, screen.height);
        callback.cancel();
    }
}
