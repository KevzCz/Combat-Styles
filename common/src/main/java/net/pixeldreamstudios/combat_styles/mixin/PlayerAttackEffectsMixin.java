package net.pixeldreamstudios.combat_styles.mixin;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;

@Mixin(Player.class)
public abstract class PlayerAttackEffectsMixin {

    @Inject(method = "attack", at = @At("HEAD"), require = 1)
    private void combat_styles$markAlive(Entity target, CallbackInfo callback) {
        Player player = (Player) (Object) this;
        CombatEffectDealer.consumeConverted();
        boolean server = !player.level().isClientSide();
        boolean extra = server && PendingExtraHit.index() >= 0;
        boolean swept = server && SwingDrag.claimSwept(player);
        boolean spent = server && PendingExtraHit.struck(player);
        boolean restore = extra || spent || SwingDrag.sweeping(player) || swept;
        float before = player.getAttackStrengthScale(0.0F);
        if (restore) {
            AttackStrengthAccessor accessor = (AttackStrengthAccessor) player;
            combat_styles$borrowedTicker.set(accessor.combat_styles$attackStrengthTicker());
            accessor.combat_styles$setAttackStrengthTicker(
                    (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()) + 1);
        }
        if (extra) {
            PendingExtraHit.markStruck(player);
        }
        if (CombatStylesConfig.get().debugLogging()) {
            CombatStyles.LOGGER.info(
                    "[ATTACK] side={} tick={} before={} after={} extra={} swept={} restore={}",
                    player.level().isClientSide() ? "client" : "server",
                    player.tickCount, before, player.getAttackStrengthScale(0.0F),
                    PendingExtraHit.index(), swept, restore);
        }
        DamageDealer.recordConnected(false);
        CombatEffectDealer.markTargetAlive(target);
    }

    @Unique
    private static final ThreadLocal<Integer> combat_styles$borrowedTicker =
            new ThreadLocal<>();

    @Inject(method = "attack", at = @At("RETURN"), require = 1)
    private void combat_styles$returnStrength(Entity target, CallbackInfo callback) {
        Integer borrowed = combat_styles$borrowedTicker.get();
        if (borrowed == null) {
            return;
        }
        combat_styles$borrowedTicker.remove();
        Player player = (Player) (Object) this;
        AttackStrengthAccessor accessor = (AttackStrengthAccessor) player;
        accessor.combat_styles$setAttackStrengthTicker(
                Math.max(borrowed, accessor.combat_styles$attackStrengthTicker()));
    }

    @Inject(method = "attack", at = @At("TAIL"), require = 1)
    private void combat_styles$applyAffix(Entity target, CallbackInfo callback) {
        Player player = (Player) (Object) this;
        boolean converted = CombatEffectDealer.consumeConverted();
        float dealt = DamageDealer.lastDealtDamage(
                (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));

        if (!(target instanceof LivingEntity living)) {
            return;
        }
        if (!DamageDealer.consumeConnected()) {
            return;
        }
        CombatEffectDealer.recordLanded(player, dealt);

        if (converted) {
            return;
        }
        CombatEffectDealer.onPlayerHit(player, living, dealt);
    }
}
