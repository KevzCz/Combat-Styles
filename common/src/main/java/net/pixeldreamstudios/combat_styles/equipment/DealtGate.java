package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import net.pixeldreamstudios.combat_styles.affix.DamageTypeMatch;
import net.pixeldreamstudios.combat_styles.entity.MobFilter;

import java.util.List;
import java.util.Optional;

public record DealtGate(Optional<Float> above, Optional<Float> below,
                        List<String> damageTypes, List<String> targets,
                        List<String> targetEffects) {

    public static final String KEY_TARGET_EFFECTS = "target_effects";

    public static final DealtGate ANY = new DealtGate(Optional.empty(), Optional.empty(),
            List.of(), List.of(), List.of());

    public static final MapCodec<DealtGate> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("damage_above").forGetter(DealtGate::above),
                    Codec.FLOAT.optionalFieldOf("damage_below").forGetter(DealtGate::below),
                    Codec.STRING.listOf().optionalFieldOf("damage_type", List.of())
                            .forGetter(DealtGate::damageTypes),
                    Codec.STRING.listOf().optionalFieldOf("target", List.of())
                            .forGetter(DealtGate::targets),
                    Codec.STRING.listOf().optionalFieldOf(KEY_TARGET_EFFECTS, List.of())
                            .forGetter(DealtGate::targetEffects)
            ).apply(instance, DealtGate::new));

    public DealtGate {
        damageTypes = damageTypes == null ? List.of() : List.copyOf(damageTypes);
        targets = targets == null ? List.of() : List.copyOf(targets);
        targetEffects = targetEffects == null ? List.of() : List.copyOf(targetEffects);
    }

    public boolean isEmpty() {
        return above.isEmpty() && below.isEmpty() && damageTypes.isEmpty() && targets.isEmpty()
                && targetEffects.isEmpty();
    }

    public boolean needsTarget() {
        return !targets.isEmpty() || !targetEffects.isEmpty();
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        float dealt = context.dealtDamage();
        if (above.isPresent() && dealt <= above.get()) {
            return "damage_above (dealt " + dealt + ")";
        }
        if (below.isPresent() && dealt >= below.get()) {
            return "damage_below (dealt " + dealt + ")";
        }
        if (!damageTypes.isEmpty() && !matchesType(context)) {
            return "damage_type";
        }
        if (!targets.isEmpty() && !namesTarget(context.target())) {
            return "target";
        }
        if (!targetEffects.isEmpty() && !afflicted(context.target())) {
            return KEY_TARGET_EFFECTS;
        }
        return null;
    }

    private boolean matchesType(EquipmentContext context) {
        ProjectileHitEvent hit = context.projectileHit();
        if (hit != null) {
            return DamageTypeMatch.matches(damageTypes, hit.source());
        }
        return DamageTypeMatch.matches(damageTypes, CombatState.lastDealtType(context.player()));
    }

    private boolean namesTarget(LivingEntity victim) {
        if (victim == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id == null) {
            return false;
        }
        String full = id.toString();
        for (String entry : targets) {
            if (entry.startsWith(MobFilter.TAG_MARKER)) {
                if (MobFilter.tagged(victim.getType(), entry)) {
                    return true;
                }
            } else if (entry.equals(full) || entry.equals(id.getPath())) {
                return true;
            }
        }
        return false;
    }

    private boolean afflicted(LivingEntity victim) {
        if (victim == null) {
            return false;
        }
        for (String entry : targetEffects) {
            ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id == null) {
                continue;
            }
            Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(id);
            if (effect.isPresent() && victim.hasEffect(effect.get())) {
                return true;
            }
        }
        return false;
    }
}
