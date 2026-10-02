package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

@Pseudo
@Mixin(targets = "net.spell_engine.internals.container.SpellContainerSource", remap = false)
public abstract class SpellContainerUpdateMixin {

    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private static void combat_styles$applySpellLocks(Player player, CallbackInfo callback) {
        SpellEngineCompat.afterContainerUpdate(player);
    }
}
