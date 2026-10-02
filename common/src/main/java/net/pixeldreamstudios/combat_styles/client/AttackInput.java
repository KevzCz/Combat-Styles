package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;

public final class AttackInput {

    private static final int HOLD_GRACE_TICKS = 2;

    private static int clickedAt = Integer.MIN_VALUE;
    private static int heldAt = Integer.MIN_VALUE;
    private static int lastUpswingTick = Integer.MIN_VALUE;

    private AttackInput() {
    }

    public static void markClick() {
        clickedAt = tick();
    }

    public static void markHold() {
        heldAt = tick();
    }

    public static boolean holding() {
        return heldAt != Integer.MIN_VALUE && tick() - heldAt <= HOLD_GRACE_TICKS;
    }

    public static String label() {
        if (holding()) {
            return "HOLD";
        }
        return clickedAt != Integer.MIN_VALUE && tick() - clickedAt <= HOLD_GRACE_TICKS
                ? "CLICK"
                : "IDLE";
    }

    public static boolean claimUpswing() {
        int now = tick();
        if (now == lastUpswingTick) {
            return false;
        }
        lastUpswingTick = now;
        return true;
    }

    private static int tick() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? Integer.MIN_VALUE : minecraft.player.tickCount;
    }
}
