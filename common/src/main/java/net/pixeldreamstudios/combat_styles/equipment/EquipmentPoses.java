package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;

public final class EquipmentPoses {

    private EquipmentPoses() {
    }

    public static StackPose stackPoseOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StackPose.NONE;
        }
        StackPose stored = stack.get(ModComponents.STACK_POSE.get());
        return stored == null ? StackPose.NONE : stored;
    }

    public static StackPose equipmentPoseOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StackPose.NONE;
        }
        StackPose stored = stack.get(ModComponents.EQUIPMENT_POSE.get());
        if (stored != null) {
            return stored;
        }
        if (EquipmentRegistry.isEmpty()) {
            return StackPose.NONE;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null || !definition.definesPose()) {
            return StackPose.NONE;
        }
        return new StackPose(definition.pose(), definition.offHandPose(),
                definition.poseTuning());
    }

    public static void storeEquipment(ItemStack stack, StackPose pose) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (pose == null) {
            stack.remove(ModComponents.EQUIPMENT_POSE.get());
            return;
        }
        stack.set(ModComponents.EQUIPMENT_POSE.get(), pose);
    }

    public static StackPose poseOf(ItemStack stack) {
        StackPose stackPose = stackPoseOf(stack);
        StackPose equipmentPose = equipmentPoseOf(stack);
        if (equipmentPose.isEmpty()) {
            return stackPose;
        }
        if (stackPose.isEmpty()) {
            return equipmentPose;
        }
        return new StackPose(
                stackPose.pose().or(equipmentPose::pose),
                stackPose.offHandPose().or(equipmentPose::offHandPose),
                stackPose.tuning().isEmpty() ? equipmentPose.tuning() : stackPose.tuning());
    }

    public static void store(ItemStack stack, StackPose pose) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (pose == null) {
            stack.remove(ModComponents.STACK_POSE.get());
            return;
        }
        stack.set(ModComponents.STACK_POSE.get(), pose);
    }

    public static PoseTuning tuningFor(ItemStack stack) {
        return poseOf(stack).tuning();
    }

    public static WeaponAttributes applyTo(WeaponAttributes attributes, ItemStack stack) {
        if (attributes == null) {
            return attributes;
        }
        StackPose resolved = poseOf(stack);
        if (resolved.isEmpty()) {
            return attributes;
        }
        String pose = resolved.pose().orElse(attributes.pose());
        String offHandPose = resolved.offHandPose().orElse(attributes.offHandPose());
        if (equalPose(pose, attributes.pose()) && equalPose(offHandPose, attributes.offHandPose())) {
            return attributes;
        }
        return new WeaponAttributes(
                attributes.attackRange(),
                attributes.rangeBonus(),
                pose,
                offHandPose,
                attributes.two_handed(),
                attributes.category(),
                attributes.attacks(),
                attributes.trailAppearance());
    }

    private static boolean equalPose(String one, String other) {
        return one == null ? other == null : one.equals(other);
    }
}
