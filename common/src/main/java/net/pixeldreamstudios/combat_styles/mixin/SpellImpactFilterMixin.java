package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.core.Holder;
import net.spell_engine.api.spell.Spell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.compat.TunedImpactFilter;

@Pseudo
@Mixin(targets = "net.spell_engine.internals.SpellModifiers", remap = false)
public abstract class SpellImpactFilterMixin {

    @Inject(method = "impactMatches", at = @At("HEAD"), cancellable = true, remap = false)
    private static void combat_styles$tunedFilter(Spell.Impact impact, Holder<Spell> spell,
                                                  Spell.Modifier.ImpactFilter filter,
                                                  CallbackInfoReturnable<Boolean> callback) {
        if (filter instanceof TunedImpactFilter tuned) {
            callback.setReturnValue(tuned.accepts(impact, spell));
        }
    }
}
