package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.network.SwungAttackPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSwing {
    private record Declared(int slot, int size, String animation, String hitbox, long tick,
                            float aimX, float aimZ, long serial) {
    }

    public static final int NO_SLOT = -1;

    private static final int STALE_TICKS = 20;

    private static final Map<UUID, Declared> DECLARED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERIALS = new ConcurrentHashMap<>();

    private ClientSwing() {
    }

    public static void declare(Player player, SwungAttackPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        long serial = SERIALS.merge(player.getUUID(), 1L, Long::sum);
        DECLARED.put(player.getUUID(), new Declared(payload.slotIndex(),
                payload.sequenceSize(),
                payload.animation(), payload.hitbox(), player.level().getGameTime(),
                payload.aimX(), payload.aimZ(), serial));
    }

    public static long serialOf(Player player) {
        Declared declared = player == null ? null : DECLARED.get(player.getUUID());
        return declared == null ? -1L : declared.serial();
    }

    public static Vec3 declaredAim(Player player) {
        Declared declared = fresh(player);
        if (declared == null) {
            return null;
        }
        double length = Math.sqrt(declared.aimX() * declared.aimX()
                + declared.aimZ() * declared.aimZ());
        if (length <= 1.0E-4D) {
            return null;
        }
        return new Vec3(declared.aimX() / length, 0.0D, declared.aimZ() / length);
    }

    public static void forget(UUID playerId) {
        DECLARED.remove(playerId);
    }

    public static void consume(Player player) {
        if (player == null) {
            return;
        }
        DECLARED.remove(player.getUUID());
    }

    public static boolean played(Player player, int comboCount,
                                 WeaponAttributes.Attack attack) {
        if (player == null || attack == null) {
            return false;
        }
        Declared declared = fresh(player);
        return declared != null && matches(declared, attack);
    }

    public static boolean hasDeclaration(Player player) {
        return fresh(player) != null;
    }

    private static Declared fresh(Player player) {
        if (player == null) {
            return null;
        }
        Declared declared = DECLARED.get(player.getUUID());
        if (declared == null) {
            return null;
        }
        return player.level().getGameTime() - declared.tick() > STALE_TICKS ? null : declared;
    }

    public static boolean declaredElsewhere(Player player, WeaponAttributes.Attack attack) {
        Declared declared = fresh(player);
        return declared != null && attack != null && !matches(declared, attack);
    }

    public static boolean matchesDeclared(Player player, WeaponAttributes.Attack attack) {
        Declared declared = fresh(player);
        return declared != null && attack != null && matches(declared, attack);
    }

    public static int declaredSlot(Player player) {
        Declared declared = fresh(player);
        return declared == null ? NO_SLOT : declared.slot();
    }

    public static String animationOf(Player player) {
        Declared declared = player == null ? null : DECLARED.get(player.getUUID());
        return declared == null ? null : declared.animation();
    }

    private static boolean matches(Declared declared, WeaponAttributes.Attack attack) {
        String animation = attack.animation();
        if (animation == null || !animation.equals(declared.animation())) {
            return false;
        }
        String hitbox = attack.hitbox() == null ? "" : attack.hitbox().name();
        return hitbox.equals(declared.hitbox());
    }
}
