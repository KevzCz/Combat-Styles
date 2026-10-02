package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public record BlockEvent(DamageSource source, float amount, ItemStack shield,
                         boolean fullyBlocked) {

    public BlockEvent {
        shield = shield == null ? ItemStack.EMPTY : shield;
    }

    public Entity attacker() {
        if (source == null) {
            return null;
        }
        Entity direct = source.getEntity();
        return direct == null ? source.getDirectEntity() : direct;
    }
}
