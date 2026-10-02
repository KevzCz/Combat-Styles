package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatState {
    private record Entry(long lastSwingTick, long lastHitTick, long swingId, long hitSwingId) {
    }

    private static final Map<UUID, Entry> STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DAMAGED = new ConcurrentHashMap<>();
    private static final Map<UUID, Holder<DamageType>> DAMAGED_BY = new ConcurrentHashMap<>();
    private static final Map<UUID, Holder<DamageType>> DEALT_BY = new ConcurrentHashMap<>();
    private static final Set<UUID> CHANGED = ConcurrentHashMap.newKeySet();

    private static final String FIELD = "|";
    private static final int MARK_FIELDS = 3;
    private static final long NEVER = -1L;
    private static final String MISSED = "1";
    private static final String CONNECTED = "0";

    private static volatile long mirroredSwingTick = NEVER;
    private static volatile long mirroredDamagedTick = NEVER;
    private static volatile boolean mirroredMissed;

    private CombatState() {
    }

    public static void forget(UUID playerId) {
        STATE.remove(playerId);
        SWUNG.remove(playerId);
        SWUNG_HAND.remove(playerId);
        SWUNG_AT.remove(playerId);
        SWUNG_ENTRY.remove(playerId);
        SWUNG_SLOT.remove(playerId);
        DAMAGED.remove(playerId);
        DAMAGED_BY.remove(playerId);
        DEALT_BY.remove(playerId);
        IDLE_AT_SWING.remove(playerId);
        SPRINTING_AT_SWING.remove(playerId);
        CHANGED.remove(playerId);
    }

    private static final Map<UUID, Long> IDLE_AT_SWING = new ConcurrentHashMap<>();
    private static final Set<UUID> SPRINTING_AT_SWING = ConcurrentHashMap.newKeySet();

    public static long idleAtSwing(Player player) {
        if (player.level().isClientSide()) {
            return idleTicks(player);
        }
        Long captured = IDLE_AT_SWING.get(player.getUUID());
        return captured == null ? idleTicks(player) : captured;
    }

    public static boolean sprintingAtSwing(Player player) {
        return player != null && SPRINTING_AT_SWING.contains(player.getUUID());
    }

    public static long recordSwing(Player player) {
        if (player == null || player.level().isClientSide()) {
            return 0L;
        }
        IDLE_AT_SWING.put(player.getUUID(), idleTicks(player));
        if (player.isSprinting()) {
            SPRINTING_AT_SWING.add(player.getUUID());
        } else {
            SPRINTING_AT_SWING.remove(player.getUUID());
        }
        UUID id = player.getUUID();
        Entry previous = STATE.get(id);
        long swingId = (previous == null ? 0L : previous.swingId()) + 1L;
        long now = player.level().getGameTime();
        long lastHit = previous == null ? 0L : previous.lastHitTick();
        long hitSwingId = previous == null ? -1L : previous.hitSwingId();
        if (previous != null && lastHit >= previous.lastSwingTick()
                && hitSwingId == previous.swingId() && lastHit == now) {
            hitSwingId = swingId;
        }
        STATE.put(id, new Entry(now, lastHit, swingId, hitSwingId));
        CHANGED.add(id);
        return swingId;
    }

    public static void recordHit(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        Entry previous = STATE.get(id);
        long now = player.level().getGameTime();
        if (previous == null) {
            STATE.put(id, new Entry(now, now, 0L, 0L));
            CHANGED.add(id);
            return;
        }
        STATE.put(id, new Entry(previous.lastSwingTick(), now,
                previous.swingId(), previous.swingId()));
        CHANGED.add(id);
    }

    public static long currentSwingId(Player player) {
        Entry entry = STATE.get(player.getUUID());
        return entry == null ? 0L : entry.swingId();
    }

    public static boolean swingMissed(Player player, long swingId) {
        Entry entry = STATE.get(player.getUUID());
        return entry == null || entry.hitSwingId() != swingId;
    }

    public static long hitSwingId(Player player) {
        Entry entry = STATE.get(player.getUUID());
        return entry == null ? -1L : entry.hitSwingId();
    }

    private static final long NEVER_SWUNG = 72000L;

    private static final Map<UUID, WeaponAttributes.Attack> SWUNG = new ConcurrentHashMap<>();
    private static final Map<UUID, AttackHand> SWUNG_HAND = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SWUNG_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, StyleEntry> SWUNG_ENTRY = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SWUNG_SLOT = new ConcurrentHashMap<>();

    public static void recordSwungSlot(Player player, int slot) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        SWUNG_SLOT.put(player.getUUID(), slot);
    }

    public static int swungSlot(Player player) {
        return player == null ? ClientSwing.NO_SLOT
                : SWUNG_SLOT.getOrDefault(player.getUUID(), ClientSwing.NO_SLOT);
    }

    public static void recordSwungAttack(Player player, WeaponAttributes.Attack attack) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (attack == null) {
            SWUNG.remove(player.getUUID());
        } else {
            SWUNG.put(player.getUUID(), attack);
        }
    }

    public static void recordSwungHand(Player player, AttackHand hand) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (hand == null) {
            SWUNG_HAND.remove(player.getUUID());
            SWUNG_AT.remove(player.getUUID());
        } else {
            SWUNG_HAND.put(player.getUUID(), hand);
            SWUNG_AT.put(player.getUUID(), player.level().getGameTime());
        }
        recordSwungAttack(player, hand == null ? null : hand.attack());
    }

    public static AttackHand swungHand(Player player) {
        return player == null ? null : SWUNG_HAND.get(player.getUUID());
    }

    public static long swungAt(Player player) {
        Long at = player == null ? null : SWUNG_AT.get(player.getUUID());
        return at == null ? NEVER_SWUNG : at;
    }

    public static long ticksSinceSwungHand(Player player) {
        Long at = player == null ? null : SWUNG_AT.get(player.getUUID());
        return at == null ? NEVER_SWUNG : player.level().getGameTime() - at;
    }

    public static void recordSwungEntry(Player player, StyleEntry entry) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (entry == null) {
            SWUNG_ENTRY.remove(player.getUUID());
        } else {
            SWUNG_ENTRY.put(player.getUUID(), entry);
        }
    }

    public static StyleEntry swungEntry(Player player) {
        return player == null ? null : SWUNG_ENTRY.get(player.getUUID());
    }

    public static WeaponAttributes.Attack swungAttack(Player player) {
        return player == null ? null : SWUNG.get(player.getUUID());
    }

    public static long idleTicks(Player player) {
        if (player.level().isClientSide()) {
            long swung = mirroredSwingTick;
            return swung == NEVER ? NEVER_SWUNG : player.level().getGameTime() - swung;
        }
        Entry entry = STATE.get(player.getUUID());
        if (entry == null) {
            return NEVER_SWUNG;
        }
        return player.level().getGameTime() - entry.lastSwingTick();
    }

    public static void recordDamaged(Player player) {
        recordDamaged(player, null);
    }

    public static void recordDamaged(Player player, DamageSource source) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        DAMAGED.put(player.getUUID(), player.level().getGameTime());
        CHANGED.add(player.getUUID());
        if (source != null) {
            DAMAGED_BY.put(player.getUUID(), source.typeHolder());
        }
    }

    public static boolean damagedWithin(Player player, int ticks) {
        if (player.level().isClientSide()) {
            long damaged = mirroredDamagedTick;
            return damaged != NEVER && player.level().getGameTime() - damaged <= ticks;
        }
        Long last = DAMAGED.get(player.getUUID());
        return last != null && player.level().getGameTime() - last <= ticks;
    }

    public static void recordDealtType(Player player, DamageSource source) {
        if (player == null || source == null || player.level().isClientSide()) {
            return;
        }
        DEALT_BY.put(player.getUUID(), source.typeHolder());
    }

    public static Holder<DamageType> lastDealtType(Player player) {
        return player == null ? null : DEALT_BY.get(player.getUUID());
    }

    public static Holder<DamageType> lastDamageType(Player player) {
        return player == null ? null : DAMAGED_BY.get(player.getUUID());
    }

    public static boolean lastSwingMissed(Player player) {
        if (player.level().isClientSide()) {
            return mirroredMissed;
        }
        Entry entry = STATE.get(player.getUUID());
        return entry != null && entry.hitSwingId() != entry.swingId();
    }

    public static boolean tracks(Player player) {
        return player != null
                && (STATE.containsKey(player.getUUID()) || DAMAGED.containsKey(player.getUUID()));
    }

    public static boolean consumeChanged(Player player) {
        return player != null && CHANGED.remove(player.getUUID());
    }

    public static String markFor(Player player) {
        long now = player.level().getGameTime();
        Long damaged = DAMAGED.get(player.getUUID());
        return ChargeMirror.COMBAT_MARK + idleTicks(player) + FIELD
                + (damaged == null ? NEVER : now - damaged) + FIELD
                + (lastSwingMissed(player) ? MISSED : CONNECTED);
    }

    public static void acceptMark(String mark, long now) {
        String[] fields = mark.substring(ChargeMirror.COMBAT_MARK.length())
                .split(Pattern.quote(FIELD), MARK_FIELDS);
        if (fields.length < MARK_FIELDS) {
            return;
        }
        try {
            long idle = Long.parseLong(fields[0]);
            long damagedAgo = Long.parseLong(fields[1]);
            mirroredSwingTick = idle >= NEVER_SWUNG ? NEVER : now - idle;
            mirroredDamagedTick = damagedAgo < 0L ? NEVER : now - damagedAgo;
            mirroredMissed = MISSED.equals(fields[2]);
        } catch (NumberFormatException ignored) {
        }
    }

    public static String absoluteMark(String mark, long now) {
        String[] fields = mark.substring(ChargeMirror.COMBAT_MARK.length())
                .split(Pattern.quote(FIELD), MARK_FIELDS);
        if (fields.length < MARK_FIELDS) {
            return mark;
        }
        try {
            long idle = Long.parseLong(fields[0]);
            long damagedAgo = Long.parseLong(fields[1]);
            return ChargeMirror.COMBAT_MARK + (idle >= NEVER_SWUNG ? NEVER : now - idle) + FIELD
                    + (damagedAgo < 0L ? NEVER : now - damagedAgo) + FIELD + fields[2];
        } catch (NumberFormatException ignored) {
            return mark;
        }
    }

    public static void clearMirror() {
        mirroredSwingTick = NEVER;
        mirroredDamagedTick = NEVER;
        mirroredMissed = false;
    }
}
