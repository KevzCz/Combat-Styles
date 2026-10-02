package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public record ShotEvent(Entity projectile, ItemStack weapon, boolean viaSummon) {

    public ShotEvent {
        weapon = weapon == null ? ItemStack.EMPTY : weapon;
    }
}
