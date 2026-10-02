package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.world.entity.player.Player;

public final class DraggedTrail {

    private static Player pending;

    private DraggedTrail() {
    }

    public static void begin(Player player) {
        pending = player;
    }

    public static void end() {
        pending = null;
    }

    public static Player pendingOwner() {
        return pending;
    }
}
