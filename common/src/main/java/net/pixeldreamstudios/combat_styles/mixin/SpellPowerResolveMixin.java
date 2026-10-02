package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.compat.SpellPowerOverrides;

@Pseudo
@Mixin(targets = "net.spell_engine.internals.SpellExecution$Power", remap = false)
public abstract class SpellPowerResolveMixin {

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true, remap = false)
    private static void combat_styles$keepShotDamage(Spell spell, Spell.Impact impact,
                                                     LivingEntity caster,
                                                     SpellPower.Result power, Entity target,
                                                     CallbackInfoReturnable<SpellPower.Result> callback) {
        float damage = SpellPowerOverrides.damageFor(power);
        if (damage < 0.0F || impact == null || impact.action == null
                || impact.action.type != Spell.Impact.Action.Type.DAMAGE
                || impact.action.damage == null
                || impact.action.damage.spell_power_coefficient <= 0.0F) {
            return;
        }
        SpellPower.Result resolved = callback.getReturnValue();
        SpellSchool school = resolved == null ? power.school() : resolved.school();
        SpellPower.Result natural = SpellPower.getSpellPower(school, caster);
        callback.setReturnValue(new SpellPower.Result(school,
                damage / impact.action.damage.spell_power_coefficient,
                natural.criticalChance(), natural.criticalDamage()));
    }
}
