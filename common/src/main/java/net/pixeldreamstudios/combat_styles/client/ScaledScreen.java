package net.pixeldreamstudios.combat_styles.client;

public interface ScaledScreen {

    float FULL_SCALE = 1.0F;

    void fitTo(int realWidth, int realHeight);

    float uiScale();

    int virtualWidth();

    int virtualHeight();

    default boolean isScaledDown() {
        return uiScale() < FULL_SCALE;
    }
}
