package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomStyleLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "custom_styles";

    public CustomStyleLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, CustomStyle> loaded = new LinkedHashMap<>();
        int rejected = 0;
        int unresolved = 0;

        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            CustomStyle style = CustomStyle.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid custom style {}: {}",
                                    file.getKey(), error))
                    .orElse(null);
            if (style == null) {
                rejected++;
                continue;
            }
            if (style.id().isBlank()) {
                CombatStyles.LOGGER.error("Custom style {} has a blank id", file.getKey());
                rejected++;
                continue;
            }
            if (StyleRegistry.get(style.entry().styleId()) == null) {
                CombatStyles.LOGGER.warn("Custom style {} references unknown style {} (registry has {} pages)",
                        style.id(), style.entry().styleId(), StyleRegistry.all().size());
                unresolved++;
            }

            CustomStyle previous = loaded.put(style.id(), style);
            if (previous != null) {
                CombatStyles.LOGGER.warn("Custom style id {} from {} overrides an earlier definition",
                        style.id(), file.getKey());
            }
        }

        CustomStyles.accept(loaded);
        CombatStyles.LOGGER.info(
                "Loaded {} custom styles in {} groups ({} rejected, {} unresolved)",
                loaded.size(), CustomStyles.groups().size(), rejected, unresolved);
    }
}
