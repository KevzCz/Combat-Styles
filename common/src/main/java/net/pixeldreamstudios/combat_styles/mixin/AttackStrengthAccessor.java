package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AttackStrengthAccessor {

    @Accessor("attackStrengthTicker")
    int combat_styles$attackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void combat_styles$setAttackStrengthTicker(int ticker);
}
