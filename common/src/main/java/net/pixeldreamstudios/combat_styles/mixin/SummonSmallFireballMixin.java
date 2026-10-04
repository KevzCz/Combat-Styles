package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(SmallFireball.class)
public abstract class SummonSmallFireballMixin {

    @Redirect(method = "onHitBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean combat_styles$noSummonFire(Level level, BlockPos pos, BlockState state) {
        if (SummonOwner.isSummon(((SmallFireball) (Object) this).getOwner())) {
            return false;
        }
        return level.setBlockAndUpdate(pos, state);
    }
}
