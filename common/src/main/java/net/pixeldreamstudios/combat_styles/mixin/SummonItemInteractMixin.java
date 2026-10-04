package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(ItemStack.class)
public abstract class SummonItemInteractMixin {

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void combat_styles$guardSummon(Player player, LivingEntity target, InteractionHand hand,
                                           CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (SummonOwner.isSummon(target) && !self.is(Items.LEAD) && !self.is(Items.NAME_TAG)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
