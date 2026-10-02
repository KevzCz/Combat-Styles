package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class ConfigSnapshot {

    private static final String MAIN = "main";
    private static final String STATUS_EFFECTS = "status_effects";
    private static final String ABILITIES = "abilities";
    private static final String EQUIPMENT_DROPS = "equipment_drops";

    private ConfigSnapshot() {
    }

    public static String capture() {
        JsonObject json = new JsonObject();
        json.add(MAIN, CombatStylesConfig.rulesJson());
        json.add(STATUS_EFFECTS, StatusEffectConfig.toJson());
        json.add(ABILITIES, AbilityConfig.toJson());
        json.add(EQUIPMENT_DROPS, EquipmentDropConfig.toJson());
        return json.toString();
    }

    public static boolean acceptRemote(String raw) {
        JsonObject json = parse(raw);
        if (json == null) {
            return false;
        }
        CombatStylesConfig.acceptServer(json.get(MAIN));
        StatusEffectConfig.accept(json.get(STATUS_EFFECTS));
        AbilityConfig.accept(json.get(ABILITIES));
        EquipmentDropConfig.accept(json.get(EQUIPMENT_DROPS));
        return true;
    }

    public static void restoreLocal() {
        CombatStylesConfig.load();
        StatusEffectConfig.load();
        AbilityConfig.load();
        EquipmentDropConfig.load();
    }

    private static JsonObject parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException exception) {
            CombatStyles.LOGGER.warn("Rejected malformed server config", exception);
            return null;
        }
    }
}
