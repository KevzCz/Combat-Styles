package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.equipment.StackAttributes;

import java.util.function.BiConsumer;

@Mixin(ItemStack.class)
public abstract class ItemStackAttributeMixin {

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;"
            + "Ljava/util/function/BiConsumer;)V", at = @At("RETURN"), require = 0)
    private void combat_styles$equipmentBonusesForGroup(
            EquipmentSlotGroup slotGroup,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo callback) {
        StackAttributes.forEachBonus((ItemStack) (Object) this, slotGroup, consumer);
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;"
            + "Ljava/util/function/BiConsumer;)V", at = @At("RETURN"), require = 0)
    private void combat_styles$equipmentBonusesForSlot(
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo callback) {
        StackAttributes.forEachBonus((ItemStack) (Object) this, slot, consumer);
    }
}
