package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.api.CombatScheduler;
import net.pixeldreamstudios.combat_styles.compat.SpellChannel;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;

public final class SpellCasts {

    private static final String PULSE_LABEL = "cast_spell channel pulse";

    private SpellCasts() {
    }

    public static void cast(Player player, LivingEntity hit, CastSpell cast, float triggeringDamage) {
        if (player == null || cast == null || player.level().isClientSide()) {
            return;
        }
        if (!SpellEngineCompat.isLoaded()) {
            CombatDebug.note(player, "cast_spell skipped — Spell Engine not installed");
            return;
        }
        ResourceLocation spellId = ResourceLocation.tryParse(cast.spell());
        if (spellId == null || !EffectChance.rolls(player, cast.chance())) {
            return;
        }
        CastSpell.Target target = cast.resolvedTarget(hit != null);
        float damage = damageOf(player, cast, triggeringDamage, hit);
        if (!pulse(player, hit, cast, spellId, target, damage)) {
            return;
        }
        SpellChannel channel = SpellEngineCompat.channelOf(spellId, player.level(), cast.tuning());
        if (!channel.isChanneled()) {
            return;
        }
        int duration = cast.overridesChannel() ? cast.channelDuration() : channel.duration();
        for (int at = channel.interval(); at < duration; at += channel.interval()) {
            CombatScheduler.after(player, at, PULSE_LABEL, () -> {
                if (target == CastSpell.Target.HIT && (hit == null || !hit.isAlive())) {
                    return;
                }
                pulse(player, hit, cast, spellId, target, damage);
            });
        }
    }

    private static boolean pulse(Player player, LivingEntity hit, CastSpell cast,
                                 ResourceLocation spellId, CastSpell.Target target, float damage) {
        DamageSpec spec = cast.damageSpec().orElse(null);
        Object tag;
        ProjectileUpkeep.beginCast(player, spec, cast.ignoreIframes());
        try {
            tag = SpellEngineCompat.castSpell(player, spellId, target, hit, damage, cast.tuning());
        } finally {
            ProjectileUpkeep.endCast();
        }
        if (tag == null) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "cast_spell " + spellId + " did not cast");
            }
            return false;
        }
        ProjectileUpkeep.watchSpellShot(tag, player, spec, cast.ignoreIframes());
        return true;
    }

    private static float damageOf(Player player, CastSpell cast, float triggeringDamage,
                                  LivingEntity hit) {
        return cast.damageSpec()
                .map(spec -> spec.amountFor(player, triggeringDamage, hit))
                .orElse(SpellEngineCompat.INHERIT_DAMAGE);
    }
}
