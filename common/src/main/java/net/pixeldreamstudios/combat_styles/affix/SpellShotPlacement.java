package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.entity.ConfigurableSummon;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class SpellShotPlacement {

    private static final double MIN_SPEED = 1.0E-6D;

    private record Pending(WeakReference<Object> tag, Player player, LivingEntity target,
                           ProjectileSpawn spawn, int index, int count, Entity origin) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private SpellShotPlacement() {
    }

    public static void expect(Object tag, Player player, LivingEntity target,
                              ProjectileSpawn spawn, int index, int count) {
        PENDING.removeIf(entry -> entry.tag().get() == null);
        if (tag == null || player == null || spawn == null || spawn.isEmpty()) {
            return;
        }
        PENDING.add(new Pending(new WeakReference<>(tag), player, target, spawn, index, count,
                null));
    }

    public static void expectFrom(Object tag, Player player, LivingEntity target, Entity origin) {
        PENDING.removeIf(entry -> entry.tag().get() == null);
        if (tag == null || player == null || origin == null) {
            return;
        }
        PENDING.add(new Pending(new WeakReference<>(tag), player, target, ProjectileSpawn.DEFAULT,
                0, 1, origin));
    }

    public static void place(Entity shot) {
        if (PENDING.isEmpty() || shot == null || shot.level().isClientSide()) {
            return;
        }
        Object tag = SpellEngineCompat.shotTagOf(shot);
        if (tag == null) {
            return;
        }
        for (Pending entry : PENDING) {
            if (entry.tag().get() == tag) {
                apply(shot, entry);
                return;
            }
        }
    }

    private static void apply(Entity shot, Pending entry) {
        Vec3 motion = shot.getDeltaMovement();
        double speed = motion.length();
        if (speed < MIN_SPEED) {
            return;
        }
        if (entry.origin() != null) {
            launchFrom(shot, entry, speed);
            return;
        }
        ProjectileSpawn spawn = entry.spawn();
        if (spawn.placesOrigin()) {
            Vec3 origin = spawn.originFor(entry.player(), entry.target(), entry.index(),
                    entry.count());
            shot.setPos(origin.x, origin.y, origin.z);
            shot.xo = origin.x;
            shot.yo = origin.y;
            shot.zo = origin.z;
        }
        Vec3 direction = spawn.directionFor(entry.player(), entry.target(),
                motion.scale(1.0D / speed), entry.index(), entry.count());
        shot.setDeltaMovement(direction.scale(speed).add(spawn.carriedMotion(entry.player())));
        SpellEngineCompat.faceAlong(shot, direction);
    }

    private static void launchFrom(Entity shot, Pending entry, double speed) {
        Entity origin = entry.origin();
        Vec3 from = origin instanceof ConfigurableSummon summon
                ? summon.abilityOrigin()
                : origin.getEyePosition();
        LivingEntity target = entry.target();
        Vec3 toward = target != null && target.isAlive()
                ? target.getBoundingBox().getCenter().subtract(from)
                : origin.getLookAngle();
        if (toward.lengthSqr() < MIN_SPEED) {
            return;
        }
        Vec3 direction = toward.normalize();
        shot.setPos(from.x, from.y, from.z);
        shot.xo = from.x;
        shot.yo = from.y;
        shot.zo = from.z;
        shot.setDeltaMovement(direction.scale(speed));
        SpellEngineCompat.faceAlong(shot, direction);
    }
}
