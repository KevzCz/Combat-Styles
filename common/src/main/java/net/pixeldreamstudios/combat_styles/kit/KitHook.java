package net.pixeldreamstudios.combat_styles.kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record KitHook(String kit, List<KitAction> onGiven, List<KitAction> onReset, boolean rechooseGivesItems,
                      Map<String, String> legacyCustomData) {

    private static final String KEY_KIT = "kit";
    private static final String KEY_ON_GIVEN = "on_given";
    private static final String KEY_ON_RESET = "on_reset";
    private static final String KEY_RECHOOSE_GIVES_ITEMS = "rechoose_gives_items";
    private static final String KEY_LEGACY_CUSTOM_DATA = "legacy_custom_data";

    public KitHook {
        onGiven = List.copyOf(onGiven);
        onReset = List.copyOf(onReset);
        legacyCustomData = Map.copyOf(legacyCustomData);
    }

    public static Optional<KitHook> parse(JsonObject json) {
        if (!json.has(KEY_KIT) || !json.get(KEY_KIT).isJsonPrimitive()) {
            return Optional.empty();
        }
        String kit = json.get(KEY_KIT).getAsString();
        if (kit.isBlank()) {
            return Optional.empty();
        }
        boolean givesItems = json.has(KEY_RECHOOSE_GIVES_ITEMS) && json.get(KEY_RECHOOSE_GIVES_ITEMS).getAsBoolean();
        return Optional.of(new KitHook(kit, actions(json, KEY_ON_GIVEN), actions(json, KEY_ON_RESET), givesItems,
                legacyCustomData(json)));
    }

    private static Map<String, String> legacyCustomData(JsonObject json) {
        Map<String, String> entries = new LinkedHashMap<>();
        if (json.has(KEY_LEGACY_CUSTOM_DATA) && json.get(KEY_LEGACY_CUSTOM_DATA).isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(KEY_LEGACY_CUSTOM_DATA).entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    entries.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        return entries;
    }

    private static List<KitAction> actions(JsonObject json, String key) {
        List<KitAction> actions = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    KitAction.parse(element.getAsJsonObject()).ifPresent(actions::add);
                }
            }
        }
        return actions;
    }
}
