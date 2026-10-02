package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class ProjectileOrigins {

    private static final Map<UUID, Vec3> ORIGINS = new WeakHashMap<>();
    private static final int MAX_TRACKED = 512;

    private ProjectileOrigins() {
    }

    public static void remember(Entity projectile) {
        if (projectile == null || projectile.level().isClientSide()) {
            return;
        }
        if (ORIGINS.size() >= MAX_TRACKED) {
            ORIGINS.clear();
        }
        ORIGINS.put(projectile.getUUID(), projectile.position());
    }

    public static double travelled(Entity projectile) {
        if (projectile == null) {
            return 0.0D;
        }
        Vec3 origin = ORIGINS.get(projectile.getUUID());
        return origin == null ? 0.0D : origin.distanceTo(projectile.position());
    }

    public static void clear() {
        ORIGINS.clear();
    }
}
