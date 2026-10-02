package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityHurtAccessor {

    @Accessor("lastHurt")
    float combat_styles$getLastHurt();

    @Accessor("lastHurt")
    void combat_styles$setLastHurt(float lastHurt);
}
