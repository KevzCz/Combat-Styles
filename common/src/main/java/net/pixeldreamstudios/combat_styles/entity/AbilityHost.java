package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;

import java.util.List;

public interface AbilityHost {

    Entity body();

    Player owner();

    LivingEntity target();

    void retarget(LivingEntity target);

    float baseDamage();

    AbilityTuning orderTuning();

    AbilityCaster caster();

    default Vec3 origin() {
        return body().getEyePosition();
    }

    default Vec3 castStart() {
        AbilityCaster caster = caster();
        Vec3 start = caster == null ? null : caster.castStart();
        return start == null ? body().position() : start;
    }

    default void holdAt(Vec3 point) {
    }

    default void showBeam(Vec3 end) {
    }

    default boolean drawsOwnBeam() {
        return false;
    }

    default List<AbilityGrant> grants() {
        return List.of();
    }

    default boolean grantOnly() {
        return false;
    }

    static AbilityHost of(ConfigurableSummon summon, Player owner) {
        return new SummonHost(summon, owner);
    }

    record SummonHost(ConfigurableSummon summon, Player owner) implements AbilityHost {

        @Override
        public Entity body() {
            return (Entity) summon;
        }

        @Override
        public LivingEntity target() {
            return summon.abilityTarget();
        }

        @Override
        public void retarget(LivingEntity target) {
            summon.retarget(target);
        }

        @Override
        public float baseDamage() {
            return summon.abilityDamage();
        }

        @Override
        public AbilityTuning orderTuning() {
            return summon.orderTuning();
        }

        @Override
        public AbilityCaster caster() {
            return summon.caster();
        }

        @Override
        public Vec3 origin() {
            return summon.abilityOrigin();
        }

        @Override
        public Vec3 castStart() {
            Vec3 start = summon.abilityStart();
            return start == null ? AbilityHost.super.castStart() : start;
        }

        @Override
        public void holdAt(Vec3 point) {
            summon.holdAt(point);
        }

        @Override
        public void showBeam(Vec3 end) {
            summon.showBeam(end);
        }

        @Override
        public boolean drawsOwnBeam() {
            return summon.drawsOwnBeam();
        }

        @Override
        public List<AbilityGrant> grants() {
            return AbilityGrants.grantsFor(owner, body());
        }
    }
}
