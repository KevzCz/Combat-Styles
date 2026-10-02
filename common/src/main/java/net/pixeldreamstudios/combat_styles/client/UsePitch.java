package net.pixeldreamstudios.combat_styles.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class UsePitch {

    private static final String BODY = "body";
    private static final String RIGHT_ARM = "rightArm";
    private static final String LEFT_ARM = "leftArm";
    private static final String RIGHT_LEG = "rightLeg";
    private static final String LEFT_LEG = "leftLeg";

    private static final float THIRD_PERSON_SHARE = 2.0F;
    private static final float COUNTER = -1.0F;

    private UsePitch() {
    }

    public static AdjustmentModifier modifierFor(Player player) {
        return new AdjustmentModifier(bone -> partFor(player, bone));
    }

    private static Optional<AdjustmentModifier.PartModifier> partFor(Player player,
                                                                    String bone) {
        if (player == null) {
            return Optional.empty();
        }
        return FirstPersonMode.isFirstPersonPass()
                ? firstPerson(player, bone)
                : thirdPerson(player, bone);
    }

    private static Optional<AdjustmentModifier.PartModifier> firstPerson(Player player,
                                                                         String bone) {
        if (!RIGHT_ARM.equals(bone) && !LEFT_ARM.equals(bone)) {
            return Optional.empty();
        }
        return rotation(radians(player.getXRot()));
    }

    private static Optional<AdjustmentModifier.PartModifier> thirdPerson(Player player,
                                                                         String bone) {
        float pitch = radians(player.getXRot() / THIRD_PERSON_SHARE);
        return switch (bone) {
            case BODY, RIGHT_LEG, LEFT_LEG -> rotation(pitch * COUNTER);
            case RIGHT_ARM, LEFT_ARM -> rotation(pitch);
            default -> Optional.empty();
        };
    }

    private static Optional<AdjustmentModifier.PartModifier> rotation(float pitch) {
        return Optional.of(new AdjustmentModifier.PartModifier(
                new Vec3f(pitch, 0.0F, 0.0F), Vec3f.ZERO));
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
