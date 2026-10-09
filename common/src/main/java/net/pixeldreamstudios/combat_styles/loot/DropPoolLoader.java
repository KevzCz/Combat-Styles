package net.pixeldreamstudios.combat_styles.loot;

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

public class DropPoolLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = ConfigPaths.EQUIPMENT_DROPS;

    public DropPoolLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, DropPool> loaded = new LinkedHashMap<>();
        int rejected = 0;
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            DropPool pool = DropPool.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid drop pool {}: {}", file.getKey(), error))
                    .orElse(null);
            if (pool == null) {
                rejected++;
                continue;
            }
            loaded.put(file.getKey().toString(), pool);
        }
        DropPools.DEFINITIONS.acceptData(loaded);
        CombatStyles.LOGGER.info("Loaded {} drop pools ({} rejected)", loaded.size(), rejected);
    }
}
