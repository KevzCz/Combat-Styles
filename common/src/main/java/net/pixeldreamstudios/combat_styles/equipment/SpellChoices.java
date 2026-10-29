package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.config.ChoiceSource;
import net.pixeldreamstudios.combat_styles.style.ClientHolder;

import java.util.List;

public final class SpellChoices implements ChoiceSource {

    public static final String SELECTOR = "spell_id";

    @Override
    public String selector() {
        return SELECTOR;
    }

    @Override
    public List<String> values() {
        Player player = ClientHolder.localPlayer();
        if (player == null || !SpellEngineCompat.isLoaded()) {
            return List.of();
        }
        return SpellEngineCompat.spellIds(player.level()).stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    @Override
    public JsonElement template(String value, String scope, String key) {
        return new JsonPrimitive("");
    }
}
