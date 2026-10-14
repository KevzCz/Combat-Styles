package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.compat.puffish.TreePools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTreePoolsMixin implements TreePools.Holder {

    @Unique
    private final TreePools.Data combat_styles$treePools = new TreePools.Data();

    @Override
    public TreePools.Data combat_styles$treePools() {
        return combat_styles$treePools;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$saveTreePools(CompoundTag tag, CallbackInfo callback) {
        tag.put(TreePools.NBT_KEY, combat_styles$treePools.save());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$loadTreePools(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(TreePools.NBT_KEY)) {
            combat_styles$treePools.load(tag.getCompound(TreePools.NBT_KEY));
        }
    }
}
