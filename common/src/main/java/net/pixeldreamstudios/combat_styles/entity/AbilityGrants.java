package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityGrants {

    private static final int DISCOVER_TICKS = 20;
    private static final Map<UUID, Cached> LIVE = new ConcurrentHashMap<>();

    private record Cached(long tick, List<AbilityGrant> grants) {
    }

    private AbilityGrants() {
    }

    public static List<AbilityGrant> liveFor(Player owner) {
        if (owner == null || owner.level().isClientSide()) {
            return List.of();
        }
        long now = owner.level().getGameTime();
        Cached cached = LIVE.get(owner.getUUID());
        if (cached != null && cached.tick() == now) {
            return cached.grants();
        }
        List<AbilityGrant> fresh = List.copyOf(SummonProfiles.liveGrants(owner));
        LIVE.put(owner.getUUID(), new Cached(now, fresh));
        return fresh;
    }

    public static List<AbilityGrant> grantsFor(Player owner, Entity body) {
        if (owner == null || body == null) {
            return List.of();
        }
        List<AbilityGrant> live = liveFor(owner);
        if (live.isEmpty()) {
            return List.of();
        }
        boolean summon = SummonOwner.isSummon(body);
        if (!summon && !petOf(owner.getUUID(), body)) {
            return List.of();
        }
        List<AbilityGrant> matching = new ArrayList<>();
        for (AbilityGrant grant : live) {
            if (grant.reaches(body, summon)) {
                matching.add(grant);
            }
        }
        return matching;
    }

    public static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % DISCOVER_TICKS != 0) {
            return;
        }
        boolean anyone = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!liveFor(player).isEmpty()) {
                anyone = true;
                break;
            }
        }
        if (!anyone) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob) || !mob.isAlive() || mob instanceof ConfigurableSummon
                        || AbilityHosts.hosts(mob)) {
                    continue;
                }
                Player owner = ownerOf(level, mob);
                if (owner != null && !grantsFor(owner, mob).isEmpty()) {
                    AbilityHosts.hostGranted(owner, mob);
                }
            }
        }
    }

    public static void forget(UUID player) {
        LIVE.remove(player);
    }

    private static Player ownerOf(ServerLevel level, Mob mob) {
        if (SummonOwner.isSummon(mob)) {
            return SummonOwner.ownerPlayer(mob);
        }
        UUID owner = mob instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
        return owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
    }

    private static boolean petOf(UUID owner, Entity body) {
        return !SummonOwner.isSummon(body) && body instanceof OwnableEntity ownable
                && owner.equals(ownable.getOwnerUUID());
    }
}
