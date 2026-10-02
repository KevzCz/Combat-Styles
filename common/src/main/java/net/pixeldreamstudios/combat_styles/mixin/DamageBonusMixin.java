package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.pixeldreamstudios.combat_styles.damage.DamageBonuses;

@Mixin({LivingEntity.class, Player.class})
public abstract class DamageBonusMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), ordinal = 0, argsOnly = true,
            require = 1)
    private float combat_styles$damageTypeBonus(float amount, DamageSource source) {
        return DamageBonuses.apply((LivingEntity) (Object) this, source, amount);
    }
}
