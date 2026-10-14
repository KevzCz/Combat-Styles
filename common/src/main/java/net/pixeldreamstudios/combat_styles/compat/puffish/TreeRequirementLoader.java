package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TreeRequirementLoader extends SimpleJsonResourceReloadListener {

    private static volatile List<TreeRequirement> requirements = List.of();

    public TreeRequirementLoader() {
        super(CombatStyles.GSON, ConfigPaths.TREE_REQUIREMENTS);
    }

    public static List<TreeRequirement> requirements() {
        return requirements;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<TreeRequirement> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            if (!file.getValue().isJsonObject()) {
                CombatStyles.LOGGER.warn("Skipping tree requirement {}: not a JSON object", file.getKey());
                continue;
            }
            TreeRequirement.parse(file.getValue().getAsJsonObject()).ifPresentOrElse(loaded::add,
                    () -> CombatStyles.LOGGER.warn("Skipping tree requirement {}: missing or invalid category", file.getKey()));
        }
        requirements = List.copyOf(loaded);
        CombatStyles.LOGGER.info("Loaded {} tree requirements", loaded.size());
    }
}
