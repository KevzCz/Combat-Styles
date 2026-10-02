package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class SpellShotPlacement {

    private static final double MIN_SPEED = 1.0E-6D;

    private record Pending(WeakReference<Object> tag, Player player, LivingEntity target,
                           ProjectileSpawn spawn, int index, int count) {
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
        PENDING.add(new Pending(new WeakReference<>(tag), player, target, spawn, index, count));
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
}
