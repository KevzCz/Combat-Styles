package net.pixeldreamstudios.combat_styles.api;

import net.bettercombat.api.WeaponAttributes;

import java.util.Optional;

public interface AttackInfo {

    AttackInfo UNKNOWN = new AttackInfo() {

        @Override
        public boolean isKnown() {
            return false;
        }

        @Override
        public AttackShape shape() {
            return AttackShape.HORIZONTAL_SWEEP;
        }

        @Override
        public WeaponAttributes.HitBoxShape hitbox() {
            return null;
        }

        @Override
        public double angle() {
            return 0.0D;
        }

        @Override
        public double upswing() {
            return 0.0D;
        }

        @Override
        public double damageMultiplier() {
            return 1.0D;
        }

        @Override
        public float rangeMultiplier() {
            return 1.0F;
        }

        @Override
        public Optional<String> animation() {
            return Optional.empty();
        }
    };

    boolean isKnown();

    AttackShape shape();

    WeaponAttributes.HitBoxShape hitbox();

    double angle();

    double upswing();

    double damageMultiplier();

    float rangeMultiplier();

    Optional<String> animation();

    default boolean isThrust() {
        return shape().isThrust();
    }

    default boolean isSweep() {
        return shape().isSweep();
    }
}
