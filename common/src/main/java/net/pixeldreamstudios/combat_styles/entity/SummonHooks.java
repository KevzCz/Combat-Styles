package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SummonHooks {

    @FunctionalInterface
    public interface Dive {
        void dive(Player owner, LivingEntity target, float damage);
    }

    public interface Filtered extends Dive {

        int dive(Player owner, LivingEntity target, float damage,
                 List<String> entities, int limit);

        @Override
        default void dive(Player owner, LivingEntity target, float damage) {
            dive(owner, target, damage, List.of(), 0);
        }
    }

    private static final List<Consumer<UUID>> FORGET = new CopyOnWriteArrayList<>();
    private static final List<Consumer<ServerPlayer>> KEEP_ALIVE = new CopyOnWriteArrayList<>();
    private static final List<Runnable> RELOAD = new CopyOnWriteArrayList<>();
    private static final List<Dive> DIVE = new CopyOnWriteArrayList<>();

    private SummonHooks() {
    }

    public static void onReload(Runnable hook) {
        if (hook != null) {
            RELOAD.add(hook);
        }
    }

    public static void onDive(Dive hook) {
        if (hook != null) {
            DIVE.add(hook);
        }
    }

    public static void reload() {
        for (Runnable hook : RELOAD) {
            hook.run();
        }
    }

    public static void dive(Player owner, LivingEntity target, float damage) {
        dive(owner, target, damage, List.of(), 0);
    }

    public static void dive(Player owner, LivingEntity target, float damage,
                            List<String> entities, int maxDiving) {
        int budget = maxDiving <= 0 ? Integer.MAX_VALUE : maxDiving;
        for (Dive hook : DIVE) {
            if (budget <= 0) {
                return;
            }
            if (hook instanceof Filtered filtered) {
                budget -= filtered.dive(owner, target, damage, entities, budget);
                continue;
            }
            hook.dive(owner, target, damage);
            budget = maxDiving <= 0 ? budget : 0;
        }
    }

    public static void onForget(Consumer<UUID> hook) {
        if (hook != null) {
            FORGET.add(hook);
        }
    }

    public static void onKeepAlive(Consumer<ServerPlayer> hook) {
        if (hook != null) {
            KEEP_ALIVE.add(hook);
        }
    }

    public static void forget(UUID playerId) {
        for (Consumer<UUID> hook : FORGET) {
            hook.accept(playerId);
        }
    }

    public static void keepAlive(ServerPlayer player) {
        for (Consumer<ServerPlayer> hook : KEEP_ALIVE) {
            hook.accept(player);
        }
    }
}
