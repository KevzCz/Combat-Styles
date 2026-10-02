package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public record HurtEvent(DamageSource source, float amount, float total) {

    public Entity attacker() {
        if (source == null) {
            return null;
        }
        Entity direct = source.getEntity();
        return direct == null ? source.getDirectEntity() : direct;
    }
}
