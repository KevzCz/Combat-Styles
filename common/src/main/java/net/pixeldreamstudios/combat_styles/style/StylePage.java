package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.api.StyleInfo;

public record StylePage(String styleId,
                        String category,
                        int attackIndex,
                        WeaponAttributes.Attack attack,
                        ResourceLocation sourceWeapon,
                        StyleRarity rarity) implements StyleInfo {

    public static String idFor(String category, int attackIndex) {
        return category + StyleIds.SEPARATOR + attackIndex;
    }

    public double damageMultiplier() {
        return attack.damageMultiplier();
    }

    public double upswing() {
        return attack.upswing();
    }

    public double angle() {
        return attack.angle();
    }

    public float rangeMultiplier() {
        return attack.rangeMultiplier();
    }

    public float movementSpeedMultiplier() {
        return attack.movementSpeedMultiplier();
    }

    public WeaponAttributes.HitBoxShape hitbox() {
        return attack.hitbox();
    }

    public String animation() {
        return attack.animation();
    }

    public WeaponAttributes.Sound swingSound() {
        return attack.swingSound();
    }

    public String swingSoundId() {
        WeaponAttributes.Sound sound = attack.swingSound();
        return sound == null ? null : sound.id();
    }

    public boolean isConditional() {
        return attack.conditions() != null && attack.conditions().length > 0;
    }

    public String translationKey() {
        return StyleIds.STYLE_NAME_PREFIX + styleId;
    }

    public String descriptionKey() {
        return StyleIds.STYLE_DESC_PREFIX + styleId;
    }
}
