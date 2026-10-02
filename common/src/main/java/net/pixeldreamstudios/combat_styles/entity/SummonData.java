package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;

import java.util.UUID;

public interface SummonData {

    UUID combat_styles$summonOwner();

    void combat_styles$setSummonOwner(Player owner);

    void combat_styles$setSummonOwnerId(UUID owner);

    float combat_styles$summonDamage();

    void combat_styles$setSummonDamage(float damage);

    DamageSpec combat_styles$summonSpec();

    void combat_styles$applySummonGoals();

    void combat_styles$setSummonSpec(DamageSpec spec);
}
