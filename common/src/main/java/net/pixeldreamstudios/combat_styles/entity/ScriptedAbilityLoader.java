package net.pixeldreamstudios.combat_styles.entity;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScriptedAbilityLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = ConfigPaths.SUMMON_ABILITIES;

    public ScriptedAbilityLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, ScriptedAbility> loaded = new LinkedHashMap<>();
        int rejected = 0;
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            ScriptedAbility ability = ScriptedAbility.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error -> CombatStyles.LOGGER.error(
                            "Invalid summon ability {}: {}", file.getKey(), error))
                    .orElse(null);
            if (ability == null) {
                rejected++;
                continue;
            }
            if (ability.steps().isEmpty()) {
                CombatStyles.LOGGER.warn("Summon ability {} has no steps", file.getKey());
            }
            loaded.put(file.getKey().toString(), ability);
        }
        ScriptedAbilities.DEFINITIONS.acceptData(loaded);
        if (rejected > 0) {
            CombatStyles.LOGGER.warn("Rejected {} invalid summon ability definitions", rejected);
        }
        CombatStyles.LOGGER.info("Loaded {} summon ability definitions", loaded.size());
    }
}
