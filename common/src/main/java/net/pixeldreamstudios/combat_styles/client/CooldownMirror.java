package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;

import net.pixeldreamstudios.combat_styles.equipment.ChargeMirror;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.Cooldowns;
import net.pixeldreamstudios.combat_styles.equipment.EffectKeys;
import net.pixeldreamstudios.combat_styles.equipment.Gates;
import net.pixeldreamstudios.combat_styles.equipment.CycleChance;
import net.pixeldreamstudios.combat_styles.equipment.GroupDraw;
import net.pixeldreamstudios.combat_styles.equipment.PlayerTags;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionChoice;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class CooldownMirror {
    private static final int PROC_FLASH_TICKS = 10;

    private static final Map<String, Integer> REMAINING = new LinkedHashMap<>();
    private static final Map<String, Integer> TOTAL = new LinkedHashMap<>();
    private static final Map<String, Integer> PROCS = new LinkedHashMap<>();
    private static final Set<String> ARMED = new LinkedHashSet<>();
    private static final Map<String, Integer> HOLDS = new LinkedHashMap<>();
    private static final Set<String> ARMED_PASSES = new LinkedHashSet<>();
    private static final Map<String, String> CHOICES = new LinkedHashMap<>();
    private static final Set<String> WINDOWS = new LinkedHashSet<>();

    private CooldownMirror() {
    }

    public static void accept(Map<String, Integer> remaining, Map<String, Integer> total,
                              Set<String> armed, String proc) {
        REMAINING.clear();
        REMAINING.putAll(remaining);
        TOTAL.clear();
        TOTAL.putAll(total);
        ARMED.clear();
        HOLDS.clear();
        ARMED_PASSES.clear();
        CHOICES.clear();
        WINDOWS.clear();
        PlayerTags.clearMirror();
        Set<String> cooling = new LinkedHashSet<>();
        Set<String> draws = new LinkedHashSet<>();
        Set<String> rolls = new LinkedHashSet<>();
        for (String entry : armed) {
            if (entry.startsWith(ChargeMirror.COOLDOWN_MARK)) {
                cooling.add(entry);
                continue;
            }
            if (entry.startsWith(ChargeMirror.DRAW_MARK)) {
                draws.add(entry);
                continue;
            }
            if (entry.startsWith(ChargeMirror.ROLL_MARK)) {
                rolls.add(entry);
                continue;
            }
            if (entry.startsWith(ChargeMirror.TAGS_MARK)) {
                PlayerTags.acceptMark(entry);
                continue;
            }
            if (entry.startsWith(ChargeMirror.COMBAT_MARK)) {
                CombatState.acceptMark(entry, clientTime());
                continue;
            }
            if (entry.startsWith(ChargeMirror.WINDOW_MARK)) {
                WINDOWS.add(entry);
                continue;
            }
            if (entry.startsWith(ChargeMirror.SWING_MARK)) {
                try {
                    ChargeMirror.acceptSwingCount(Integer.parseInt(
                            entry.substring(ChargeMirror.SWING_MARK.length())));
                } catch (NumberFormatException ignored) {
                }
                continue;
            }
            if (entry.startsWith(ChargeMirror.ARM_MARK)) {
                ARMED_PASSES.add(entry.substring(ChargeMirror.ARM_MARK.length()));
                continue;
            }
            if (entry.startsWith(ChargeMirror.CHOICE_MARK)) {
                String pair = entry.substring(ChargeMirror.CHOICE_MARK.length());
                int split = pair.indexOf(EffectKeys.SEPARATOR);
                if (split > 0) {
                    CHOICES.put(pair.substring(0, split),
                            pair.substring(split + EffectKeys.SEPARATOR.length()));
                }
                continue;
            }
            int mark = entry.indexOf(ChargeMirror.HOLD_MARK);
            if (mark < 0) {
                ARMED.add(entry);
                continue;
            }
            try {
                HOLDS.put(entry.substring(0, mark), Integer.parseInt(
                        entry.substring(mark + ChargeMirror.HOLD_MARK.length())));
            } catch (NumberFormatException ignored) {
            }
        }
        ChargeMirror.accept(remaining.keySet());
        ChargeMirror.acceptHolds(HOLDS);
        ChargeMirror.acceptArmed(ARMED_PASSES);
        SubstitutionChoice.accept(CHOICES);
        Gates.acceptMarks(WINDOWS, clientTime());
        Cooldowns.acceptMarks(Minecraft.getInstance().player, cooling, clientTime());
        GroupDraw.acceptMarks(draws);
        CycleChance.acceptMarks(rolls);
        if (proc != null && !proc.isEmpty()) {
            PROCS.put(proc, PROC_FLASH_TICKS);
        }
    }

    private static long clientTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    public static void tick() {
        for (String id : Set.copyOf(PROCS.keySet())) {
            PROCS.computeIfPresent(id, (key, value) -> value <= 1 ? null : value - 1);
        }
        for (String id : Set.copyOf(REMAINING.keySet())) {
            Integer left = REMAINING.computeIfPresent(id, (key, value) -> value <= 1 ? null : value - 1);
            if (left == null) {
                ChargeMirror.release(id);
            }
        }
    }

    public static int remaining(String id) {
        Integer ticks = REMAINING.get(id);
        return ticks == null ? 0 : ticks;
    }

    public static float fill(String id) {
        Integer left = REMAINING.get(id);
        Integer whole = TOTAL.get(id);
        if (left == null || whole == null || whole <= 0) {
            return 1.0F;
        }
        return 1.0F - Math.min(1.0F, left / (float) whole);
    }

    public static boolean armed(String id) {
        return ARMED.contains(id);
    }

    public static float procFlash(String id) {
        Integer ticks = PROCS.get(id);
        return ticks == null ? 0.0F : ticks / (float) PROC_FLASH_TICKS;
    }

    public static void clear() {
        REMAINING.clear();
        TOTAL.clear();
        PROCS.clear();
        ARMED.clear();
        HOLDS.clear();
        CHOICES.clear();
        SubstitutionChoice.clear();
        WINDOWS.clear();
        Gates.clearMirror();
        Cooldowns.clearMirror(Minecraft.getInstance().player);
        GroupDraw.clearMirror();
        CycleChance.clearMirror();
        CombatState.clearMirror();
        PlayerTags.clearMirror();
        ChargeMirror.clear();
    }

    public static boolean isEmpty() {
        return REMAINING.isEmpty() && PROCS.isEmpty() && ARMED.isEmpty();
    }

}
