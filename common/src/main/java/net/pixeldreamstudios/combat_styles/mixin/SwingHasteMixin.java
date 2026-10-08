package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponSwing;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.SwingScales;
import net.pixeldreamstudios.combat_styles.client.AttackInput;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.SwingDash;
import net.pixeldreamstudios.combat_styles.equipment.PassDriver;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;

@Mixin(value = Minecraft.class, priority = 1500)
public class SwingHasteMixin {

    @Shadow
    private int rightClickDelay;

    @Shadow
    protected int missTime;

    @SuppressWarnings(SHADOW_TARGET_ADDED_BY_BETTER_COMBAT)
    @Shadow(remap = false)
    private WeaponSwing ongoingSwing;

    @SuppressWarnings(SHADOW_TARGET_ADDED_BY_BETTER_COMBAT)
    @Shadow(remap = false)
    private int comboReset;

    @SuppressWarnings(SHADOW_TARGET_ADDED_BY_BETTER_COMBAT)
    @Shadow(remap = false)
    private int lastAttacked;

    @SuppressWarnings(SHADOW_TARGET_ADDED_BY_BETTER_COMBAT)
    @Shadow(remap = false)
    private float lastSwingDuration;

    @Unique
    private static final String SHADOW_TARGET_ADDED_BY_BETTER_COMBAT = "target";

    @Unique
    private static int combat_styles$lastStart = -1;

    @Unique
    private static int combat_styles$lastNeeded = 0;

    @WrapOperation(
            method = "startUpswing",
            at = @At(value = "INVOKE",
                    target = "Lnet/bettercombat/logic/PlayerAttackHelper;"
                            + "getAttackCooldownTicksCapped(Lnet/minecraft/world/entity/player/Player;)F"),
            remap = false,
            require = 1)
    private float combat_styles$beginHastenedSwing(Player target,
                                                   Operation<Float> original) {
        float duration;
        SwingHaste.beginReadingSwingDuration();
        try {
            duration = original.call(target);
        } finally {
            SwingHaste.endReadingSwingDuration();
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return duration;
        }
        MinecraftClient_BetterCombat client =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        int raw = client.getComboCount();
        combat_styles$scalingAnimation = SwingScales.scalesAnimation(player, raw);
        combat_styles$animationScale = combat_styles$scalingAnimation
                ? SwingScales.cooldownScaleFor(player, raw)
                : SwingHaste.NEUTRAL_MULTIPLIER;
        if (!combat_styles$scalingAnimation) {
            return duration;
        }
        float scaled = Math.max(SwingHaste.MIN_SWING_TICKS,
                duration * combat_styles$animationScale);
        if (CombatStylesConfig.get().debugLogging()) {
            CombatStyles.LOGGER.info("[CD] scale_animation combo={} duration {} -> {}",
                    raw, duration, scaled);
        }
        return scaled;
    }

    @Unique
    private static boolean combat_styles$scalingAnimation;

    @Unique
    private static float combat_styles$animationScale = SwingHaste.NEUTRAL_MULTIPLIER;

