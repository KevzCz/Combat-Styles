package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTags {

    private static final Map<UUID, Map<String, Long>> EXPIRIES = new ConcurrentHashMap<>();

    private SkillTags() {
    }

    public static void add(ServerPlayer player, String tag, int ticks) {
        player.addTag(tag);
        EXPIRIES.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>())
                .put(tag, player.level().getGameTime() + Math.max(1, ticks));
    }

    public static void tick(ServerPlayer player) {
        Map<String, Long> expiries = EXPIRIES.get(player.getUUID());
        if (expiries == null || expiries.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        for (Map.Entry<String, Long> entry : Map.copyOf(expiries).entrySet()) {
            if (now >= entry.getValue()) {
                player.removeTag(entry.getKey());
                expiries.remove(entry.getKey());
            }
        }
    }

    public static void forget(ServerPlayer player) {
        Map<String, Long> expiries = EXPIRIES.remove(player.getUUID());
        if (expiries != null) {
            expiries.keySet().forEach(player::removeTag);
        }
    }
}
