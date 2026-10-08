package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.affix.DamageTypeMatch;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;
import java.util.Optional;

public record HurtGate(boolean enabled, List<String> damageTypes, Optional<Float> above,
                       Optional<Float> below, Optional<Float> totalAbove,
                       List<String> attackers) {

    public static final HurtGate ANY = new HurtGate(false, List.of(), Optional.empty(),
            Optional.empty(), Optional.empty(), List.of());

    public static final Codec<HurtGate> CODEC = Customs.wrap(HurtGate.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE).forGetter(HurtGate::enabled),
            Codec.STRING.listOf().optionalFieldOf("taken_damage_type", List.of())
                    .forGetter(HurtGate::damageTypes),
            Codec.FLOAT.optionalFieldOf("damage_above").forGetter(HurtGate::above),
            Codec.FLOAT.optionalFieldOf("damage_below").forGetter(HurtGate::below),
            Codec.FLOAT.optionalFieldOf("total_damage_above").forGetter(HurtGate::totalAbove),
            Codec.STRING.listOf().optionalFieldOf("attacker", List.of())
                    .forGetter(HurtGate::attackers)
    ).apply(instance, HurtGate::new)));

    public HurtGate {
        damageTypes = damageTypes == null ? List.of() : List.copyOf(damageTypes);
        attackers = attackers == null ? List.of() : List.copyOf(attackers);
    }

    public boolean isEmpty() {
        return !enabled && damageTypes.isEmpty() && above.isEmpty() && below.isEmpty()
                && totalAbove.isEmpty() && attackers.isEmpty();
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        HurtEvent hurt = context.hurt();
        if (hurt == null) {
            return "hurt (this is not a hurt event)";
        }
        if (!DamageTypeMatch.matches(damageTypes, hurt.source())) {
            return "taken_damage_type";
        }
        if (above.isPresent() && hurt.amount() <= above.get()) {
            return "damage_above";
        }
        if (below.isPresent() && hurt.amount() >= below.get()) {
            return "damage_below";
        }
        if (totalAbove.isPresent() && hurt.total() <= totalAbove.get()) {
            return "total_damage_above";
        }
        if (!attackers.isEmpty() && !namesAttacker(hurt.attacker())) {
            return "attacker";
        }
        return null;
    }

    private boolean namesAttacker(Entity attacker) {
        if (attacker == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
        if (id == null) {
            return false;
        }
        String full = id.toString();
        for (String entry : attackers) {
            if (entry.equals(full) || entry.equals(id.getPath())) {
                return true;
            }
        }
        return false;
    }

}
