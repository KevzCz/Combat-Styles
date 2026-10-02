package net.pixeldreamstudios.combat_styles.style;

public final class StyleIds {

    public static final String SEPARATOR = "_";
    public static final String SEQUENCE_DELIMITER = ";";
    public static final String STYLE_NAME_PREFIX = "combat_styles.style.";
    public static final String STYLE_DESC_PREFIX = "combat_styles.style_desc.";
    public static final String CATEGORY_PREFIX = "combat_styles.category.";
    public static final String HITBOX_PREFIX = "combat_styles.hitbox.";
    public static final String CONDITION_PREFIX = "combat_styles.condition.";
    public static final String RARITY_PREFIX = "combat_styles.rarity.";
    public static final String SCHOOL_PREFIX = "combat_styles.school.";

    private StyleIds() {
    }

    public static String categoryOf(String styleId) {
        int split = styleId.lastIndexOf(SEPARATOR);
        return split <= 0 ? styleId : styleId.substring(0, split);
    }

    public static int indexOf(String styleId) {
        int split = styleId.lastIndexOf(SEPARATOR);
        if (split < 0 || split == styleId.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(styleId.substring(split + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
