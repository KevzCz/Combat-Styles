package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DashGlide {

    private static final int MAX_TICKS = 40;

    private record Glide(double x, double z, int ticksLeft) {
    }

    private static final Map<UUID, Glide> ACTIVE = new ConcurrentHashMap<>();

    private DashGlide() {
    }

    public static void begin(Player player, Vec3 direction, double perTick, int ticks) {
        if (player == null || direction == null || ticks <= 1) {
            return;
        }
        ACTIVE.put(player.getUUID(), new Glide(direction.x * perTick,
                direction.z * perTick, Math.min(ticks, MAX_TICKS) - 1));
    }

    public static void cancel(Player player) {
        if (player != null) {
            ACTIVE.remove(player.getUUID());
        }
    }

    public static void forget(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void tick(Player player) {
        if (player == null) {
            return;
        }
        Glide glide = ACTIVE.get(player.getUUID());
        if (glide == null) {
            return;
        }
        if (glide.ticksLeft() <= 0 || player.horizontalCollision) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(glide.x(), velocity.y, glide.z());
        player.hurtMarked = true;
        ACTIVE.put(player.getUUID(),
                new Glide(glide.x(), glide.z(), glide.ticksLeft() - 1));
    }
}
