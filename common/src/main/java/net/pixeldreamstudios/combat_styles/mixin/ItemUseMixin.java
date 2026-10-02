package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentUse;
import net.pixeldreamstudios.combat_styles.equipment.UseSpec;

@Mixin(Item.class)
public abstract class ItemUseMixin {

    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true, require = 1)
    private void combat_styles$useDuration(ItemStack stack, LivingEntity holder,
                                           CallbackInfoReturnable<Integer> callback) {
        if (callback.getReturnValueI() > 0) {
            return;
        }
        int declared = EquipmentUse.durationOf(stack);
        if (declared > 0) {
            callback.setReturnValue(declared);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("RETURN"), cancellable = true, require = 1)
    private void combat_styles$useAnimation(ItemStack stack,
                                            CallbackInfoReturnable<UseAnim> callback) {
        if (callback.getReturnValue() != UseAnim.NONE) {
            return;
        }
        UseSpec spec = EquipmentUse.specOf(stack);
        if (spec.isEmpty()) {
            return;
        }
        callback.setReturnValue(spec.useAnimation());
    }

    @Inject(method = "use", at = @At("RETURN"), cancellable = true, require = 1)
    private void combat_styles$use(Level level, Player player, InteractionHand hand,
                                   CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        ItemStack held = player.getItemInHand(hand);
        UseSpec spec = EquipmentUse.specOf(held);
        if (spec.isEmpty()) {
            return;
        }
        if (EquipmentUse.blocked(player, held)) {
            return;
        }
        if (spec.channels()) {
            if (!player.isUsingItem()) {
                player.startUsingItem(hand);
                if (level.isClientSide()) {
                    EquipmentUse.channelStarted(player, held);
                }
                callback.setReturnValue(InteractionResultHolder.consume(held));
            }
            return;
        }
        if (level.isClientSide()) {
            EquipmentUse.pressed(player, held);
            return;
        }
        EquipmentUse.begin(player, held);
    }

}
