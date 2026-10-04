package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonCaps {

    private static final Map<UUID, Map<String, Deque<Entity>>> ACTIVE = new ConcurrentHashMap<>();

    private SummonCaps() {
    }

    public static void makeRoom(Player owner, String source, int max) {
        if (owner == null || max <= 0) {
            return;
        }
        Deque<Entity> alive = aliveFor(owner, source);
        while (alive.size() >= max) {
            SummonLifecycle.dismiss(alive.pollFirst());
        }
    }

    public static void track(Player owner, String source, Entity summon) {
        if (owner == null || summon == null) {
            return;
        }
        aliveFor(owner, source).addLast(summon);
    }

    private static Deque<Entity> aliveFor(Player owner, String source) {
        Deque<Entity> alive = ACTIVE.computeIfAbsent(owner.getUUID(), id -> new ConcurrentHashMap<>())
                .computeIfAbsent(source, key -> new ArrayDeque<>());
        alive.removeIf(entity -> entity.isRemoved() || !entity.isAlive());
        return alive;
    }

    public static void forget(UUID owner) {
        ACTIVE.remove(owner);
    }
}
