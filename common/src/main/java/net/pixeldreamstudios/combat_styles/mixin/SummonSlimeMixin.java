package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(Slime.class)
public abstract class SummonSlimeMixin {

    @Unique
    private static final int NO_SPLIT_SIZE = 1;

    @ModifyVariable(method = "remove", at = @At("STORE"), ordinal = 0)
    private int combat_styles$noSplit(int size) {
        return SummonOwner.isSummon((Slime) (Object) this) ? NO_SPLIT_SIZE : size;
    }
}
