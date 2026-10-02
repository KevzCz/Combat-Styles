package net.pixeldreamstudios.combat_styles.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class EffectLookup {

    private EffectLookup() {
    }

    public static Holder<MobEffect> of(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation location = id.indexOf(':') < 0
                ? CombatStyles.id(id)
                : ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceKey.create(Registries.MOB_EFFECT, location))
                .orElse(null);
    }
}
