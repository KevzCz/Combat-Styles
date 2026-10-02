package net.pixeldreamstudios.combat_styles.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum HudDensity {

    MINIMAL,
    CHIPS,
    EXPANDED;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HudDensity byKey(String key) {
        if (key != null) {
            for (HudDensity density : values()) {
                if (density.key().equalsIgnoreCase(key)) {
                    return density;
                }
            }
        }
        return CHIPS;
    }

    public static List<String> keys() {
        List<String> keys = new ArrayList<>(values().length);
        for (HudDensity density : values()) {
            keys.add(density.key());
        }
        return keys;
    }
}
