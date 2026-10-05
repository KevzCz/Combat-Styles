package net.pixeldreamstudios.combat_styles.equipment;

import java.util.stream.IntStream;

public final class ComboCycle {

    public static final int NOT_LIVE = -1;

    private ComboCycle() {
    }

    public static int slotFor(int comboCount, int size) {
        return size <= 0 ? 0 : Math.floorMod(comboCount, size);
    }

    public static int[] all(int size) {
        return IntStream.range(0, Math.max(0, size)).toArray();
    }

    public static int slotOf(int[] live, int comboCount, int size) {
        if (live == null || live.length == 0 || live.length == size) {
            return slotFor(comboCount, size);
        }
        return live[slotFor(comboCount, live.length)];
    }

    public static int positionOf(int[] live, int slot) {
        if (live != null) {
            for (int position = 0; position < live.length; position++) {
                if (live[position] == slot) {
                    return position;
                }
            }
        }
        return NOT_LIVE;
    }
}
