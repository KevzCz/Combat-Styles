package net.pixeldreamstudios.combat_styles.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpellParentLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "spell";
    private static final String KEY_BLOCK = CombatStyles.MOD_ID;
    private static final String KEY_PARENT = "parent";

    private static volatile Map<String, String> parents = Map.of();

    public SpellParentLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    public static Map<String, String> parents() {
        return parents;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, String> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            if (!file.getValue().isJsonObject()) {
                continue;
            }
            JsonObject spell = file.getValue().getAsJsonObject();
            if (!spell.has(KEY_BLOCK) || !spell.get(KEY_BLOCK).isJsonObject()) {
                continue;
            }
            JsonObject block = spell.getAsJsonObject(KEY_BLOCK);
            if (block.has(KEY_PARENT) && block.get(KEY_PARENT).isJsonPrimitive()) {
                String parent = block.get(KEY_PARENT).getAsString();
                if (ResourceLocation.tryParse(parent) != null) {
                    loaded.put(file.getKey().toString(), parent);
                }
            }
        }
        parents = Map.copyOf(loaded);
        CombatStyles.LOGGER.info("Loaded {} spell parents", loaded.size());
    }
}
