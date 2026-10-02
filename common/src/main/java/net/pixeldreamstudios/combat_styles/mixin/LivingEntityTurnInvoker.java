package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityTurnInvoker {

    @Invoker("tickHeadTurn")
    float combat_styles$tickHeadTurn(float yRot, float animStep);
}
