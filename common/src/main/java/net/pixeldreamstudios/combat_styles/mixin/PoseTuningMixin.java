package net.pixeldreamstudios.combat_styles.mixin;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.bettercombat.client.animation.PoseSubStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.client.PoseOwner;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.client.PoseTuner;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentPoses;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;

@Mixin(value = PoseSubStack.class, remap = false)
public class PoseTuningMixin {

    @Shadow(remap = false)
    @Final
    private boolean isMainHand;

    @Shadow(remap = false)
    @Final
    private boolean isBodyChannel;

    @ModifyVariable(method = "setPose", at = @At("HEAD"), argsOnly = true, require = 1)
    private KeyframeAnimation combat_styles$tunePose(KeyframeAnimation pose) {
        if (pose == null || isBodyChannel) {
            return pose;
        }
        Player player = PoseOwner.current();
        boolean debug = CombatStylesConfig.get().debugLogging();
        if (player == null) {
            if (debug) {
                CombatStyles.LOGGER.info("[POSE] no owner (mainHand={})", isMainHand);
            }
            return pose;
        }
        ItemStack held = isMainHand ? player.getMainHandItem() : player.getOffhandItem();
        PoseTuning tuning = EquipmentPoses.tuningFor(held);
        if (debug) {
            CombatStyles.LOGGER.info("[POSE] mainHand={} held={} tuning={} empty={}",
                    isMainHand, held.getItem(), tuning.signature(), tuning.isEmpty());
        }
        return tuning.isEmpty() ? pose : PoseTuner.tuned(pose, tuning);
    }
}
