package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.ChoiceSource;
import net.pixeldreamstudios.combat_styles.style.ClientHolder;

import java.util.List;

public final class EnchantmentChoices implements ChoiceSource {

    public static final String SELECTOR = "enchantment_id";

    @Override
    public String selector() {
        return SELECTOR;
    }

    @Override
    public List<String> values() {
        Player player = ClientHolder.localPlayer();
        if (player == null) {
            return List.of();
        }
        return player.level().registryAccess().lookup(Registries.ENCHANTMENT)
                .map(registry -> registry.listElementIds()
                        .map(key -> key.location())
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList())
                .orElse(List.of());
    }
}
