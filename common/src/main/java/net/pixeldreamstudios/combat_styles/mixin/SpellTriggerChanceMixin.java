package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellTriggers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;

@Pseudo
@Mixin(targets = "net.spell_engine.internals.SpellTriggers", remap = false)
public abstract class SpellTriggerChanceMixin {

    @ModifyExpressionValue(method = "evaluateTrigger", remap = false, require = 0,
            at = @At(value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Trigger;chance:F",
                    remap = false))
    private static float combat_styles$tunedChance(float chance,
                                                   @Local(argsOnly = true) Holder<Spell> spell,
                                                   @Local(argsOnly = true) SpellTriggers.Event event) {
        if (event == null || spell == null) {
            return chance;
        }
        String id = spell.unwrapKey().map(ResourceKey::location).map(Object::toString).orElse(null);
        return SpellLockState.tunedChance(event.player, id, chance);
    }
}
