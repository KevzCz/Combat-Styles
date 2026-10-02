package net.pixeldreamstudios.combat_styles.api.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.entity.ConfigurableSummon;

import java.util.ArrayList;
import java.util.List;

public final class SummonEvents {

    private static final List<Ordered> ORDERED = new ArrayList<>();
    private static final List<Acted> ACTED = new ArrayList<>();
    private static final List<Dismissed> DISMISSED = new ArrayList<>();

    @FunctionalInterface
    public interface Ordered {
        boolean allow(Player owner, ConfigurableSummon summon, LivingEntity target,
                      String ability, float damage);
    }

    @FunctionalInterface
    public interface Acted {
        void onAct(Player owner, ConfigurableSummon summon, String ability, float damage);
    }

    @FunctionalInterface
    public interface Dismissed {
        void onDismiss(Player owner, ConfigurableSummon summon);
    }

    private SummonEvents() {
    }

    public static void onOrdered(Ordered listener) {
        if (listener != null) {
            ORDERED.add(listener);
        }
    }

    public static void onActed(Acted listener) {
        if (listener != null) {
            ACTED.add(listener);
        }
    }

    public static void onDismissed(Dismissed listener) {
        if (listener != null) {
            DISMISSED.add(listener);
        }
    }

    public static boolean allowOrder(Player owner, ConfigurableSummon summon,
                                     LivingEntity target, String ability, float damage) {
        for (Ordered listener : ORDERED) {
            if (!listener.allow(owner, summon, target, ability, damage)) {
                return false;
            }
        }
        return true;
    }

    public static void fireActed(Player owner, ConfigurableSummon summon,
                                 String ability, float damage) {
        for (Acted listener : ACTED) {
            listener.onAct(owner, summon, ability, damage);
        }
    }

    public static void fireDismissed(Player owner, ConfigurableSummon summon) {
        for (Dismissed listener : DISMISSED) {
            listener.onDismiss(owner, summon);
        }
    }

    public static boolean listening() {
        return !ORDERED.isEmpty() || !ACTED.isEmpty() || !DISMISSED.isEmpty();
    }
}
