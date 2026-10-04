package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.entity.SummonData;
import net.pixeldreamstudios.combat_styles.network.SummonSyncPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSummons {

    private record Known(UUID entityUuid, UUID owner, boolean spectral) {
    }

    private static final Map<Integer, Known> KNOWN = new ConcurrentHashMap<>();

    private ClientSummons() {
    }

    public static void accept(SummonSyncPayload payload) {
        if (payload == null) {
            return;
        }
        KNOWN.put(payload.entityId(),
                new Known(payload.entityUuid(), payload.owner(), payload.spectral()));
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(payload.entityId());
        bind(entity, known(entity));
    }

    private static Known known(Entity entity) {
        if (entity == null) {
            return null;
        }
        Known known = KNOWN.get(entity.getId());
        if (known == null) {
            return null;
        }
        if (!known.entityUuid().equals(entity.getUUID())) {
            KNOWN.remove(entity.getId());
            return null;
        }
        return known;
    }

    private static void bind(Entity entity, Known known) {
        if (known != null && entity instanceof SummonData data
                && data.combat_styles$summonOwner() == null) {
            data.combat_styles$setSummonOwnerId(known.owner());
        }
    }

    public static boolean spectral(Entity entity) {
        Known known = known(entity);
        return known != null && known.spectral();
    }

    public static boolean ownedByLocalPlayer(Entity entity) {
        Known known = known(entity);
        if (known == null) {
            return false;
        }
        bind(entity, known);
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && known.owner().equals(minecraft.player.getUUID());
    }

    public static void clear() {
        KNOWN.clear();
    }
}
