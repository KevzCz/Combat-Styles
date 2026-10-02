package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubstitutionAnchor {

    public static final int NONE = 0;
    public static final int NO_COMBO = -1;

    private static final Map<UUID, Integer> ANCHORS = new ConcurrentHashMap<>();

    private SubstitutionAnchor() {
    }

    public static void engage(Player player, int comboCount) {
        if (player == null) {
            return;
        }
        Integer existing = ANCHORS.get(player.getUUID());
        if (existing != null && existing > comboCount) {
            ANCHORS.put(player.getUUID(), comboCount);
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "anchor engage — combo " + comboCount
                        + " RESET from stale " + existing);
            }
            return;
        }
        if (existing != null) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "anchor engage — combo " + comboCount
                        + " ignored, already " + existing);
            }
            return;
        }
        ANCHORS.put(player.getUUID(), comboCount);
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "anchor engage — combo " + comboCount + " SET");
        }
    }

    public static void release(Player player) {
        if (player == null) {
            return;
        }
        Integer removed = ANCHORS.remove(player.getUUID());
        if (removed != null) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "anchor release — was " + removed);
            }
        }
    }

    public static int of(Player player, int comboCount) {
        if (player == null) {
            return NONE;
        }
        Integer anchor = ANCHORS.get(player.getUUID());
        return anchor == null || anchor > comboCount ? NONE : anchor;
    }

    public static void forget(UUID playerId) {
        ANCHORS.remove(playerId);
    }
}
