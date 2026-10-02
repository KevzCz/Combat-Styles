package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.network.HitboxClaimPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

import java.util.ArrayList;
import java.util.List;

public final class PendingClaims {

    private static final List<Integer> IDS = new ArrayList<>();
    private static final List<Integer> INDICES = new ArrayList<>();
    private static final List<Integer> PRIORITIES = new ArrayList<>();

    private static boolean previewing;

    private PendingClaims() {
    }

    public static void beginPreview() {
        previewing = true;
    }

    public static void endPreview() {
        previewing = false;
    }

    public static boolean previewing() {
        return previewing;
    }

    public static void declare(int entityId, int boxIndex, int priority) {
        int existing = IDS.indexOf(entityId);
        if (existing >= 0) {
            if (PRIORITIES.get(existing) >= priority) {
                return;
            }
            INDICES.set(existing, boxIndex);
            PRIORITIES.set(existing, priority);
            return;
        }
        IDS.add(entityId);
        INDICES.add(boxIndex);
        PRIORITIES.add(priority);
    }

    public static void clear() {
        IDS.clear();
        INDICES.clear();
        PRIORITIES.clear();
    }

    public static int idOf(Entity target) {
        return target == null ? HitboxClaimPayload.NO_TARGET : target.getId();
    }

    public static void flush(Player player, int extraIndex, int delayStep, int cursorTargetId) {
        if (IDS.isEmpty()) {
            return;
        }
        NetworkManager.sendHitboxClaims(new HitboxClaimPayload(
                List.copyOf(IDS), List.copyOf(INDICES), List.copyOf(PRIORITIES),
                extraIndex, delayStep, cursorTargetId, player.getYRot(), player.getXRot()));
        clear();
    }
}
