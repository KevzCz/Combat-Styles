package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomAttackLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "custom_attacks";

    public CustomAttackLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, CustomAttack> loaded = new LinkedHashMap<>();
        int rejected = 0;

        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            CustomAttack attack = CustomAttack.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error -> CombatStyles.LOGGER.error(
                            "Invalid custom attack {}: {}", file.getKey(), error))
                    .orElse(null);
            if (attack == null) {
                rejected++;
                continue;
            }
            if (attack.id().isBlank()) {
                CombatStyles.LOGGER.error("Custom attack {} has a blank id", file.getKey());
                rejected++;
                continue;
            }
            if (attack.attack().animation().isBlank()) {
                CombatStyles.LOGGER.error("Custom attack {} has no animation", attack.id());
                rejected++;
                continue;
            }
            CustomAttack previous = loaded.put(attack.id(), attack);
            if (previous != null) {
                CombatStyles.LOGGER.warn("Custom attack {} from {} overrides an earlier definition",
                        attack.id(), file.getKey());
            }
        }

        CustomAttacks.accept(loaded);
        CombatStyles.LOGGER.info("Loaded {} custom attacks ({} rejected)",
                loaded.size(), rejected);
    }
}
