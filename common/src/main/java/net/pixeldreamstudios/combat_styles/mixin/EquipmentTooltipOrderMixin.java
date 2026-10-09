package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.client.EquipmentTooltipLines;
import net.pixeldreamstudios.combat_styles.item.HeldPreview;

import java.util.List;

@Mixin(value = ItemStack.class, priority = 2000)
public abstract class EquipmentTooltipOrderMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), require = 0)
    private void combat_styles$appendEquipmentLines(Item.TooltipContext context,
                                                    Player player, TooltipFlag flag,
                                                    CallbackInfoReturnable<List<Component>> callback) {
        EquipmentTooltipLines.append((ItemStack) (Object) this, callback.getReturnValue());
    }

    @WrapMethod(method = "getTooltipLines")
    private List<Component> combat_styles$previewAsHeld(Item.TooltipContext context, Player player,
                                                        TooltipFlag flag,
                                                        Operation<List<Component>> original) {
        return HeldPreview.showing((ItemStack) (Object) this,
                () -> original.call(context, player, flag));
    }
}
