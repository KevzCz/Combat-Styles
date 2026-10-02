package net.pixeldreamstudios.combat_styles.style;

public final class ClientTooltipState {

    private static volatile boolean expanded;

    private ClientTooltipState() {
    }

    public static void setExpanded(boolean value) {
        expanded = value;
    }

    public static boolean expanded() {
        return expanded;
    }
}
