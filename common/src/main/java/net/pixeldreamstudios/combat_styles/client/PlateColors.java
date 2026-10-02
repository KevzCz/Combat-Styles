package net.pixeldreamstudios.combat_styles.client;

public final class PlateColors {

    private static final int CHANNEL = 0xFF;
    private static final int FULL = 255;

    private PlateColors() {
    }

    public static int lighten(int argb, float amount) {
        int alpha = (argb >>> 24) & CHANNEL;
        int red = clamp((int) (((argb >> 16) & CHANNEL) + FULL * amount));
        int green = clamp((int) (((argb >> 8) & CHANNEL) + FULL * amount));
        int blue = clamp((int) ((argb & CHANNEL) + FULL * amount));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int darken(int argb, float amount) {
        int alpha = (argb >>> 24) & CHANNEL;
        int red = (int) (((argb >> 16) & CHANNEL) * (1.0F - amount));
        int green = (int) (((argb >> 8) & CHANNEL) * (1.0F - amount));
        int blue = (int) ((argb & CHANNEL) * (1.0F - amount));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, FULL);
    }
}
