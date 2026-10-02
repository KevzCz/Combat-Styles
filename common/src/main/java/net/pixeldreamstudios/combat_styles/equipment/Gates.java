package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class Gates {

    private static final String SEPARATOR = "|";

    private record Window(int used, long expiresAt, long openedAt) {
    }

    private static final String FIELD = "|";
    private static final long UNKNOWN_OPENING = 0L;
    public static final long NOT_OPEN = -1L;
    private static final int MARK_FIELDS = 3;

    private static final Map<String, Window> OPEN = new ConcurrentHashMap<>();
    private static final Map<String, Window> MIRRORED = new ConcurrentHashMap<>();
    private static final Set<UUID> CHANGED = ConcurrentHashMap.newKeySet();

    private Gates() {
    }

    public static void forget(UUID playerId) {
        OPEN.keySet().removeIf(key -> key.startsWith(playerId + SEPARATOR));
        CHANGED.remove(playerId);
    }

    public static void clear() {
        OPEN.clear();
    }

    private static String keyOf(Player player, GateScope scope, String ownerId, String name) {
        return player.getUUID() + SEPARATOR + localKeyOf(scope, ownerId, name);
    }

    private static String localKeyOf(GateScope scope, String ownerId, String name) {
        return ownerId + EffectKeys.SEPARATOR + scope.prefix() + name;
    }

    private static Window windowOf(Player player, GateScope scope, String ownerId, String name) {
        return player.level().isClientSide()
                ? MIRRORED.get(localKeyOf(scope, ownerId, name))
                : OPEN.get(keyOf(player, scope, ownerId, name));
    }

    private static void put(Player player, String key, Window window) {
        OPEN.put(key, window);
        CHANGED.add(player.getUUID());
    }

    private static void drop(Player player, String key) {
        if (OPEN.remove(key) != null) {
            CHANGED.add(player.getUUID());
        }
    }

    public static boolean consumeChanged(Player player) {
        return player != null && CHANGED.remove(player.getUUID());
    }

    public static Set<String> marksFor(Player player) {
        if (player == null || OPEN.isEmpty()) {
            return Set.of();
        }
        long now = player.level().getGameTime();
        String prefix = player.getUUID() + SEPARATOR;
        Set<String> marks = new LinkedHashSet<>();
        for (Map.Entry<String, Window> entry : OPEN.entrySet()) {
            Window window = entry.getValue();
            if (!entry.getKey().startsWith(prefix) || now > window.expiresAt()) {
                continue;
            }
            marks.add(ChargeMirror.WINDOW_MARK + (window.expiresAt() - now) + FIELD
                    + window.used() + FIELD + entry.getKey().substring(prefix.length()));
        }
        return marks;
    }

    public static void acceptMarks(Set<String> marks, long now) {
        MIRRORED.clear();
        for (String mark : marks) {
            String[] fields = mark.substring(ChargeMirror.WINDOW_MARK.length())
                    .split(Pattern.quote(FIELD), MARK_FIELDS);
            if (fields.length < MARK_FIELDS) {
                continue;
            }
            try {
                MIRRORED.put(fields[2], new Window(Integer.parseInt(fields[1]),
                        now + Long.parseLong(fields[0]), UNKNOWN_OPENING));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static void clearMirror() {
        MIRRORED.clear();
    }

    public static String absoluteMark(String mark, long now) {
        String[] fields = mark.substring(ChargeMirror.WINDOW_MARK.length())
                .split(Pattern.quote(FIELD), MARK_FIELDS);
        if (fields.length < MARK_FIELDS) {
            return mark;
        }
        try {
            return ChargeMirror.WINDOW_MARK + (now + Long.parseLong(fields[0])) + FIELD
                    + fields[1] + FIELD + fields[2];
        } catch (NumberFormatException ignored) {
            return mark;
        }
    }

    private static String cooldownKey(GateScope scope, String name) {
        return scope.prefix() + name;
    }

    public static boolean cooling(Player player, GateScope scope, String ownerId,
                                  String name, EffectCooldown gate) {
        return gate.ticks() > 0 && Cooldowns.active(player, ownerId,
                cooldownKey(scope, name), 0, Cooldowns.Track.PROC);
    }

    public static int remaining(Player player, GateScope scope, String ownerId,
                                String name) {
        return Cooldowns.remaining(player, ownerId, cooldownKey(scope, name), 0,
                Cooldowns.Track.PROC);
    }

    public static void beginCooldown(Player player, GateScope scope, String ownerId,
                                     String name, EffectCooldown gate) {
        if (player.level().isClientSide() || gate.ticks() <= 0) {
            return;
        }
        Cooldowns.start(player, ownerId, cooldownKey(scope, name), 0,
                Cooldowns.Track.PROC, gate.ticks());
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "gate — " + ownerId + EffectKeys.SEPARATOR
                    + scope.prefix() + name + " cooling " + gate.ticks() + "t");
        }
    }

    public static boolean windowOpen(Player player, GateScope scope, String ownerId,
                                     String name) {
        Window open = windowOf(player, scope, ownerId, name);
        return open != null && player.level().getGameTime() <= open.expiresAt();
    }

    public static int windowRemaining(Player player, GateScope scope, String ownerId,
                                      String name) {
        Window open = windowOf(player, scope, ownerId, name);
        if (open == null) {
            return 0;
        }
        long left = open.expiresAt() - player.level().getGameTime();
        return left <= 0L ? 0 : (int) left;
    }

    public static long windowOpenedAt(Player player, GateScope scope, String ownerId,
                                      String name) {
        Window open = windowOf(player, scope, ownerId, name);
        if (open == null || player.level().getGameTime() > open.expiresAt()) {
            return NOT_OPEN;
        }
        return open.openedAt();
    }

    public static int used(Player player, GateScope scope, String ownerId, String name) {
        Window open = windowOf(player, scope, ownerId, name);
        return open == null ? 0 : open.used();
    }

    public static boolean arm(Player player, GateScope scope, String ownerId,
                              String name, EffectCooldown gate) {
        if (player.level().isClientSide()) {
            return false;
        }
        String key = keyOf(player, scope, ownerId, name);
        long now = player.level().getGameTime();
        Window armed = OPEN.get(key);
        if (armed != null && now <= armed.expiresAt()) {
            if (!gate.refreshOnUse()) {
                return false;
            }
            put(player, key, new Window(armed.used(), now + gate.span(), armed.openedAt()));
            return true;
        }
        if (cooling(player, scope, ownerId, name, gate)) {
            return false;
        }
        beginCooldown(player, scope, ownerId, name, gate);
        put(player, key, new Window(0, now + gate.span(), now));
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "gate — " + ownerId + EffectKeys.SEPARATOR
                    + scope.prefix() + name + " armed for " + gate.span() + "t");
        }
        return true;
    }

    public static void lockOut(Player player, GateScope scope, String ownerId, String name,
                               int ticks) {
        if (player.level().isClientSide() || ticks <= 0) {
            return;
        }
        put(player, keyOf(player, scope, ownerId, name),
                new Window(0, player.level().getGameTime() + ticks, player.level().getGameTime()));
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "gate — " + ownerId + EffectKeys.SEPARATOR
                    + scope.prefix() + name + " locked out for " + ticks + "t");
        }
    }

    public static boolean claim(Player player, GateScope scope, String ownerId,
                                String name, EffectCooldown gate) {
        return claim(player, scope, ownerId, name, gate, false);
    }

    public static boolean claim(Player player, GateScope scope, String ownerId,
                                String name, EffectCooldown gate, boolean requireArmed) {
        if (player.level().isClientSide()) {
            return true;
        }
        if (requireArmed && !windowOpen(player, scope, ownerId, name)) {
            return false;
        }
        if (!gate.windowed()) {
            if (cooling(player, scope, ownerId, name, gate)) {
                return false;
            }
            beginCooldown(player, scope, ownerId, name, gate);
            return true;
        }
        String key = keyOf(player, scope, ownerId, name);
        long now = player.level().getGameTime();
        Window armed = OPEN.get(key);
        if (armed != null && now > armed.expiresAt()) {
            drop(player, key);
            armed = null;
        }
        if (armed == null) {
            if (cooling(player, scope, ownerId, name, gate)) {
                return false;
            }
            beginCooldown(player, scope, ownerId, name, gate);
            put(player, key, new Window(1, now + gate.span(), now));
            return true;
        }
        int used = armed.used() + 1;
        if (gate.limited() && used > gate.uses()) {
            close(player, key, ownerId, "window spent");
            return false;
        }
        long expiry = gate.refreshOnUse() ? now + gate.span() : armed.expiresAt();
        put(player, key, new Window(used, expiry, armed.openedAt()));
        if (gate.limited() && used == gate.uses()) {
            close(player, key, ownerId, "window spent");
        }
        return true;
    }

    private static void close(Player player, String key, String ownerId, String why) {
        drop(player, key);
        CombatDebug.skipped(player, ownerId, why);
    }

    public static void tick(Player player) {
        if (player == null || OPEN.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        String prefix = player.getUUID() + SEPARATOR;
        for (Map.Entry<String, Window> entry : OPEN.entrySet()) {
            if (!entry.getKey().startsWith(prefix) || now <= entry.getValue().expiresAt()) {
                continue;
            }
            drop(player, entry.getKey());
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "gate — " + entry.getKey().substring(prefix.length())
                        + " window lapsed");
            }
        }
    }
}