    @ModifyVariable(
            method = "startUpswing",
            at = @At(value = "STORE", ordinal = 0),
            index = 5,
            remap = false,
            require = 1)
    private int combat_styles$hastenAttackCooldown(int attackCooldownTicks) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return attackCooldownTicks;
        }
        if (combat_styles$scalingAnimation) {
            if (combat_styles$animationScale <= 0.0F) {
                return attackCooldownTicks;
            }
            float extra = Math.min(combat_styles$animationScale, SwingHaste.clamped(player))
                    / combat_styles$animationScale;
            return extra >= SwingHaste.NEUTRAL_MULTIPLIER
                    ? attackCooldownTicks
                    : Math.max(1, Math.round(attackCooldownTicks * extra));
        }
        MinecraftClient_BetterCombat client =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        int raw = client.getComboCount();
        float multiplier = Math.min(SwingScales.cooldownScaleFor(player, raw),
                SwingHaste.clamped(player));
        int result = multiplier >= SwingHaste.NEUTRAL_MULTIPLIER
                ? attackCooldownTicks
                : Math.max(1, Math.round(attackCooldownTicks * multiplier));
        if (multiplier > SwingHaste.NEUTRAL_MULTIPLIER) {
            combat_styles$lastStart = player.tickCount;
            combat_styles$lastNeeded = result;
        } else {
            combat_styles$lastStart = -1;
            combat_styles$lastNeeded = 0;
        }
        if (CombatStylesConfig.get().debugLogging()) {
            CombatStyles.LOGGER.info("[CD] rawCombo={} scale={} in={} out={} self={}",
                    raw, multiplier, attackCooldownTicks, result,
                    SwingHaste.clamped(player));
        }
        return result;
    }

    @WrapOperation(
            method = "startUpswing",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;round(F)I", ordinal = 1),
            remap = false,
            require = 1)
    private int combat_styles$stretchComboWindow(float ticks, Operation<Integer> original) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return original.call(ticks);
        }
        if (combat_styles$scalingAnimation) {
            float scale = combat_styles$animationScale;
            return original.call(scale > 0.0F && scale < SwingHaste.NEUTRAL_MULTIPLIER
                    ? ticks / scale
                    : ticks);
        }
        MinecraftClient_BetterCombat client =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        float multiplier = SwingScales.comboWindowScaleFor(player,
                client.getComboCount());
        int result = original.call(multiplier > SwingHaste.NEUTRAL_MULTIPLIER
                ? ticks * multiplier
                : ticks);
        return result;
    }

    @Unique
    private static int combat_styles$pendingUpswing;

    @ModifyArg(
            method = "startUpswing",
            at = @At(value = "INVOKE",
                    target = "Lnet/bettercombat/logic/WeaponSwing;<init>"
                            + "(Lnet/bettercombat/api/AttackHand;IIF)V"),
            index = 2,
            remap = false,
            require = 1)
    private int combat_styles$captureUpswing(int upswingTicks) {
        combat_styles$pendingUpswing = upswingTicks;
        return upswingTicks;
    }

    @ModifyArg(
            method = "startUpswing",
            at = @At(value = "INVOKE",
                    target = "Lnet/bettercombat/logic/WeaponSwing;<init>"
                            + "(Lnet/bettercombat/api/AttackHand;IIF)V"),
            index = 3,
            remap = false,
            require = 1)
    private float combat_styles$hastenRecoveryTail(float duration) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return duration;
        }
        MinecraftClient_BetterCombat combat =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        float multiplier = SwingScales.upcomingTimingScaleFor(player,
                combat.getComboCount() + 1);
        if (multiplier >= SwingHaste.NEUTRAL_MULTIPLIER) {
            return duration;
        }
        int upswing = combat_styles$pendingUpswing;
        float floor = upswing + 1.0F;
        if (duration <= floor) {
            return duration;
        }
        float scaled = Math.max(floor, upswing + (duration - upswing) * multiplier);
        return scaled;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void combat_styles$publishSequenceScale(CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        MinecraftClient_BetterCombat client =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        int combo = client.getComboCount();
        float scale = SwingScales.upcomingTimingScaleFor(player, combo);
        SwingHaste.setSequenceScale(player, scale);
    }

    @Inject(method = "startUpswing", at = @At("RETURN"), remap = false, require = 1)
    private void combat_styles$upswingDone(WeaponAttributes attributes, CallbackInfo callback) {
        if (!CombatStylesConfig.get().debugLogging()) {
            return;
        }
        MinecraftClient_BetterCombat client =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        Player player = Minecraft.getInstance().player;
        CombatStyles.LOGGER.info(
                "[UPSWING-END] combo={} upswingTicks={} swingDur={} swingUpsw={}"
                        + " comboReset={} clamped={}",
                client.getComboCount(), client.getUpswingTicks(),
                ongoingSwing == null ? "none" : ongoingSwing.duration(),
                ongoingSwing == null ? "none" : ongoingSwing.upswingTicks(),
                comboReset, player == null ? "?" : SwingHaste.clamped(player));
    }

    @Inject(method = "startUpswing", at = @At("HEAD"), remap = false, require = 1,
            cancellable = true)
    private void combat_styles$blockEarlySwing(WeaponAttributes attributes,
                                               CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        MinecraftClient_BetterCombat client =
                (MinecraftClient_BetterCombat) Minecraft.getInstance();
        if (client.getUpswingTicks() <= 0 && AttackInput.claimUpswing()) {
            SwingHaste.endSwing(player, SwingHaste.swingIndex(player));
            SwingHaste.beginSwing(player);
            PassDriver.onSwingStart(player);
            int raw = client.getComboCount();
            CombatEffectDealer.applyLocalAttackTiming(player, raw);
            SwingDash.onUpswing(player, raw);
            combat_styles$lastStart = -1;
            combat_styles$lastNeeded = 0;
        }
        float haste = SwingHaste.clamped(player);
        if (haste < SwingHaste.NEUTRAL_MULTIPLIER
                && client.getUpswingTicks() <= 0
                && !player.isUsingItem()) {
            if (rightClickDelay > 0) {
                rightClickDelay = 0;
            }
            if (missTime > 0) {
                int allowed = Math.max(0,
                        Math.round(lastSwingDuration * haste) - lastAttacked);
                if (missTime > allowed) {
                    if (CombatStylesConfig.get().debugLogging()) {
                        CombatStyles.LOGGER.info(
                                "[MISS] tick={} combo={} haste={} miss={} allowed={}"
                                        + " lastDur={} lastAtk={}",
                                player.tickCount, client.getComboCount(), haste,
                                missTime, allowed, lastSwingDuration, lastAttacked);
                    }
                    missTime = allowed;
                }
            }
        }
        if (combat_styles$lastStart < 0
                || haste != SwingHaste.NEUTRAL_MULTIPLIER) {
            return;
        }
        int elapsed = player.tickCount - combat_styles$lastStart;
        if (elapsed > 0 && elapsed < combat_styles$lastNeeded) {
            if (CombatStylesConfig.get().debugLogging()) {
                CombatStyles.LOGGER.info("[GATE] blocked: elapsed={} needed={} combo={}",
                        elapsed, combat_styles$lastNeeded, client.getComboCount());
            }
            callback.cancel();
            return;
        }
        combat_styles$lastStart = -1;
        combat_styles$lastNeeded = 0;
    }
}
