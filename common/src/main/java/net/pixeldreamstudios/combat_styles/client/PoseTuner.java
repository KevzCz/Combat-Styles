package net.pixeldreamstudios.combat_styles.client;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import net.minecraft.util.Mth;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PoseTuner {

    private static final float PIXELS_PER_BLOCK = 16.0F;
    private static final int CACHE_LIMIT = 64;
    private static final Map<String, KeyframeAnimation> CACHE = new HashMap<>();

    private PoseTuner() {
    }

    public static KeyframeAnimation tuned(KeyframeAnimation animation, PoseTuning tuning) {
        if (animation == null || tuning == null || tuning.isEmpty()) {
            return animation;
        }
        String key = animation.getUuid() + tuning.signature();
        KeyframeAnimation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
            applyTo(copy, tuning);
            copy.uuid = UUID.nameUUIDFromBytes(
                    (String.valueOf(animation.getUuid()) + tuning.signature())
                            .getBytes(StandardCharsets.UTF_8));
            KeyframeAnimation built = copy.build();
            if (CACHE.size() >= CACHE_LIMIT) {
                CACHE.clear();
            }
            CACHE.put(key, built);
            return built;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.warn("Pose tuning failed", throwable);
            return animation;
        }
    }

    public static void applyTo(KeyframeAnimation.AnimationBuilder copy, PoseTuning tuning) {
        if (copy == null || tuning == null || tuning.isEmpty()) {
            return;
        }
        bend(copy.rightItem, tuning.rightItem());
        bend(copy.leftItem, tuning.leftItem());
        bend(copy.rightArm, tuning.rightArm());
        bend(copy.leftArm, tuning.leftArm());
        bend(copy.torso, tuning.torso());
        bend(copy.head, tuning.head());
    }

    private static void bend(KeyframeAnimation.StateCollection bone,
                             PoseTuning.BoneOffset offset) {
        if (bone == null || offset == null || offset.isEmpty()) {
            return;
        }
        PoseTuning.BoneOffset clamped = offset.clamped();
        shift(bone.pitch, clamped.pitch() * Mth.DEG_TO_RAD);
        shift(bone.yaw, clamped.yaw() * Mth.DEG_TO_RAD);
        shift(bone.roll, clamped.roll() * Mth.DEG_TO_RAD);
        shift(bone.x, clamped.x() / PIXELS_PER_BLOCK);
        shift(bone.y, clamped.y() / PIXELS_PER_BLOCK);
        shift(bone.z, clamped.z() / PIXELS_PER_BLOCK);
    }

    private static void shift(KeyframeAnimation.StateCollection.State axis, float amount) {
        if (axis == null || amount == 0.0F) {
            return;
        }
        if (axis.getKeyFrames().isEmpty()) {
            axis.addKeyFrame(0, axis.defaultValue + amount, Ease.INOUTSINE);
            axis.setEnabled(true);
            return;
        }
        for (int index = 0; index < axis.getKeyFrames().size(); index++) {
            KeyframeAnimation.KeyFrame frame = axis.getKeyFrames().get(index);
            axis.replace(new KeyframeAnimation.KeyFrame(frame.tick, frame.value + amount,
                    frame.ease == null ? Ease.INOUTSINE : frame.ease), index);
        }
        axis.setEnabled(true);
    }
}
