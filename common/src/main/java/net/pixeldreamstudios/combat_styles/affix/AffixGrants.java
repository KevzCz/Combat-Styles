package net.pixeldreamstudios.combat_styles.affix;

import java.util.ArrayList;
import java.util.List;

public final class AffixGrants {

    public static final String REMOVAL_PREFIX = "!";

    private AffixGrants() {
    }

    public static boolean isRemoval(String entry) {
        return entry != null && entry.startsWith(REMOVAL_PREFIX);
    }

    public static String affixOf(String entry) {
        return isRemoval(entry) ? entry.substring(REMOVAL_PREFIX.length()) : entry;
    }

    public static List<String> removals(List<String> entries) {
        List<String> removed = new ArrayList<>();
        for (String entry : entries) {
            if (isRemoval(entry)) {
                removed.add(affixOf(entry));
            }
        }
        return removed;
    }

    public static List<String> applied(List<String> current, List<String> entries) {
        List<String> merged = new ArrayList<>(current);
        for (String entry : entries) {
            if (!isRemoval(entry) && !merged.contains(entry)) {
                merged.add(entry);
            }
        }
        merged.removeAll(removals(entries));
        return merged;
    }
}
