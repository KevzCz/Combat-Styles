package net.pixeldreamstudios.combat_styles.client;

public final class ScreenScaleState {

    private static float activeScale = ScaledScreen.FULL_SCALE;

    private ScreenScaleState() {
    }

    public static float activeScale() {
        return activeScale;
    }

    public static boolean isActive() {
        return activeScale < ScaledScreen.FULL_SCALE;
    }

    public static void begin(float scale) {
        activeScale = scale;
    }

    public static void end() {
        activeScale = ScaledScreen.FULL_SCALE;
    }
}
