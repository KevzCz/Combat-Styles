package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.client.AffixPlateComponent;
import net.pixeldreamstudios.combat_styles.client.tooltip.AffixPlates;
import net.pixeldreamstudios.combat_styles.equipment.AffixPlateData;

import java.util.List;

@Mixin(ClientTooltipComponent.class)
public interface StyleTooltipComponentMixin {

    @Inject(method = "create(Lnet/minecraft/util/FormattedCharSequence;)"
            + "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private static void combat_styles$plateComponent(FormattedCharSequence text,
                                                     CallbackInfoReturnable<ClientTooltipComponent>
                                                             callback) {
        AffixPlateData.Plate plate = AffixPlates.of(text);
        if (plate != null) {
            callback.setReturnValue(new AffixPlateComponent(
                    new AffixPlateData(null, List.of(plate), List.of()), text));
        }
    }
}
