package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.AttackDrag;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class MissTracker {

    private static final int RESOLVE_DELAY = 6;

    private record Pending(Player player, int comboCount, long dueAt, long swingId) {
    }

    private static final Set<Pending> PENDING = ConcurrentHashMap.newKeySet();
    private static volatile BiConsumer<Player, Integer> handler;

    private MissTracker() {
    }

    public static void setHandler(BiConsumer<Player, Integer> resolver) {
        handler = resolver;
    }

    public static void schedule(Player player, int comboCount) {
        if (player == null || handler == null) {
            return;
        }
        long now = player.level().getGameTime();
        PENDING.add(new Pending(player, comboCount, now + delayFor(player, comboCount),
                CombatState.currentSwingId(player)));
    }

    private static int delayFor(Player player, int comboCount) {
        AttackDrag drag = SwingDrag.dragFor(player, comboCount);
        if (!drag.enabled()) {
            return RESOLVE_DELAY;
        }
        return RESOLVE_DELAY + Math.max(0, drag.ticks());
    }

    public static void tick(long gameTime) {
        if (PENDING.isEmpty()) {
            return;
        }
        PENDING.removeIf(pending -> {
            if (gameTime < pending.dueAt()) {
                return false;
            }
            Player player = pending.player();
            BiConsumer<Player, Integer> resolver = handler;
            boolean missed = player != null
                    && CombatState.swingMissed(player, pending.swingId());
            if (resolver != null && player != null && player.isAlive()
                    && !player.level().isClientSide()
                    && missed) {
                resolver.accept(player, pending.comboCount());
            }
            return true;
        });
    }
}
