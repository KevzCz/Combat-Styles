package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record PoseTuning(BoneOffset rightItem, BoneOffset leftItem,
                         BoneOffset rightArm, BoneOffset leftArm,
                         BoneOffset torso, BoneOffset head) {

    public static final PoseTuning NONE = new PoseTuning(
            BoneOffset.NONE, BoneOffset.NONE, BoneOffset.NONE,
            BoneOffset.NONE, BoneOffset.NONE, BoneOffset.NONE);

    public static final String BONE_RIGHT_ITEM = "right_item";
    public static final String BONE_LEFT_ITEM = "left_item";
    public static final String BONE_RIGHT_ARM = "right_arm";
    public static final String BONE_LEFT_ARM = "left_arm";
    public static final String BONE_TORSO = "torso";
    public static final String BONE_HEAD = "head";

    public static final List<String> BONES = List.of(
            BONE_RIGHT_ITEM, BONE_LEFT_ITEM, BONE_RIGHT_ARM,
            BONE_LEFT_ARM, BONE_TORSO, BONE_HEAD);

    public static final Codec<PoseTuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoneOffset.CODEC.optionalFieldOf(BONE_RIGHT_ITEM, BoneOffset.NONE)
                    .forGetter(PoseTuning::rightItem),
            BoneOffset.CODEC.optionalFieldOf(BONE_LEFT_ITEM, BoneOffset.NONE)
                    .forGetter(PoseTuning::leftItem),
            BoneOffset.CODEC.optionalFieldOf(BONE_RIGHT_ARM, BoneOffset.NONE)
                    .forGetter(PoseTuning::rightArm),
            BoneOffset.CODEC.optionalFieldOf(BONE_LEFT_ARM, BoneOffset.NONE)
                    .forGetter(PoseTuning::leftArm),
            BoneOffset.CODEC.optionalFieldOf(BONE_TORSO, BoneOffset.NONE)
                    .forGetter(PoseTuning::torso),
            BoneOffset.CODEC.optionalFieldOf(BONE_HEAD, BoneOffset.NONE)
                    .forGetter(PoseTuning::head)
    ).apply(instance, PoseTuning::new));

    public PoseTuning {
        rightItem = rightItem == null ? BoneOffset.NONE : rightItem;
        leftItem = leftItem == null ? BoneOffset.NONE : leftItem;
        rightArm = rightArm == null ? BoneOffset.NONE : rightArm;
        leftArm = leftArm == null ? BoneOffset.NONE : leftArm;
        torso = torso == null ? BoneOffset.NONE : torso;
        head = head == null ? BoneOffset.NONE : head;
    }

    public boolean isEmpty() {
        return rightItem.isEmpty() && leftItem.isEmpty() && rightArm.isEmpty()
                && leftArm.isEmpty() && torso.isEmpty() && head.isEmpty();
    }

    public BoneOffset boneOf(String bone) {
        return switch (bone) {
            case BONE_RIGHT_ITEM -> rightItem;
            case BONE_LEFT_ITEM -> leftItem;
            case BONE_RIGHT_ARM -> rightArm;
            case BONE_LEFT_ARM -> leftArm;
            case BONE_TORSO -> torso;
            case BONE_HEAD -> head;
            default -> BoneOffset.NONE;
        };
    }

    public PoseTuning withBone(String bone, BoneOffset offset) {
        return switch (bone) {
            case BONE_RIGHT_ITEM -> new PoseTuning(offset, leftItem, rightArm, leftArm, torso, head);
            case BONE_LEFT_ITEM -> new PoseTuning(rightItem, offset, rightArm, leftArm, torso, head);
            case BONE_RIGHT_ARM -> new PoseTuning(rightItem, leftItem, offset, leftArm, torso, head);
            case BONE_LEFT_ARM -> new PoseTuning(rightItem, leftItem, rightArm, offset, torso, head);
            case BONE_TORSO -> new PoseTuning(rightItem, leftItem, rightArm, leftArm, offset, head);
            case BONE_HEAD -> new PoseTuning(rightItem, leftItem, rightArm, leftArm, torso, offset);
            default -> this;
        };
    }

    public String signature() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("p");
        for (String bone : BONES) {
            builder.append(boneOf(bone).signature());
        }
        return builder.toString();
    }

    public record BoneOffset(float pitch, float yaw, float roll,
                             float x, float y, float z) {

        public static final float ANGLE_LIMIT = 180.0F;
        public static final float OFFSET_LIMIT = 300.0F;

        public static final BoneOffset NONE = new BoneOffset(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        public static final Codec<BoneOffset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(BoneOffset::pitch),
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(BoneOffset::yaw),
                Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(BoneOffset::roll),
                Codec.FLOAT.optionalFieldOf("x", 0.0F).forGetter(BoneOffset::x),
                Codec.FLOAT.optionalFieldOf("y", 0.0F).forGetter(BoneOffset::y),
                Codec.FLOAT.optionalFieldOf("z", 0.0F).forGetter(BoneOffset::z)
        ).apply(instance, BoneOffset::new));

        public boolean isEmpty() {
            return pitch == 0.0F && yaw == 0.0F && roll == 0.0F
                    && x == 0.0F && y == 0.0F && z == 0.0F;
        }

        public BoneOffset clamped() {
            return new BoneOffset(
                    Math.clamp(pitch, -ANGLE_LIMIT, ANGLE_LIMIT),
                    Math.clamp(yaw, -ANGLE_LIMIT, ANGLE_LIMIT),
                    Math.clamp(roll, -ANGLE_LIMIT, ANGLE_LIMIT),
                    Math.clamp(x, -OFFSET_LIMIT, OFFSET_LIMIT),
                    Math.clamp(y, -OFFSET_LIMIT, OFFSET_LIMIT),
                    Math.clamp(z, -OFFSET_LIMIT, OFFSET_LIMIT));
        }

        public String signature() {
            return isEmpty() ? "_" : "b" + pitch + "_" + yaw + "_" + roll
                    + "_" + x + "_" + y + "_" + z;
        }
    }
}
