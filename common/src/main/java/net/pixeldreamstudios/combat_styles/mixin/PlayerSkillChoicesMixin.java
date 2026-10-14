package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.skill.SkillChoices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerSkillChoicesMixin implements SkillChoices.Holder {

    @Unique
    private final SkillChoices.Data combat_styles$skillChoices = new SkillChoices.Data();

    @Override
    public SkillChoices.Data combat_styles$skillChoices() {
        return combat_styles$skillChoices;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$saveChoices(CompoundTag tag, CallbackInfo callback) {
        tag.put(SkillChoices.NBT_KEY, combat_styles$skillChoices.save());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combat_styles$loadChoices(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(SkillChoices.NBT_KEY)) {
            combat_styles$skillChoices.load(tag.getCompound(SkillChoices.NBT_KEY));
        }
    }
}
