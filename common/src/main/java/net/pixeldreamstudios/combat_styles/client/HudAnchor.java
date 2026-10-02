package net.pixeldreamstudios.combat_styles.client;

import java.util.Locale;

public enum HudAnchor {

    TOP_LEFT(0.0F, 0.0F),
    TOP_CENTER(0.5F, 0.0F),
    TOP_RIGHT(1.0F, 0.0F),
    MIDDLE_LEFT(0.0F, 0.5F),
    MIDDLE_CENTER(0.5F, 0.5F),
    MIDDLE_RIGHT(1.0F, 0.5F),
    BOTTOM_LEFT(0.0F, 1.0F),
    BOTTOM_CENTER(0.5F, 1.0F),
    BOTTOM_RIGHT(1.0F, 1.0F);

    private final float horizontal;
    private final float vertical;

    HudAnchor(float horizontal, float vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public int originX(int screenWidth, int panelWidth) {
        return Math.round((screenWidth - panelWidth) * horizontal);
    }

    public int originY(int screenHeight, int panelHeight) {
        return Math.round((screenHeight - panelHeight) * vertical);
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HudAnchor byKey(String key) {
        if (key != null) {
            for (HudAnchor anchor : values()) {
                if (anchor.key().equalsIgnoreCase(key)) {
                    return anchor;
                }
            }
        }
        return TOP_LEFT;
    }

    public static HudAnchor nearest(int centreX, int centreY, int screenWidth, int screenHeight) {
        HudAnchor closest = TOP_LEFT;
        double best = Double.MAX_VALUE;
        for (HudAnchor anchor : values()) {
            double x = screenWidth * anchor.horizontal;
            double y = screenHeight * anchor.vertical;
            double distance = (x - centreX) * (x - centreX) + (y - centreY) * (y - centreY);
            if (distance < best) {
                best = distance;
                closest = anchor;
            }
        }
        return closest;
    }
}
