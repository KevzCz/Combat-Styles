package net.pixeldreamstudios.combat_styles.compat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class SpellPowerOverrides {

    private record Entry(WeakReference<Object> marker, float damage) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private SpellPowerOverrides() {
    }

    public static void register(Object marker, float damage) {
        ENTRIES.removeIf(entry -> entry.marker().get() == null);
        if (marker != null && damage >= 0.0F) {
            ENTRIES.add(new Entry(new WeakReference<>(marker), damage));
        }
    }

    public static float damageFor(Object marker) {
        if (marker == null || ENTRIES.isEmpty()) {
            return SpellEngineCompat.INHERIT_DAMAGE;
        }
        for (Entry entry : ENTRIES) {
            if (entry.marker().get() == marker) {
                return entry.damage();
            }
        }
        return SpellEngineCompat.INHERIT_DAMAGE;
    }
}
