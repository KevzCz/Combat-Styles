package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.world.entity.player.Player;

public final class PoseOwner {

    private static final ThreadLocal<Player> CURRENT = new ThreadLocal<>();

    private PoseOwner() {
    }

    public static void push(Player player) {
        CURRENT.set(player);
    }

    public static void pop() {
        CURRENT.remove();
    }

    public static Player current() {
        return CURRENT.get();
    }
}
