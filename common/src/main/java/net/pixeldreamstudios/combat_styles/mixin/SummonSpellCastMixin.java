package net.pixeldreamstudios.combat_styles.mixin;

import net.spell_engine.entity.SummonedEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.entity.SummonStrike;

@Pseudo
@Mixin(targets = "net.spell_engine.internals.casting.MobCastController", remap = false)
public abstract class SummonSpellCastMixin {

    @Shadow(remap = false)
    @Final
    private SummonedEntity entity;

    @Inject(method = {"performSpell", "fireChannelTick"}, at = @At("HEAD"), remap = false,
            require = 0)
    private void combat_styles$beginSummonCast(CallbackInfo callback) {
        SummonStrike.beginCast(entity);
    }

    @Inject(method = {"performSpell", "fireChannelTick"}, at = @At("RETURN"), remap = false,
            require = 0)
    private void combat_styles$endSummonCast(CallbackInfo callback) {
        SummonStrike.endCast(entity);
    }
}
