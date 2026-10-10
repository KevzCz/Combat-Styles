package net.pixeldreamstudios.combat_styles.damage;

import com.google.common.collect.MapMaker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.equipment.OwnerScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CritRolls {

    private static final int MAX_REACTION_PASSES = 4;
    private static final Object NO_OWNER = new Object();

    private record Reaction(ItemStack owner, Consumer<AffixEffects.OnHit> reactor,
                            AffixEffects.OnHit payload) {
    }

    private static final class Scope {
        private final Map<Object, Map<String, Boolean>> rolls = new IdentityHashMap<>();
        private final List<Reaction> pending = new ArrayList<>();
        private final Map<Object, Set<String>> reacted = new IdentityHashMap<>();
        private Consumer<AffixEffects.OnHit> reactor;
        private int depth;
    }

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private static final Map<Entity, Scope> CARRIED = new MapMaker().weakKeys().makeMap();

    private CritRolls() {
    }

    public static void firing(Runnable action) {
        if (CURRENT.get() != null) {
            action.run();
            return;
        }
        runIn(new Scope(), action);
    }

    public static void enter() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            scope = new Scope();
            CURRENT.set(scope);
        }
        scope.depth++;
    }

    public static void exit() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            return;
        }
        scope.depth--;
        if (scope.depth > 0) {
            return;
        }
        try {
            drain(scope);
        } finally {
            CURRENT.remove();
        }
    }

    public static Runnable bound(Runnable action) {
        Scope scope = CURRENT.get();
        return scope == null ? action : () -> runIn(scope, action);
    }

    public static <T> T carriedBy(Entity entity, Supplier<T> body) {
        Scope carried = entity == null ? null : CARRIED.get(entity);
        if (carried == null || carried == CURRENT.get()) {
            return body.get();
        }
        List<T> result = new ArrayList<>(1);
        runIn(carried, () -> result.add(body.get()));
        return result.getFirst();
    }

    public static void stamp(Entity entity) {
        Scope current = CURRENT.get();
        if (entity != null && current != null) {
            CARRIED.put(entity, current);
        }
    }

    public static void reacting(Consumer<AffixEffects.OnHit> reactor, Runnable body) {
        Scope scope = CURRENT.get();
        if (scope == null) {
            body.run();
            return;
        }
        Consumer<AffixEffects.OnHit> previous = scope.reactor;
        scope.reactor = reactor;
        try {
            body.run();
        } finally {
            scope.reactor = previous;
        }
    }

    public static boolean outcome(ScaledNumber.Crit crit, BooleanSupplier roll) {
        Scope scope = CURRENT.get();
        if (scope == null) {
            return roll.getAsBoolean();
        }
        String group = crit.group().orElse(null);
        if (group == null) {
            boolean hit = roll.getAsBoolean();
            reactionFor(crit, hit).ifPresent(payload -> queue(scope, payload));
            return hit;
        }
        Object owner = ownerKey();
        Map<String, Boolean> rolled = scope.rolls.computeIfAbsent(owner, ignored -> new HashMap<>());
        Boolean known = rolled.get(group);
        boolean hit = known != null ? known : roll.getAsBoolean();
        if (known == null) {
            rolled.put(group, hit);
        }
        Optional<AffixEffects.OnHit> reaction = reactionFor(crit, hit);
        if (reaction.isPresent() && scope.reactor != null
                && scope.reacted.computeIfAbsent(owner, ignored -> new HashSet<>()).add(group)) {
            queue(scope, reaction.get());
        }
        return hit;
    }

    private static Optional<AffixEffects.OnHit> reactionFor(ScaledNumber.Crit crit, boolean hit) {
        return hit ? crit.onCrit() : crit.onNoCrit();
    }

    private static void queue(Scope scope, AffixEffects.OnHit payload) {
        if (scope.reactor == null) {
            return;
        }
        scope.pending.add(new Reaction(OwnerScope.current(), scope.reactor, payload));
    }

    private static Object ownerKey() {
        ItemStack owner = OwnerScope.current();
        return owner == null ? NO_OWNER : owner;
    }

    private static void runIn(Scope scope, Runnable action) {
        Scope previous = CURRENT.get();
        CURRENT.set(scope);
        try {
            action.run();
            drain(scope);
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static void drain(Scope scope) {
        for (int pass = 0; pass < MAX_REACTION_PASSES && !scope.pending.isEmpty(); pass++) {
            List<Reaction> ready = List.copyOf(scope.pending);
            scope.pending.clear();
            for (Reaction reaction : ready) {
                if (reaction.owner() == null) {
                    reaction.reactor().accept(reaction.payload());
                } else {
                    OwnerScope.run(reaction.owner(), () -> reaction.reactor().accept(reaction.payload()));
                }
            }
        }
        scope.pending.clear();
    }
}
