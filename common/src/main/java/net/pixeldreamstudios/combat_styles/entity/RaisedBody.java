package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class RaisedBody {

    private static final List<String> DROPPED = List.of("UUID", "Health", "DeathTime", "HurtTime", "HurtByTimestamp", "Fire", "Motion", "FallDistance",
            "AbsorptionAmount", "active_effects", "Brain", "leash", "Passengers", "CustomName", "CustomNameVisible", "PortalCooldown", "Air",
            "OnGround", "Invulnerable", "NoAI", "DeathLootTable", "DeathLootTableSeed");
    private static final List<String> GEAR = List.of("ArmorItems", "HandItems", "body_armor_item");

    private RaisedBody() {
    }

    public static void copy(LivingEntity victim, Entity raised, boolean keepGear) {
        if (victim.getType() != raised.getType()) {
            return;
        }
        CompoundTag body = victim.saveWithoutId(new CompoundTag());
        DROPPED.forEach(body::remove);
        if (!keepGear) {
            GEAR.forEach(body::remove);
        }
        raised.load(body);
        if (raised instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
        }
    }
}
