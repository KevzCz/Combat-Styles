package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ComboHold {
    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private ComboHold() {
    }

    public static void queue(Player player, int slots) {
        if (player == null) {
            return;
        }
        PENDING.merge(player.getUUID(), Math.max(1, slots), Integer::sum);
    }

    public static void apply(Player player) {
        if (player == null) {
            return;
        }
        Integer held = PENDING.remove(player.getUUID());
        if (held == null || held <= 0) {
            return;
        }
        if (player instanceof PlayerAttackProperties properties) {
            properties.setComboCount(Math.max(0, properties.getComboCount() - held));
        }
    }

    public static void align(Player player, int combo) {
        if (player instanceof PlayerAttackProperties properties) {
            properties.setComboCount(Math.max(0, combo));
        }
    }

    public static boolean pending(Player player) {
        return player != null && PENDING.containsKey(player.getUUID());
    }

    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
    }
}
