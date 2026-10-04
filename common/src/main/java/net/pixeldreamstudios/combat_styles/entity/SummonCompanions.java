package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonCompanions {

    private static final int UPKEEP_INTERVAL = 20;

    private static final class Slots {
        private final List<UUID> alive = new ArrayList<>();
        private long nextSpawn;
    }

    private static final Map<UUID, Map<String, Slots>> TRACKED = new ConcurrentHashMap<>();
    private static final Set<UUID> SUSTAINED = ConcurrentHashMap.newKeySet();

    private SummonCompanions() {
    }

    public static boolean sustains(Entity entity) {
        return entity != null && SUSTAINED.contains(entity.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now % UPKEEP_INTERVAL != 0L) {
            return;
        }
        Map<String, Slots> tracked = TRACKED.computeIfAbsent(player.getUUID(),
                id -> new ConcurrentHashMap<>());
        if (!player.isAlive()) {
            dismissAll(level, tracked);
            return;
        }
        Set<String> active = new HashSet<>();
        for (SummonProfiles.Sourced sourced : SummonProfiles.liveProfiles(player)) {
            SummonProfile.Resolved resolved = sourced.profile().resolve(player,
                    SummonProfiles.CONTEXT);
            Optional<SummonCompanion> companion = resolved.companion();
            if (companion.isEmpty() || companion.get().count() <= 0) {
                continue;
            }
            String key = sourced.key();
            if (!active.add(key)) {
                continue;
            }
            upkeep(level, player, tracked.computeIfAbsent(key, ignored -> new Slots()),
                    sourced.profile(), resolved, companion.get(), now);
        }
        for (Map.Entry<String, Slots> entry : new ArrayList<>(tracked.entrySet())) {
            if (!active.contains(entry.getKey())) {
                dismiss(level, entry.getValue());
                tracked.remove(entry.getKey());
            }
        }
    }

    private static void upkeep(ServerLevel level, ServerPlayer player, Slots slots,
                               SummonProfile profile, SummonProfile.Resolved resolved,
                               SummonCompanion companion, long now) {
        int before = slots.alive.size();
        slots.alive.removeIf(id -> {
            Entity entity = locate(level, id);
            if (entity != null && entity.level() != level) {
                entity.discard();
                entity = null;
            }
            boolean gone = entity == null || entity.isRemoved() || !entity.isAlive();
            if (gone) {
                SUSTAINED.remove(id);
            }
            return gone;
        });
        if (slots.alive.size() < before) {
            slots.nextSpawn = Math.max(slots.nextSpawn, now + companion.respawnTicks());
        }
        while (slots.alive.size() > companion.count()) {
            UUID extra = slots.alive.removeLast();
            SUSTAINED.remove(extra);
            SummonLifecycle.dismiss(locate(level, extra));
        }
        if (slots.alive.size() >= companion.count() || now < slots.nextSpawn) {
            return;
        }
        String entityId = profile.entity().orElse("");
        SummonProfile replacing = SummonProfiles.replacementFor(player, entityId);
        if (replacing != null && replacing != profile) {
            spawnReplacement(level, player, entityId);
            slots.nextSpawn = now + companion.respawnTicks();
            return;
        }
        int count = companion.count();
        for (int slot = slots.alive.size(); slot < count; slot++) {
            Entity spawned = SummonSpawner.spawnCompanion(level, player, profile, resolved,
                    companion, slot, count);
            if (spawned == null) {
                return;
            }
            slots.alive.add(spawned.getUUID());
            SUSTAINED.add(spawned.getUUID());
        }
    }

    private static Entity locate(ServerLevel level, UUID id) {
        Entity here = level.getEntity(id);
        if (here != null || level.getServer() == null) {
            return here;
        }
        for (ServerLevel other : level.getServer().getAllLevels()) {
            Entity found = other.getEntity(id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void spawnReplacement(ServerLevel level, ServerPlayer player, String entityId) {
        AffixEffects.Summon stand = new AffixEffects.Summon(entityId, 1,
                SummonCompanion.DEFAULT_RESPAWN_TICKS, 0.0F, false, 0.0F, Optional.empty(),
                Optional.empty(), 1.0F, Optional.empty(), Optional.empty(),
                SummonEquipment.NONE, SummonStats.NONE, false, false, false);
        SummonSpawner.applySummons(level, player, null, List.of(stand), 0.0F);
    }

    public static void dismissAll(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Map<String, Slots> tracked = TRACKED.get(player.getUUID());
        if (tracked != null) {
            dismissAll(level, tracked);
        }
    }

    private static void dismissAll(ServerLevel level, Map<String, Slots> tracked) {
        for (Slots slots : tracked.values()) {
            dismiss(level, slots);
        }
        tracked.clear();
    }

    private static void dismiss(ServerLevel level, Slots slots) {
        for (UUID id : slots.alive) {
            SUSTAINED.remove(id);
            SummonLifecycle.dismiss(locate(level, id));
        }
        slots.alive.clear();
    }

    public static void forget(UUID playerId) {
        Map<String, Slots> tracked = TRACKED.remove(playerId);
        if (tracked != null) {
            for (Slots slots : tracked.values()) {
                slots.alive.forEach(SUSTAINED::remove);
            }
        }
    }
}
