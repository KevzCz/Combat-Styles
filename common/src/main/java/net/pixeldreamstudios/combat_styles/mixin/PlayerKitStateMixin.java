package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.kit.KitState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerKitStateMixin implements KitState.Holder {

    @Unique
    private final KitState.Data combat_styles$kitState = new KitState.Data();

    @Override
    public KitState.Data combat_styles$kitState() {
        return combat_styles$kitState;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$saveKitState(CompoundTag tag, CallbackInfo callback) {
        tag.put(KitState.NBT_KEY, combat_styles$kitState.save());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$loadKitState(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(KitState.NBT_KEY)) {
            combat_styles$kitState.load(tag.getCompound(KitState.NBT_KEY));
        }
    }
}
