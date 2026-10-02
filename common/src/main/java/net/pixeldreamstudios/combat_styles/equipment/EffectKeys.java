package net.pixeldreamstudios.combat_styles.equipment;

public final class EffectKeys {

    public static final String SEPARATOR = "#";

    private EffectKeys() {
    }

    public static String of(String itemId, String effectKey) {
        return itemId + SEPARATOR + effectKey;
    }

    public static String effectOf(String key) {
        int mark = key == null ? -1 : key.indexOf(SEPARATOR);
        return mark < 0 ? "" : key.substring(mark + SEPARATOR.length());
    }
}
