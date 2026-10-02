package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import net.pixeldreamstudios.combat_styles.affix.DamageTypeMatch;

import java.util.List;
import java.util.Optional;

public record DealtGate(Optional<Float> above, Optional<Float> below,
                        List<String> damageTypes, List<String> targets) {

    public static final DealtGate ANY = new DealtGate(Optional.empty(), Optional.empty(),
            List.of(), List.of());

    public static final MapCodec<DealtGate> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("damage_above").forGetter(DealtGate::above),
                    Codec.FLOAT.optionalFieldOf("damage_below").forGetter(DealtGate::below),
                    Codec.STRING.listOf().optionalFieldOf("damage_type", List.of())
                            .forGetter(DealtGate::damageTypes),
                    Codec.STRING.listOf().optionalFieldOf("target", List.of())
                            .forGetter(DealtGate::targets)
            ).apply(instance, DealtGate::new));

    public DealtGate {
        damageTypes = damageTypes == null ? List.of() : List.copyOf(damageTypes);
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    public boolean isEmpty() {
        return above.isEmpty() && below.isEmpty() && damageTypes.isEmpty() && targets.isEmpty();
    }

    public boolean needsTarget() {
        return !targets.isEmpty();
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
            if (entry.equals(full) || entry.equals(id.getPath())) {
                return true;
            }
        }
        return false;
    }
}
