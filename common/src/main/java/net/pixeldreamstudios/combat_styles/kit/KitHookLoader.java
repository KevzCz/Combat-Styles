package net.pixeldreamstudios.combat_styles.kit;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.util.LinkedHashMap;
import java.util.Map;

public class KitHookLoader extends SimpleJsonResourceReloadListener {

    private static volatile Map<String, KitHook> hooks = Map.of();

    public KitHookLoader() {
        super(CombatStyles.GSON, ConfigPaths.STARTER_KIT_HOOKS);
    }

    public static Map<String, KitHook> hooks() {
        return hooks;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, KitHook> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            if (!file.getValue().isJsonObject()) {
                CombatStyles.LOGGER.warn("Skipping starter kit hook {}: not a JSON object", file.getKey());
                continue;
            }
            KitHook.parse(file.getValue().getAsJsonObject()).ifPresentOrElse(hook -> loaded.put(hook.kit(), hook),
                    () -> CombatStyles.LOGGER.warn("Skipping starter kit hook {}: missing kit name", file.getKey()));
        }
        hooks = Map.copyOf(loaded);
        CombatStyles.LOGGER.info("Loaded {} starter kit hooks", loaded.size());
    }
}
