package net.pixeldreamstudios.combat_styles.equipment;

public final class EffectKeys {

    public static final String SEPARATOR = "#";
    public static final String COPY_SEPARATOR = "~";

    private EffectKeys() {
    }

    public static String of(String itemId, String effectKey) {
        return itemId + SEPARATOR + effectKey;
    }

    public static String ownerOf(String itemId, int copy) {
        return copy <= 0 ? itemId : itemId + COPY_SEPARATOR + copy;
    }

    public static boolean ownedBy(String owner, String itemId) {
        return owner.equals(itemId) || owner.startsWith(itemId + COPY_SEPARATOR);
    }

    public static String effectOf(String key) {
        int mark = key == null ? -1 : key.indexOf(SEPARATOR);
        return mark < 0 ? "" : key.substring(mark + SEPARATOR.length());
    }
}
