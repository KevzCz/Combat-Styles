package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.equipment.SwingDash;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;

@Mixin(Player.class)
public class SwingDelayMixin {

    @Unique
    private static final ThreadLocal<Boolean> combat_styles$hastening =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "tick", at = @At("TAIL"), require = 1)
    private void combat_styles$dropHasteOnWeaponChange(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        SwingHaste.trackHeldWeapon(player);
        if (player.level().isClientSide()) {
            SwingDash.tick(player);
        }
    }

    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"),
            cancellable = true, require = 1)
    private void combat_styles$hastenDelay(CallbackInfoReturnable<Float> callback) {
        if (combat_styles$hastening.get() || SwingHaste.isReadingSwingDuration()) {
            return;
        }
        Player player = (Player) (Object) this;
        combat_styles$hastening.set(Boolean.TRUE);
        try {
            float multiplier = SwingHaste.clamped(player);
            if (multiplier == SwingHaste.NEUTRAL_MULTIPLIER) {
                return;
            }
            float scaled = Math.max(SwingHaste.MIN_SWING_TICKS,
                    callback.getReturnValueF() * multiplier);
            if (CombatStylesConfig.get().debugLogging()) {
                CombatStyles.LOGGER.info("[DELAY] side={} base={} mult={} scaled={}",
                        player.level().isClientSide() ? "client" : "server",
                        callback.getReturnValueF(), multiplier, scaled);
            }
            callback.setReturnValue(scaled);
        } finally {
            combat_styles$hastening.set(Boolean.FALSE);
        }
    }
}
