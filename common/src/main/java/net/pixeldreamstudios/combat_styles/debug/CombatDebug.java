package net.pixeldreamstudios.combat_styles.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatDebug {

    private static final String PREFIX = "[CS] ";
    private static final int REPEAT_WINDOW = 20;

    private static final Set<UUID> WATCHING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Map<String, Long>> RECENT = new ConcurrentHashMap<>();

    private CombatDebug() {
    }

    public static String slot(int index, int size) {
        return index < 0 ? "-/" + size : (index + 1) + "/" + size;
    }

    public static String slot(int index) {
        return index < 0 ? "-" : String.valueOf(index + 1);
    }

    public static boolean enabled(Player player) {
        return player != null
                && (CombatStylesConfig.get().debugLogging()
                        || (!WATCHING.isEmpty() && WATCHING.contains(player.getUUID())));
    }

    public static boolean toggle(Player player, boolean on) {
        if (player == null) {
            return false;
        }
        if (on) {
            WATCHING.add(player.getUUID());
        } else {
            WATCHING.remove(player.getUUID());
        }
        return on;
    }

    public static void forget(UUID playerId) {
        WATCHING.remove(playerId);
        RECENT.remove(playerId);
    }

    public static void phase(Player player, String phase, String detail) {
        say(player, ChatFormatting.DARK_AQUA, phase + " — " + detail);
    }

    public static void fired(Player player, String what, String detail) {
        say(player, ChatFormatting.GREEN, what + "  " + detail);
    }

    public static void skipped(Player player, String what, String why) {
        say(player, ChatFormatting.RED, what + "  SKIPPED: " + why);
    }

    public static void note(Player player, String detail) {
        say(player, ChatFormatting.GRAY, "  " + detail);
    }

    public static void value(Player player, String what, double before, double after) {
        if (!enabled(player)) {
            return;
        }
        say(player, ChatFormatting.YELLOW, what + "  "
                + round(before) + " → " + round(after)
                + (before > 0.0D ? "  (" + round((after / before - 1.0D) * 100.0D) + "%)" : ""));
    }

    private static String round(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    private static void say(Player player, ChatFormatting colour, String message) {
        if (!enabled(player)) {
            return;
        }
        if (repeated(player, message)) {
            return;
        }
        player.displayClientMessage(Component.literal(PREFIX + message).withStyle(colour), false);
    }

    private static boolean repeated(Player player, String message) {
        long now = player.level().getGameTime();
        Map<String, Long> seen = RECENT.computeIfAbsent(player.getUUID(),
                ignored -> new ConcurrentHashMap<>());
        seen.values().removeIf(tick -> now - tick > REPEAT_WINDOW);
        Long previous = seen.get(message);
        if (previous != null && now - previous <= REPEAT_WINDOW) {
            return true;
        }
        seen.put(message, now);
        return false;
    }

}
