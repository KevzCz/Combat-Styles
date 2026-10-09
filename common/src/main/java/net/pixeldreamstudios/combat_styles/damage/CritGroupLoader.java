package net.pixeldreamstudios.combat_styles.damage;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;

public class CritGroupLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "crit_groups";

    public CritGroupLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, ScaledNumber.Crit> loaded = new LinkedHashMap<>();
        int rejected = 0;
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            ScaledNumber.Crit crit = ScaledNumber.Crit.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid crit group {}: {}", file.getKey(), error))
                    .orElse(null);
            if (crit == null) {
                rejected++;
                continue;
            }
            loaded.put(file.getKey().toString(), crit);
        }
        CritGroups.DEFINITIONS.acceptData(loaded);
        CombatStyles.LOGGER.info("Loaded {} crit groups ({} rejected)", loaded.size(), rejected);
    }
}
