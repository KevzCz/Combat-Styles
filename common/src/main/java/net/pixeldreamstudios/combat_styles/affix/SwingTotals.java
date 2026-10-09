package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SwingTotals {

    public record DeferredHit(LivingEntity target, float physical, boolean converted) {
    }

    private static final class Tally {
        private final Player player;
        private final Map<LivingEntity, Float> extras = new LinkedHashMap<>();
        private final List<DeferredHit> deferred = new ArrayList<>();

        private Tally(Player player) {
            this.player = player;
        }
    }

    private static final ThreadLocal<Tally> CURRENT = new ThreadLocal<>();

    private SwingTotals() {
    }

    public static void begin(Player player) {
        if (player == null || player.level().isClientSide()) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(new Tally(player));
    }

    public static void end() {
        CURRENT.remove();
    }

    public static void add(Entity attacker, LivingEntity target, float amount) {
        Tally tally = CURRENT.get();
        if (tally == null || attacker != tally.player || target == null || amount <= 0.0F) {
            return;
        }
        tally.extras.merge(target, amount, Float::sum);
    }

    public static boolean defer(Player player, LivingEntity target, float physical, boolean converted) {
        Tally tally = CURRENT.get();
        if (tally == null || tally.player != player || target == null) {
            return false;
        }
        tally.deferred.add(new DeferredHit(target, physical, converted));
        return true;
    }

    public static float extrasOn(LivingEntity target) {
        Tally tally = CURRENT.get();
        return tally == null ? 0.0F : tally.extras.getOrDefault(target, 0.0F);
    }

    public static List<DeferredHit> takeDeferred() {
        Tally tally = CURRENT.get();
        if (tally == null || tally.deferred.isEmpty()) {
            return List.of();
        }
        List<DeferredHit> taken = List.copyOf(tally.deferred);
        tally.deferred.clear();
        return taken;
    }
}
