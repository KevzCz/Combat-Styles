package net.pixeldreamstudios.combat_styles.progression;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.skill.SkillGrants;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StyleProgression {

    private static final int GRANTED_COPIES = 1;

    private StyleProgression() {
    }

    public static LearnedStyles of(Player player) {
        if (player instanceof LearnedStylesHolder holder) {
            return holder.combat_styles$learned();
        }
        return null;
    }

    public static void migrate(Player player) {
        LearnedStyles learned = of(player);
        if (learned != null) {
            learned.migrate();
        }
    }

    public static boolean hasLearned(Player player, String styleId) {
        if (player == null || styleId == null) {
            return false;
        }
        LearnedStyles learned = of(player);
        return (learned != null && learned.has(styleId)) || SkillGrants.stylesOf(player).contains(styleId);
    }

    public static boolean learn(Player player, String styleId) {
        LearnedStyles learned = of(player);
        return learned != null && learned.learn(styleId);
    }

    public static boolean learn(Player player, StyleEntry entry) {
        LearnedStyles learned = of(player);
        return learned != null && learned.learn(entry);
    }

    public static Map<String, StyleEntry> variants(Player player) {
        LearnedStyles learned = of(player);
        return learned == null ? Map.of() : learned.allVariants();
    }

    public static Map<String, Integer> all(Player player) {
        LearnedStyles learned = of(player);
        Map<String, Integer> own = learned == null ? Map.of() : learned.all();
        List<String> granted = SkillGrants.stylesOf(player);
        if (granted.isEmpty()) {
            return own;
        }
        Map<String, Integer> merged = new LinkedHashMap<>(own);
        for (String styleId : granted) {
            merged.putIfAbsent(styleId, GRANTED_COPIES);
        }
        return merged;
    }

    public static int copies(Player player, String styleId) {
        LearnedStyles learned = of(player);
        int own = learned == null ? 0 : learned.count(styleId);
        return SkillGrants.stylesOf(player).contains(styleId) ? Math.max(own, GRANTED_COPIES) : own;
    }

    public static int count(Player player) {
        LearnedStyles learned = of(player);
        return learned == null ? 0 : learned.distinctCount();
    }
}
