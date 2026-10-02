package net.pixeldreamstudios.combat_styles.mixin;

import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(WeaponRegistry.class)
public interface WeaponRegistryAccessor {

    @Accessor(value = "registrations", remap = false)
    static Map<ResourceLocation, WeaponAttributes> combat_styles$registrations() {
        throw new AssertionError();
    }

    @Accessor(value = "containers", remap = false)
    static Map<ResourceLocation, AttributesContainer> combat_styles$containers() {
        throw new AssertionError();
    }
}
