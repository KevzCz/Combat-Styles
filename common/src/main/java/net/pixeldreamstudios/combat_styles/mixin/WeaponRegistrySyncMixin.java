package net.pixeldreamstudios.combat_styles.mixin;

import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.network.Packets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.style.DefaultStyleLoader;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

@Mixin(WeaponRegistry.class)
public class WeaponRegistrySyncMixin {

    @Inject(method = "decodeRegistry", at = @At("RETURN"), remap = false, require = 1)
    private static void combat_styles$reindex(Packets.WeaponRegistrySync sync,
                                              CallbackInfo callback) {
        StyleRegistry.rebuild();
        DefaultStyleLoader.reapply();
    }
}
