package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record TreeRequirement(ResourceLocation category, Optional<ResourceLocation> advancement, List<ResourceLocation> linked,
                              Optional<String> carryExperience, List<String> removeTags) {

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_REQUIRES = "requires";
    private static final String KEY_ADVANCEMENT = "advancement";
    private static final String KEY_LINKED = "linked";
    private static final String KEY_CARRY_EXPERIENCE = "carry_experience";
    private static final String KEY_REMOVE_TAGS = "remove_tags";

    public TreeRequirement {
        linked = List.copyOf(linked);
        removeTags = List.copyOf(removeTags);
    }

    public static Optional<TreeRequirement> parse(JsonObject json) {
        if (!json.has(KEY_CATEGORY)) {
            return Optional.empty();
        }
        ResourceLocation category = ResourceLocation.tryParse(json.get(KEY_CATEGORY).getAsString());
        if (category == null) {
            return Optional.empty();
        }
        Optional<ResourceLocation> advancement = Optional.empty();
        if (json.has(KEY_REQUIRES) && json.get(KEY_REQUIRES).isJsonObject()) {
            JsonObject requires = json.getAsJsonObject(KEY_REQUIRES);
            if (requires.has(KEY_ADVANCEMENT)) {
                advancement = Optional.ofNullable(ResourceLocation.tryParse(requires.get(KEY_ADVANCEMENT).getAsString()));
            }
        }
        List<ResourceLocation> linked = new ArrayList<>();
        for (String id : strings(json, KEY_LINKED)) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null) {
                linked.add(parsed);
            }
        }
        Optional<String> carry = json.has(KEY_CARRY_EXPERIENCE)
                ? Optional.of(json.get(KEY_CARRY_EXPERIENCE).getAsString())
                : Optional.empty();
        return Optional.of(new TreeRequirement(category, advancement, linked, carry, strings(json, KEY_REMOVE_TAGS)));
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    values.add(element.getAsString());
                }
            }
        }
        return values;
    }
}
