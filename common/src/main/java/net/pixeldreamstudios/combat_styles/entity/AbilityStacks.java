package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import net.pixeldreamstudios.combat_styles.effect.EffectLookup;

public final class AbilityStacks {

    public static final int DEFAULT_DURATION_TICKS = 100;
    public static final int DEFAULT_MAX_STACKS = 5;
    private static final int NONE = 0;

    private AbilityStacks() {
    }

    public static int apply(LivingEntity victim, String effectId, int stacks, int durationTicks,
                            int maxStacks) {
        Holder<MobEffect> effect = EffectLookup.of(effectId);
        if (victim == null || effect == null || stacks <= 0) {
            return NONE;
        }
        MobEffectInstance current = victim.getEffect(effect);
        int held = current == null ? NONE : current.getAmplifier() + 1;
        int cap = maxStacks <= 0 ? DEFAULT_MAX_STACKS : maxStacks;
        int total = Math.min(cap, held + stacks);
        int duration = durationTicks <= 0 ? DEFAULT_DURATION_TICKS : durationTicks;
        victim.addEffect(new MobEffectInstance(effect, duration, total - 1));
        return total;
    }

    public static int held(LivingEntity victim, String effectId) {
        Holder<MobEffect> effect = EffectLookup.of(effectId);
        if (victim == null || effect == null) {
            return NONE;
        }
        MobEffectInstance current = victim.getEffect(effect);
        return current == null ? NONE : current.getAmplifier() + 1;
    }

    public static int consume(LivingEntity victim, String effectId) {
        int held = held(victim, effectId);
        if (held > NONE) {
            victim.removeEffect(EffectLookup.of(effectId));
        }
        return held;
    }
}
