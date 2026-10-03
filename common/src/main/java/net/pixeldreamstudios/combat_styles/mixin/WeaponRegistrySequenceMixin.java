package net.pixeldreamstudios.combat_styles.mixin;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentPoses;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.NativeAugments;
import net.pixeldreamstudios.combat_styles.style.SequenceComposer;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.SyntheticAttributes;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.minecraft.world.item.ItemStack;

@Mixin(WeaponRegistry.class)
public class WeaponRegistrySequenceMixin {

    @Inject(method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private static void combat_styles$applySequence(ItemStack stack,
                                                    CallbackInfoReturnable<WeaponAttributes> callback) {
        if (stack == null || stack.isEmpty() || WeaponRegistryAccess.bypassSequence()) {
            return;
        }
        WeaponAttributes base = callback.getReturnValue();
        if (base == null) {
            base = SyntheticAttributes.forStack(stack, null);
        } else {
            base = WeaponCategories.applyTo(stack, base);
        }
        if (base == null) {
            return;
        }
        WeaponRegistryAccess.pushBypass();
        try {
            combat_styles$compose(stack, base, callback);
        } finally {
            WeaponRegistryAccess.popBypass();
        }
    }

    private static void combat_styles$compose(ItemStack stack, WeaponAttributes base,
                                              CallbackInfoReturnable<WeaponAttributes> callback) {
        StyleSequence sequence = SequenceService.sequenceOf(stack);
        if (sequence.isEmpty()) {
            sequence = SequenceService.substituteSequenceOf(stack);
        }
        if (sequence.isEmpty()) {
            WeaponAttributes augmented = NativeAugments.apply(base, stack);
            WeaponAttributes posed = EquipmentPoses.applyTo(
                    augmented == null ? base : augmented, stack);
            if (posed != null && posed != callback.getReturnValue()) {
                callback.setReturnValue(posed);
            }
            return;
        }
        WeaponAttributes composed = SequenceComposer.compose(base, sequence,
                SequenceService.attackOverridesOf(stack),
                DefaultStyles.nativeStyleIdsOf(stack));
        WeaponAttributes posed = EquipmentPoses.applyTo(
                composed == null ? base : composed, stack);
        if (posed != null && posed != callback.getReturnValue()) {
            callback.setReturnValue(posed);
        }
    }
}
