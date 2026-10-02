package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.attribute.ModAttributes;

@Mixin(Player.class)
public class PlayerAttributesMixin {

    @Inject(method = "createAttributes", at = @At("RETURN"), require = 1)
    private static void combat_styles$addAttributes(
            CallbackInfoReturnable<AttributeSupplier.Builder> callback) {
        ModAttributes.appendTo(callback.getReturnValue());
    }
}
