package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

public record ProjectileHitGate(boolean enabled, List<String> entities, boolean fromSummon,
                                Optional<Double> distanceAbove, Optional<Double> distanceBelow,
                                DealtGate dealt, SpellProjectileGate spellProjectile) {

    public static final ProjectileHitGate ANY = new ProjectileHitGate(false, List.of(), true,
            Optional.empty(), Optional.empty(), DealtGate.ANY, SpellProjectileGate.ANY);

    public static final Codec<ProjectileHitGate> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE)
                            .forGetter(ProjectileHitGate::enabled),
                    Codec.STRING.listOf().optionalFieldOf("entity", List.of())
                            .forGetter(ProjectileHitGate::entities),
                    Codec.BOOL.optionalFieldOf("from_summon", Boolean.TRUE)
                            .forGetter(ProjectileHitGate::fromSummon),
                    Codec.DOUBLE.optionalFieldOf("distance_above")
                            .forGetter(ProjectileHitGate::distanceAbove),
                    Codec.DOUBLE.optionalFieldOf("distance_below")
                            .forGetter(ProjectileHitGate::distanceBelow),
                    DealtGate.CODEC.forGetter(ProjectileHitGate::dealt),
                    SpellProjectileGate.CODEC.codec()
                            .optionalFieldOf("spell_projectile", SpellProjectileGate.ANY)
                            .forGetter(ProjectileHitGate::spellProjectile)
            ).apply(instance, ProjectileHitGate::new));

    public ProjectileHitGate {
        entities = entities == null ? List.of() : List.copyOf(entities);
        dealt = dealt == null ? DealtGate.ANY : dealt;
        spellProjectile = spellProjectile == null
                ? SpellProjectileGate.ANY
                : spellProjectile;
    }

    public boolean isEmpty() {
        return !enabled && entities.isEmpty() && fromSummon
                && distanceAbove.isEmpty() && distanceBelow.isEmpty() && dealt.isEmpty()
                && spellProjectile.isEmpty();
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        ProjectileHitEvent hit = context.projectileHit();
        if (hit == null) {
            return "projectile_hit (this is not a projectile hit)";
        }
        if (!fromSummon && hit.viaSummon()) {
            return "from_summon (a summon fired this)";
        }
        if (!entities.isEmpty() && !namesProjectile(hit.projectile())) {
            return "entity";
        }
        if (distanceAbove.isPresent() && hit.distance() <= distanceAbove.get()) {
            return "distance_above (travelled " + hit.distance() + ")";
        }
        if (distanceBelow.isPresent() && hit.distance() >= distanceBelow.get()) {
            return "distance_below (travelled " + hit.distance() + ")";
        }
        String spellRejection = spellProjectile.rejects(context);
        if (spellRejection != null) {
            return spellRejection;
        }
        return dealt.rejects(context);
    }

    private boolean namesProjectile(Entity projectile) {
        if (projectile == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType());
        if (id == null) {
            return false;
        }
        String full = id.toString();
        for (String entry : entities) {
            if (entry.equals(full) || entry.equals(id.getPath())) {
                return true;
            }
        }
        return false;
    }
}
