package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentUse;

@Mixin(LivingEntity.class)
public abstract class LivingUseMixin {

    @Inject(method = "startUsingItem", at = @At("RETURN"), require = 1)
    private void combat_styles$startUsing(InteractionHand hand, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || !(self instanceof Player player)) {
            return;
        }
        EquipmentUse.begin(player, player.getItemInHand(hand));
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"), require = 1)
    private void combat_styles$stopUsing(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || !(self instanceof Player player)) {
            return;
        }
        ItemStack using = player.getUseItem();
        if (using.isEmpty()) {
            return;
        }
        EquipmentUse.release(player, using, player.getUseItemRemainingTicks());
    }
    @Inject(method = "updatingUsingItem", at = @At("HEAD"), require = 1)
    private void combat_styles$updatingUsingItem(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || !(self instanceof Player player)) {
            return;
        }
        ItemStack using = player.getUseItem();
        if (using.isEmpty()) {
            return;
        }
        EquipmentUse.tick(player, using, player.getUseItemRemainingTicks());
    }
}
