package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

import net.pixeldreamstudios.combat_styles.attribute.AttackAttributes;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;

import java.util.List;

@Mixin(PlayerAttackHelper.class)
public abstract class SwingScopeMixin {

    @WrapMethod(method = "swapHandAttributes(Lnet/minecraft/world/entity/player/Player;ZLjava/lang/Runnable;)V")
    private static void combat_styles$scopeSwing(Player player, boolean offHand, Runnable action,
                                                 Operation<Void> original) {
        PendingExtraHit.beginHit(player);
        List<AttackAttributes.Applied> applied = AttackAttributes.openScope(player);
        try {
            original.call(player, offHand, action);
        } finally {
            AttackAttributes.closeScope(player, applied);
            PendingExtraHit.pop();
        }
    }
}
