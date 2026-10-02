package net.pixeldreamstudios.combat_styles.mixin;

import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.style.SwingHolder;

@Mixin(PlayerAttackHelper.class)
public class SwingHolderMixin {

    @Inject(method = "getCurrentAttack", at = @At("HEAD"), require = 1)
    private static void combat_styles$pushHolder(Player player, int combo,
                                                 CallbackInfoReturnable<AttackHand> callback) {
        SwingHolder.push(player);
    }

    @Inject(method = "getCurrentAttack", at = @At("RETURN"), require = 1)
    private static void combat_styles$popHolder(Player player, int combo,
                                                CallbackInfoReturnable<AttackHand> callback) {
        SwingHolder.pop();
    }

}
