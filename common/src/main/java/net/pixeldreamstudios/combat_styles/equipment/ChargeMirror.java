package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChargeMirror {

    private static final int LOCAL_HOLD_SYNCS = 2;

    public static final String HOLD_MARK = "@hold:";
    public static final String SWING_MARK = "@swing:";
    public static final String ARM_MARK = "@arm:";
    public static final String CHOICE_MARK = "@pick:";
    public static final String WINDOW_MARK = "@win:";
    public static final String COMBAT_MARK = "@combat:";
    public static final String TAGS_MARK = "@tags:";
    public static final String COOLDOWN_MARK = "@cd:";
    public static final String DRAW_MARK = "@draw:";
    public static final String ROLL_MARK = "@roll:";

    private static final Set<String> SPENT = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> SPENT_LOCALLY = new ConcurrentHashMap<>();
    private static final Map<String, Integer> HOLDS = new ConcurrentHashMap<>();

    private ChargeMirror() {
    }

    public static void accept(Set<String> onCooldown) {
        SPENT.clear();
        SPENT.addAll(onCooldown);
        for (String id : Set.copyOf(SPENT_LOCALLY.keySet())) {
            if (onCooldown.contains(id)) {
                SPENT_LOCALLY.remove(id);
                continue;
            }
            SPENT_LOCALLY.computeIfPresent(id, (key, value) -> value <= 1 ? null : value - 1);
        }
    }

    public static void release(String id) {
        SPENT.remove(id);
    }

    public static void acceptHolds(Map<String, Integer> holds) {
        HOLDS.clear();
        HOLDS.putAll(holds);
    }

    private static volatile int swingCount;

    public static void acceptSwingCount(int value) {
        swingCount = value;
    }

    private static volatile WeaponAttributes.Attack swungAttack;

    private static volatile int swungSlot = ClientSwing.NO_SLOT;

    public static void recordSwungAttack(WeaponAttributes.Attack attack) {
        swungAttack = attack;
    }

    private static volatile int sequenceSize;

    public static void recordSwungSlot(int slot, int size) {
        swungSlot = slot;
        sequenceSize = size;
    }

    public static int swungSlot() {
        return swungSlot;
    }

    public static int sequenceSize() {
        return sequenceSize;
    }

    public static WeaponAttributes.Attack swungAttack() {
        return swungAttack;
    }

    public static void advanceSwingCount() {
        swingCount++;
    }

    public static int swingCount() {
        return swingCount;
    }

    public static int holdProgress(String id) {
        Integer played = HOLDS.get(id);
        return played == null ? 0 : played;
    }

    public static String describe(String id) {
        return "spent=" + SPENT.contains(id)
                + " local=" + SPENT_LOCALLY.get(id)
                + " holds=" + HOLDS.get(id)
                + " armed=" + ARMED_PASSES.contains(id);
    }

    private static final Set<String> ARMED_PASSES = ConcurrentHashMap.newKeySet();

    public static void acceptArmed(Set<String> ids) {
        ARMED_PASSES.clear();
        ARMED_PASSES.addAll(ids);
    }

    public static boolean passArmed(String id) {
        return ARMED_PASSES.contains(id);
    }

    public static boolean charged(String id) {
        if (HOLDS.containsKey(id) || ARMED_PASSES.contains(id)) {
            return true;
        }
        return !SPENT.contains(id) && !SPENT_LOCALLY.containsKey(id);
    }

    public static void markSpentLocally(String id) {
        if (id != null) {
            SPENT_LOCALLY.put(id, LOCAL_HOLD_SYNCS);
        }
    }

    public static void clear() {
        SPENT.clear();
        SPENT_LOCALLY.clear();
        HOLDS.clear();
        ARMED_PASSES.clear();
    }
}
