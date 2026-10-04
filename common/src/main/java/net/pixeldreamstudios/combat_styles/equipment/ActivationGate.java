package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ActivationGate(ShootGate shoot, BlockGate block, UseGate onUse,
                             ProjectileHitGate projectileHit, DealtGate dealt, CastGate cast,
                             SummonGate summon) {

    public static final ActivationGate ANY = new ActivationGate(ShootGate.ANY, BlockGate.ANY,
            UseGate.ANY, ProjectileHitGate.ANY, DealtGate.ANY, CastGate.ANY, SummonGate.ANY);

    public static final MapCodec<ActivationGate> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ShootGate.CODEC.optionalFieldOf("on_shoot", ShootGate.ANY)
                            .forGetter(ActivationGate::shoot),
                    BlockGate.CODEC.optionalFieldOf("on_shield_block", BlockGate.ANY)
                            .forGetter(ActivationGate::block),
                    UseGate.CODEC.optionalFieldOf("on_use", UseGate.ANY)
                            .forGetter(ActivationGate::onUse),
                    ProjectileHitGate.CODEC
                            .optionalFieldOf("on_projectile_hit", ProjectileHitGate.ANY)
                            .forGetter(ActivationGate::projectileHit),
                    DealtGate.CODEC.forGetter(ActivationGate::dealt),
                    CastGate.CODEC.optionalFieldOf(CastGate.KEY, CastGate.ANY)
                            .forGetter(ActivationGate::cast),
                    SummonGate.CODEC.forGetter(ActivationGate::summon)
            ).apply(instance, ActivationGate::new));

    public ActivationGate {
        shoot = shoot == null ? ShootGate.ANY : shoot;
        block = block == null ? BlockGate.ANY : block;
        onUse = onUse == null ? UseGate.ANY : onUse;
        projectileHit = projectileHit == null ? ProjectileHitGate.ANY : projectileHit;
        dealt = dealt == null ? DealtGate.ANY : dealt;
        cast = cast == null ? CastGate.ANY : cast;
        summon = summon == null ? SummonGate.ANY : summon;
    }

    public boolean reactsToProjectileHit() {
        return !projectileHit.isEmpty();
    }

    public boolean isEmpty() {
        return shoot.isEmpty() && block.isEmpty() && !usesItem()
                && projectileHit.isEmpty() && dealt.isEmpty() && cast.isEmpty() && summon.isEmpty();
    }

    public boolean usesItem() {
        return !onUse.isEmpty();
    }

    public boolean reactsToShoot() {
        return !shoot.isEmpty();
    }

    public boolean reactsToBlock() {
        return !block.isEmpty();
    }

    public boolean reactsToCast() {
        return cast.reactsToCast();
    }

    public boolean reactsToSummon() {
        return !summon.isEmpty();
    }

    public String rejects(EquipmentContext context) {
        String shotRejection = shoot.rejects(context);
        if (shotRejection != null) {
            return shotRejection;
        }
        String blockRejection = block.rejects(context);
        if (blockRejection != null) {
            return blockRejection;
        }
        String useRejection = onUse.rejects(context);
        if (useRejection != null) {
            return useRejection;
        }
        String projectileRejection = projectileHit.rejects(context);
        if (projectileRejection != null) {
            return projectileRejection;
        }
        String dealtRejection = dealt.rejects(context);
        if (dealtRejection != null) {
            return dealtRejection;
        }
        String castRejection = cast.rejects(context);
        if (castRejection != null) {
            return castRejection;
        }
        return summon.rejects(context);
    }
}
