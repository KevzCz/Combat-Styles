package net.pixeldreamstudios.combat_styles.api;

import net.bettercombat.api.WeaponAttributes;

public enum AttackShape {

    THRUST,
    HORIZONTAL_SWEEP,
    VERTICAL_SWEEP;

    public boolean isThrust() {
        return this == THRUST;
    }

    public boolean isSweep() {
        return this != THRUST;
    }

    public static AttackShape of(WeaponAttributes.HitBoxShape hitbox, double angle) {
        if (hitbox == WeaponAttributes.HitBoxShape.FORWARD_BOX) {
            return THRUST;
        }
        if (hitbox == WeaponAttributes.HitBoxShape.VERTICAL_PLANE) {
            return VERTICAL_SWEEP;
        }
        if (hitbox == WeaponAttributes.HitBoxShape.HORIZONTAL_PLANE) {
            return HORIZONTAL_SWEEP;
        }
        return Math.abs(angle) < THRUST_ARC ? THRUST : HORIZONTAL_SWEEP;
    }

    private static final double THRUST_ARC = 30.0D;
}
