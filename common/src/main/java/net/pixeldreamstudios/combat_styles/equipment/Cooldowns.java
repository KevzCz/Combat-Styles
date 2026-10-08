package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class Cooldowns {

    public enum Track {
        CHARGE,
        PROC
    }

    private static final long STALE_TICKS = 1200L;

    private record Key(UUID player, String itemId, String effectKey, int copy, Track track,
                       boolean client) {
    }

    private static Key keyOf(Player player, String itemId, String effectKey,
                             int copy, Track track) {
        return new Key(player.getUUID(), itemId, effectKey, copy, track,
                player.level().isClientSide());
    }

    private static final Map<Key, Long> ENTRIES = new ConcurrentHashMap<>();
    private static final Set<UUID> CHANGED = ConcurrentHashMap.newKeySet();

    private Cooldowns() {
    }

    public static void forget(UUID playerId) {
        ENTRIES.keySet().removeIf(key -> key.player().equals(playerId));
        CHANGED.remove(playerId);
    }

    public static boolean consumeChanged(Player player) {
        return player != null && CHANGED.remove(player.getUUID());
    }

    private static void markChanged(Player player) {
        if (!player.level().isClientSide()) {
            CHANGED.add(player.getUUID());
        }
    }

    public static void tick(long gameTime) {
        ENTRIES.values().removeIf(until -> gameTime > until + STALE_TICKS);
    }

    public static boolean active(Player player, String itemId, String effectKey,
                                 int copy, Track track) {
        Long until = ENTRIES.get(keyOf(player, itemId, effectKey, copy, track));
        return until != null && player.level().getGameTime() < until;
    }

    public static void start(Player player, String itemId, String effectKey,
                             int copy, Track track, int ticks) {
        if (ticks <= 0) {
            return;
        }
        ENTRIES.put(keyOf(player, itemId, effectKey, copy, track),
                player.level().getGameTime() + ticks);
        markChanged(player);
    }

    public static void shorten(Player player, String itemId, String effectKey,
                               int copy, Track track, int ticks) {
        if (ticks <= 0) {
            return;
        }
        Key key = keyOf(player, itemId, effectKey, copy, track);
        Long until = ENTRIES.get(key);
        if (until == null) {
            return;
        }
        ENTRIES.put(key, Math.max(player.level().getGameTime(), until - ticks));
        markChanged(player);
    }

    public static void clear(Player player, String itemId, Track track) {
        long now = player.level().getGameTime();
        boolean client = player.level().isClientSide();
        ENTRIES.entrySet().removeIf(entry -> entry.getKey().player().equals(player.getUUID())
                && entry.getKey().itemId().equals(itemId)
                && entry.getKey().track() == track
                && entry.getKey().client() == client
                && entry.getValue() > now);
        markChanged(player);
    }

    private static final String FIELD = "|";
    private static final int MARK_FIELDS = 4;
    private static final int TRACK_FIELD = 0;
    private static final int COPY_FIELD = 1;
    private static final int TICKS_FIELD = 2;
    private static final int KEY_FIELD = 3;

    public static Set<String> marksFor(Player player) {
        if (player == null || ENTRIES.isEmpty()) {
            return Set.of();
        }
        long now = player.level().getGameTime();
        Set<String> marks = new LinkedHashSet<>();
        for (Map.Entry<Key, Long> entry : ENTRIES.entrySet()) {
            Key key = entry.getKey();
            if (key.client() || !key.player().equals(player.getUUID())
                    || entry.getValue() <= now) {
                continue;
            }
            marks.add(ChargeMirror.COOLDOWN_MARK + key.track().name() + FIELD + key.copy()
                    + FIELD + (entry.getValue() - now) + FIELD
                    + EffectKeys.of(key.itemId(), key.effectKey()));
        }
        return marks;
    }

    public static String absoluteMark(String mark, long now) {
        String[] fields = mark.substring(ChargeMirror.COOLDOWN_MARK.length())
                .split(Pattern.quote(FIELD), MARK_FIELDS);
        if (fields.length < MARK_FIELDS) {
            return mark;
        }
        try {
            return ChargeMirror.COOLDOWN_MARK + fields[TRACK_FIELD] + FIELD + fields[COPY_FIELD]
                    + FIELD + (now + Long.parseLong(fields[TICKS_FIELD])) + FIELD
                    + fields[KEY_FIELD];
        } catch (NumberFormatException ignored) {
            return mark;
        }
    }

    public static void acceptMarks(Player player, Set<String> marks, long now) {
        if (player == null) {
            return;
        }
        ENTRIES.keySet().removeIf(key -> key.client() && key.player().equals(player.getUUID()));
        for (String mark : marks) {
            String[] fields = mark.substring(ChargeMirror.COOLDOWN_MARK.length())
                    .split(Pattern.quote(FIELD), MARK_FIELDS);
            if (fields.length < MARK_FIELDS) {
                continue;
            }
            int split = fields[KEY_FIELD].indexOf(EffectKeys.SEPARATOR);
            if (split <= 0) {
                continue;
            }
            try {
                Track track = Track.valueOf(fields[TRACK_FIELD]);
                ENTRIES.put(new Key(player.getUUID(), fields[KEY_FIELD].substring(0, split),
                                fields[KEY_FIELD].substring(split + EffectKeys.SEPARATOR.length()),
                                Integer.parseInt(fields[COPY_FIELD]), track, true),
                        now + Long.parseLong(fields[TICKS_FIELD]));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static void clearMirror(Player player) {
        if (player != null) {
            ENTRIES.keySet().removeIf(key -> key.client()
                    && key.player().equals(player.getUUID()));
        }
    }

    public static int remaining(Player player, String itemId, String effectKey,
                                int copy, Track track) {
        Long until = ENTRIES.get(keyOf(player, itemId, effectKey, copy, track));
        if (until == null) {
            return 0;
        }
        long left = until - player.level().getGameTime();
        return left <= 0L ? 0 : (int) left;
    }
}
