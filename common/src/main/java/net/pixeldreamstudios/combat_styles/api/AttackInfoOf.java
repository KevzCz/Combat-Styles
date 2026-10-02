package net.pixeldreamstudios.combat_styles.api;

import net.bettercombat.api.WeaponAttributes;

import java.util.Optional;

public record AttackInfoOf(WeaponAttributes.Attack attack) implements AttackInfo {

    public static AttackInfo of(WeaponAttributes.Attack attack) {
        return attack == null ? AttackInfo.UNKNOWN : new AttackInfoOf(attack);
    }

    @Override
    public boolean isKnown() {
        return true;
    }

    @Override
    public AttackShape shape() {
        return AttackShape.of(attack.hitbox(), attack.angle());
    }

    @Override
    public WeaponAttributes.HitBoxShape hitbox() {
        return attack.hitbox();
    }

    @Override
    public double angle() {
        return attack.angle();
    }

    @Override
    public double upswing() {
        return attack.upswing();
    }

    @Override
    public double damageMultiplier() {
        return attack.damageMultiplier();
    }

    @Override
    public float rangeMultiplier() {
        return attack.rangeMultiplier();
    }

    @Override
    public Optional<String> animation() {
        return Optional.ofNullable(attack.animation());
    }
}
