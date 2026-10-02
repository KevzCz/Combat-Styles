package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.client.tooltip.SequenceMark;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class SequenceMarkMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true, require = 1)
    private void combat_styles$markSequence(Item.TooltipContext context, Player player,
                                            TooltipFlag flag,
                                            CallbackInfoReturnable<List<Component>> callback) {
        List<Component> lines = callback.getReturnValue();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        Component marked = SequenceMark.decorate(lines.getFirst(), stack);
        if (marked == lines.getFirst()) {
            return;
        }
        try {
            lines.set(0, marked);
        } catch (UnsupportedOperationException ignored) {
            List<Component> copy = new ArrayList<>(lines);
            copy.set(0, marked);
            callback.setReturnValue(copy);
        }
    }
}
