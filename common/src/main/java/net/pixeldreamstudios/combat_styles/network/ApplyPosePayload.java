package net.pixeldreamstudios.combat_styles.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.StackPose;

public record ApplyPosePayload(EquipmentSlot slot, StackPose pose,
                               boolean equipmentLayer, boolean remove)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ApplyPosePayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("apply_pose"));

    private static final StreamCodec<RegistryFriendlyByteBuf, StackPose> POSE_CODEC =
            ByteBufCodecs.STRING_UTF8.<StackPose>map(
                    raw -> StackPose.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(raw))
                            .result().orElse(StackPose.NONE),
                    pose -> StackPose.CODEC.encodeStart(JsonOps.INSTANCE, pose)
                            .result().map(JsonElement::toString).orElse("{}")).cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyPosePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(
                            ordinal -> EquipmentSlot.values()[ordinal],
                            EquipmentSlot::ordinal),
                    ApplyPosePayload::slot,
                    POSE_CODEC,
                    ApplyPosePayload::pose,
                    ByteBufCodecs.BOOL.cast(),
                    ApplyPosePayload::equipmentLayer,
                    ByteBufCodecs.BOOL.cast(),
                    ApplyPosePayload::remove,
                    ApplyPosePayload::new);

    @Override
    public Type<ApplyPosePayload> type() {
        return TYPE;
    }
}
