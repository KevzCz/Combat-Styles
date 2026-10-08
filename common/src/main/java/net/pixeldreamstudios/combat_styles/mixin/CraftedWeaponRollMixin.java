package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;

@Mixin(Item.class)
public abstract class CraftedWeaponRollMixin {

    @Inject(method = "onCraftedBy", at = @At("TAIL"))
    private void combat_styles$rollCraftedBy(ItemStack stack, Level level, Player player,
                                             CallbackInfo callback) {
        if (!level.isClientSide()) {
            WeaponRolls.applyCrafted(stack, level.getRandom());
        }
    }

    @Inject(method = "onCraftedPostProcess", at = @At("TAIL"))
    private void combat_styles$rollCrafted(ItemStack stack, Level level, CallbackInfo callback) {
        if (!level.isClientSide()) {
            WeaponRolls.applyCrafted(stack, level.getRandom());
        }
    }
}
