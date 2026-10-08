package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.EffectChance;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

public final class AbilityStrike {

    private AbilityStrike() {
    }

    public static boolean hit(Entity attacker, Player owner, LivingEntity victim, float amount,
                              AbilityConfig.Entry tuned) {
        if (attacker == null || owner == null || victim == null || !victim.isAlive()
                || amount < SummonTuning.get().minimumDamage) {
            return false;
        }
        DamageHurt.bypassingIFrames(victim, sourceFor(attacker, owner, tuned), amount);
        CombatEffectDealer.onSummonHit(owner, victim, amount);
        afterHit(owner, victim, amount, tuned);
        return true;
    }

    public static void afterHit(Player owner, LivingEntity victim, float amount,
                                AbilityConfig.Entry tuned) {
        if (tuned == null) {
            return;
        }
        if (tuned.heals()) {
            owner.heal(amount * tuned.damage.lifesteal);
        }
        if (tuned.marks() && EffectChance.rolls(owner, tuned.damage.appliesChance)) {
            AbilityStacks.apply(victim, tuned.damage.applies, tuned.damage.appliesStacks,
                    AbilityStacks.DEFAULT_DURATION_TICKS, AbilityStacks.DEFAULT_MAX_STACKS);
        }
    }

    public static DamageSource sourceFor(Entity attacker, Player owner, AbilityConfig.Entry tuned) {
        Holder<DamageType> declared = holderOf(attacker, tuned == null ? null
                : ResourceLocation.tryParse(nullToEmpty(tuned.damageType())));
        if (declared != null) {
            return new DamageSource(declared, attacker, owner);
        }
        Holder<DamageType> school = holderOf(attacker, SpellPowerAccess.damageTypeOf(
                tuned == null ? null : tuned.school()));
        if (school != null) {
            return new DamageSource(school, attacker, owner);
        }
        return owner.damageSources().indirectMagic(attacker, owner);
    }

    private static Holder<DamageType> holderOf(Entity attacker, ResourceLocation typeId) {
        if (typeId == null) {
            return null;
        }
        return attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, typeId))
                .orElse(null);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
