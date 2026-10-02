package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.equipment.BlockEvent;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentActivation;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentHurt;
import net.pixeldreamstudios.combat_styles.equipment.DodgeState;
import net.pixeldreamstudios.combat_styles.equipment.IFrameState;

@Mixin(Player.class)
public abstract class PlayerHurtMixin {

    @Inject(method = "hurt", at = @At("RETURN"), require = 1)
    private void combat_styles$onHurt(DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> callback) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide() || !Boolean.TRUE.equals(callback.getReturnValue())) {
            return;
        }
        EquipmentHurt.react(player, source, amount);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void combat_styles$dodge(DamageSource source, float amount,
                                     CallbackInfoReturnable<Boolean> callback) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        CombatState.recordDamaged(player, source);
        combat_styles$reportBlock(player, source, amount);
        if (source.getDirectEntity() == null) {
            return;
        }
        if (IFrameState.active(player)) {
            callback.setReturnValue(false);
            return;
        }
        if (!DodgeState.shouldDodge(player)) {
            return;
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.7F, 1.4F);
        callback.setReturnValue(false);
    }
    @Unique
    private void combat_styles$reportBlock(Player player, DamageSource source, float amount) {
        if (!player.isBlocking() || !player.isDamageSourceBlocked(source)) {
            return;
        }
        EquipmentActivation.blocked(player, new BlockEvent(source, amount,
                player.getUseItem(), amount <= 0.0F || !source.is(DamageTypeTags.BYPASSES_SHIELD)));
    }
}
