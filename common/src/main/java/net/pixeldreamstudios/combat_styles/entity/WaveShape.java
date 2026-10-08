package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.api.AttackShape;

public enum WaveShape {

    HORIZONTAL(1.0F, 0.35F, 0.6F),
    VERTICAL(0.35F, 1.0F, 0.6F),
    FORWARD(0.35F, 0.35F, 1.0F);

    private final float widthRatio;
    private final float heightRatio;
    private final float depthRatio;

    WaveShape(float widthRatio, float heightRatio, float depthRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
        this.depthRatio = depthRatio;
    }

    public float widthRatio() {
        return widthRatio;
    }

    public float heightRatio() {
        return heightRatio;
    }

    public float depthRatio() {
        return depthRatio;
    }

    public static WaveShape byName(String name) {
        if (name == null) {
            return HORIZONTAL;
        }
        for (WaveShape shape : values()) {
            if (shape.name().equalsIgnoreCase(name)) {
                return shape;
            }
        }
        return HORIZONTAL;
    }

    public static WaveShape ordinalOf(int index) {
        WaveShape[] values = values();
        return index < 0 || index >= values.length ? HORIZONTAL : values[index];
    }

    public static WaveShape forShape(AttackShape shape) {
        if (shape == null) {
            return HORIZONTAL;
        }
        return switch (shape) {
            case THRUST -> FORWARD;
            case VERTICAL_SWEEP -> VERTICAL;
            case HORIZONTAL_SWEEP -> HORIZONTAL;
        };
    }
}
