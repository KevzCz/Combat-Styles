package net.pixeldreamstudios.combat_styles.compat;

public record SpellChannel(int interval, int duration) {

    public static final SpellChannel NONE = new SpellChannel(0, 0);

    public boolean isChanneled() {
        return interval > 0 && duration > interval;
    }
}
