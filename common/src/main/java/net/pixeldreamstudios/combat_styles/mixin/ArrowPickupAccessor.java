package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface ArrowPickupAccessor {

    @Invoker("setPickupItemStack")
    void combat_styles$setPickupItemStack(ItemStack stack);
}
