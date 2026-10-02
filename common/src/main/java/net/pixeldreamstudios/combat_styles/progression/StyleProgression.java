package net.pixeldreamstudios.combat_styles.progression;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.Map;

public final class StyleProgression {

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
        return learned != null && learned.has(styleId);
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
        return learned == null ? Map.of() : learned.all();
    }

    public static int copies(Player player, String styleId) {
        LearnedStyles learned = of(player);
        return learned == null ? 0 : learned.count(styleId);
    }

    public static int count(Player player) {
        LearnedStyles learned = of(player);
        return learned == null ? 0 : learned.distinctCount();
    }
}
