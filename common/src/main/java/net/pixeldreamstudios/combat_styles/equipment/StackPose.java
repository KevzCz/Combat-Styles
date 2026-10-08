package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.Optional;

public record StackPose(Optional<String> pose, Optional<String> offHandPose,
                        PoseTuning tuning) {

    public static final StackPose NONE =
            new StackPose(Optional.empty(), Optional.empty(), PoseTuning.NONE);

    public static final Codec<StackPose> CODEC = Customs.wrap(StackPose.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("pose").forGetter(StackPose::pose),
            Codec.STRING.optionalFieldOf("off_hand_pose").forGetter(StackPose::offHandPose),
            PoseTuning.CODEC.optionalFieldOf("tuning", PoseTuning.NONE)
                    .forGetter(StackPose::tuning)
    ).apply(instance, StackPose::new)));

    public StackPose {
        tuning = tuning == null ? PoseTuning.NONE : tuning;
    }

    public boolean isEmpty() {
        return pose.isEmpty() && offHandPose.isEmpty() && tuning.isEmpty();
    }
}
