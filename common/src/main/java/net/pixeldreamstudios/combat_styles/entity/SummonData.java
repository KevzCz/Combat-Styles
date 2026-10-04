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

    boolean combat_styles$spectral();

    void combat_styles$setSpectral(boolean spectral);

    UUID combat_styles$castBy();

    void combat_styles$setCastBy(UUID caster);

    SummonFollow combat_styles$follow();

    void combat_styles$setFollow(SummonFollow follow);
}
