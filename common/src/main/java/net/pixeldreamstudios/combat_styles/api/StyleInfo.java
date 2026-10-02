package net.pixeldreamstudios.combat_styles.api;

import net.bettercombat.api.WeaponAttributes;

public interface StyleInfo {

    String styleId();

    double damageMultiplier();

    double upswing();

    double angle();

    float rangeMultiplier();

    float movementSpeedMultiplier();

    WeaponAttributes.HitBoxShape hitbox();

    String animation();

    String swingSoundId();

    boolean isConditional();

    String translationKey();

    String descriptionKey();
}
