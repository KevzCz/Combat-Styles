package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.network.HoldComboPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ComboControl {

    private static final Set<UUID> REPEATED = ConcurrentHashMap.newKeySet();
    private ComboControl() {
    }

    public static void forget(UUID playerId) {
        ComboHold.forget(playerId);
        REPEATED.remove(playerId);
    }

    public static int adjust(Player player, int reported) {
        return reported;
    }

    public static void recordSwing(Player player, int reported) {
    }

    public static void markUsed(Player player, int reported) {
    }

    public static void beginSwing(Player player) {
    }

    public static void holdSlot(Player player) {
        holdSlot(player, 1);
    }

    public static void holdSlot(Player player, int slots) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        int held = Math.max(1, slots);
        if (ComboHold.pending(player)) {
            CombatDebug.note(player, "repeat — already owed, ignoring");
            return;
        }
        ComboHold.queue(player, held);
        ComboHold.apply(player);
        REPEATED.add(player.getUUID());
        if (player instanceof ServerPlayer served) {
            NetworkManager.sendHoldCombo(served, new HoldComboPayload(held));
        }
        CombatDebug.note(player, "repeat — combo held, next swing replays this slot");
    }

    public static boolean consumeRepeat(Player player) {
        return player != null && REPEATED.remove(player.getUUID());
    }

    public static boolean hasRefund(Player player) {
        return ComboHold.pending(player);
    }

    public static void clear(Player player) {
        if (player != null) {
            ComboHold.forget(player.getUUID());
        }
    }
}
