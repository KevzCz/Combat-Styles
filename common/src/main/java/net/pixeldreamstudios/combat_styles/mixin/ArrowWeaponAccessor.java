package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface ArrowWeaponAccessor {

    @Accessor("firedFromWeapon")
    void combat_styles$setFiredFromWeapon(ItemStack stack);
}
