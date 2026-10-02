package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;

public record ShootGate(boolean enabled, List<String> entities, boolean fromSummon) {

    public static final ShootGate ANY = new ShootGate(false, List.of(), true);

    public static final Codec<ShootGate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE).forGetter(ShootGate::enabled),
            Codec.STRING.listOf().optionalFieldOf("entity", List.of())
                    .forGetter(ShootGate::entities),
            Codec.BOOL.optionalFieldOf("from_summon", Boolean.TRUE)
                    .forGetter(ShootGate::fromSummon)
    ).apply(instance, ShootGate::new));

    public ShootGate {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }

    public boolean isEmpty() {
        return !enabled && entities.isEmpty() && fromSummon;
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        ShotEvent shot = context.shot();
        if (shot == null) {
            return "shoot (this is not a shot event)";
        }
        if (!fromSummon && shot.viaSummon()) {
            return "from_summon (a summon fired this)";
        }
        if (!entities.isEmpty() && !namesProjectile(shot.projectile())) {
            return "entity";
        }
        return null;
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
