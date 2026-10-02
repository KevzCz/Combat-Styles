package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.api.SwingTimingInfo;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.network.SwingHastePayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SwingHaste {

    private static final String KEY_SEPARATOR = "|";

    public static final float MIN_MULTIPLIER = SwingTimingInfo.MIN_MULTIPLIER;
    public static final float MAX_MULTIPLIER = SwingTimingInfo.MAX_MULTIPLIER;
    public static final float NEUTRAL_MULTIPLIER = SwingTimingInfo.NEUTRAL;
    public static final float MIN_SWING_TICKS = SwingTimingInfo.MIN_SWING_TICKS;
    public static final float MAX_UPSWING_RATE = 0.5F;

    private record Granted(float multiplier, int swings, String hitbox) {

        Granted spend() {
            return swings <= 1 ? null : new Granted(multiplier, swings - 1, hitbox);
        }
    }

    private static final Map<String, Granted> GRANTED = new ConcurrentHashMap<>();
    private static final Map<String, Float> SEQUENCE = new ConcurrentHashMap<>();
    private static final Map<String, Item> LAST_WEAPON = new ConcurrentHashMap<>();
    private static final Map<String, Long> SWING_COUNTER = new ConcurrentHashMap<>();

    private SwingHaste() {
    }

    private static String key(Player player) {
        return player.getUUID() + KEY_SEPARATOR + player.level().isClientSide();
    }

    public static void forget(UUID playerId) {
        for (String suffix : new String[] {"|true", "|false"}) {
            GRANTED.remove(playerId + suffix);
            SEQUENCE.remove(playerId + suffix);
            LAST_WEAPON.remove(playerId + suffix);
            SWING_COUNTER.remove(playerId + suffix);
        }
    }

    public static void trackHeldWeapon(Player player) {
        if (player == null) {
            return;
        }
        String key = key(player);
        Item held = player.getMainHandItem().getItem();
        Item previous = LAST_WEAPON.put(key, held);
        if (previous != null && previous != held) {
            GRANTED.remove(key);
            SEQUENCE.remove(key);
        }
    }

    private static final ThreadLocal<Boolean> READING_SWING_DURATION =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isReadingSwingDuration() {
        return READING_SWING_DURATION.get();
    }

    public static void beginReadingSwingDuration() {
        READING_SWING_DURATION.set(Boolean.TRUE);
    }

    public static void endReadingSwingDuration() {
        READING_SWING_DURATION.set(Boolean.FALSE);
    }

    public static long swingIndex(Player player) {
        return player == null ? 0L : SWING_COUNTER.getOrDefault(key(player), 0L);
    }

    public static long beginSwing(Player player) {
        if (player == null) {
            return 0L;
        }
        return SWING_COUNTER.merge(key(player), 1L, Long::sum);
    }

    private static void trace(Player player, String what, Object detail) {
        if (player == null || !CombatStylesConfig.get().debugLogging()) {
            return;
        }
        CombatStyles.LOGGER.info("[HASTE] {} side={} tick={} {}",
                what, player.level().isClientSide() ? "C" : "S",
                player.tickCount, detail);
    }

    public static void setSequenceScale(Player player, float multiplier) {
        if (player == null) {
            return;
        }
        if (multiplier == NEUTRAL_MULTIPLIER) {
            SEQUENCE.remove(key(player));
            return;
        }
        SEQUENCE.put(key(player), Mth.clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER));
    }

    public static float sequenceScale(Player player) {
        if (player == null) {
            return NEUTRAL_MULTIPLIER;
        }
        return SEQUENCE.getOrDefault(key(player), NEUTRAL_MULTIPLIER);
    }

    public static void grant(Player player, float multiplier, int swings, String hitbox) {
        if (player == null || swings <= 0 || multiplier == NEUTRAL_MULTIPLIER) {
            return;
        }
        GRANTED.put(key(player),
                new Granted(Mth.clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER),
                        swings, hitbox));
        trace(player, "grant", "mult=" + multiplier + " swings=" + swings
                + " hitbox=" + hitbox);
    }

    public static void grantNow(Player player, float multiplier) {
        grant(player, multiplier, 1, null);
    }

    public static void accept(Player player, float multiplier, int swings, String hitbox) {
        if (swings == SwingHastePayload.THIS_SWING_ONLY) {
            if (multiplier == NEUTRAL_MULTIPLIER) {
                clearSelf(player);
                return;
            }
            grant(player, multiplier, 1, hitbox);
            return;
        }
        grant(player, multiplier, swings, hitbox);
    }

    public static float selfMultiplier(Player player) {
        return grantedMultiplier(player);
    }

    public static void clearSelf(Player player) {
        if (player != null) {
            Granted removed = GRANTED.remove(key(player));
            if (removed != null) {
                trace(player, "clear-grant", "was=" + removed.multiplier()
                        + " swings=" + removed.swings());
            }
        }
    }

    public static void consume(Player player, String hitbox) {
        if (player == null) {
            return;
        }
        String key = key(player);
        Granted granted = GRANTED.get(key);
        if (granted == null) {
            return;
        }
        if (granted.hitbox() != null && !granted.hitbox().equals(hitbox)) {
            return;
        }
        Granted next = granted.spend();
        if (next == null) {
            GRANTED.remove(key);
        } else {
            GRANTED.put(key, next);
        }
        trace(player, "consume", "mult=" + granted.multiplier()
                + " left=" + (next == null ? 0 : next.swings()) + " hitbox=" + hitbox);
    }

    public static void endSwing(Player player, long swing) {
        if (player == null) {
            return;
        }
        consume(player, pendingHitbox(player));
    }

    public static boolean armed(Player player) {
        return player != null && GRANTED.containsKey(key(player));
    }

    public static int pendingSwings(Player player) {
        if (player == null) {
            return 0;
        }
        Granted granted = GRANTED.get(key(player));
        return granted == null ? 0 : granted.swings();
    }

    public static String pendingHitbox(Player player) {
        if (player == null) {
            return null;
        }
        Granted granted = GRANTED.get(key(player));
        return granted == null ? null : granted.hitbox();
    }

    private static float grantedMultiplier(Player player) {
        if (player == null) {
            return NEUTRAL_MULTIPLIER;
        }
        Granted granted = GRANTED.get(key(player));
        return granted == null ? NEUTRAL_MULTIPLIER : granted.multiplier();
    }

    public static float ratio(Player player) {
        return grantedMultiplier(player);
    }

    public static float clamped(Player player) {
        if (player == null) {
            return NEUTRAL_MULTIPLIER;
        }
        float sequence = sequenceScale(player);
        float granted = grantedMultiplier(player);
        float combined = sequence * granted;
        return Mth.clamp(combined, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }
}
