package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.progression.LearnedStyles;
import net.pixeldreamstudios.combat_styles.progression.LearnedStylesHolder;

@Mixin(Player.class)
public abstract class PlayerLearnedStylesMixin implements LearnedStylesHolder {

    @Unique
    private final LearnedStyles combat_styles$learned = new LearnedStyles();

    @Override
    public LearnedStyles combat_styles$learned() {
        return combat_styles$learned;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$save(CompoundTag tag, CallbackInfo callback) {
        tag.put(LearnedStyles.NBT_KEY, combat_styles$learned.save());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$load(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(LearnedStyles.NBT_KEY)) {
            combat_styles$learned.load(tag.getCompound(LearnedStyles.NBT_KEY));
        }
    }
}
