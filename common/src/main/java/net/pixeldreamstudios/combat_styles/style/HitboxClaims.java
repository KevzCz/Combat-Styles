package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HitboxClaims {

    private record Claim(UUID owner, HitboxSet.Shape shape, int index, long stamp, long swing) {
    }

    private static final int STALE_TICKS = 40;

    private record Key(UUID target, boolean client) {
    }

    private static final Map<Key, Claim> CLAIMS = new ConcurrentHashMap<>();

    private static Key keyOf(Player player, Entity target) {
        return new Key(target.getUUID(), player.level().isClientSide());
    }

    private HitboxClaims() {
    }

    public static void applyDeclared(Player player, Entity target,
                                     HitboxSet boxes, int index) {
        if (player == null || target == null || boxes == null
                || index < 0 || index >= boxes.shapes().size()) {
            return;
        }
        long now = player.level().getGameTime();
        CLAIMS.put(keyOf(player, target), new Claim(player.getUUID(),
                boxes.shapes().get(index), index, now, SwingWindow.current(player, now)));
    }

    public static boolean record(Player player, Entity target, HitboxSet.Shape shape, int index) {
        if (player == null || target == null || shape == null) {
            return true;
        }
        long now = player.level().getGameTime();
        long swing = SwingWindow.current(player, now);
        Claim existing = CLAIMS.get(keyOf(player, target));
        if (existing != null && existing.owner().equals(player.getUUID())
                && existing.swing() == swing
                && existing.shape().priority() > shape.priority()) {
            return false;
        }
        CLAIMS.put(keyOf(player, target), new Claim(player.getUUID(), shape, index, now, swing));
        return true;
    }

    public static DamageSpec specFor(Player player, Entity target) {
        HitboxSet.Shape shape = shapeFor(player, target);
        return shape == null ? null : shape.damageSpec().orElse(null);
    }

    public static HitboxSet.Shape shapeFor(Player player, Entity target) {
        if (player == null || target == null) {
            return null;
        }
        Claim claim = CLAIMS.get(keyOf(player, target));
        if (claim == null || !claim.owner().equals(player.getUUID())) {
            return null;
        }
        if (player.level().getGameTime() - claim.stamp() > STALE_TICKS) {
            CLAIMS.remove(keyOf(player, target));
            return null;
        }
        return claim.shape();
    }

    public static void forget(Entity target) {
        if (target != null) {
            CLAIMS.keySet().removeIf(key -> key.target().equals(target.getUUID()));
        }
    }

    public static void clear() {
        CLAIMS.clear();
    }
}
