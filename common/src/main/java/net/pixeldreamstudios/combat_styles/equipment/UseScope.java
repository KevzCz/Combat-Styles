package net.pixeldreamstudios.combat_styles.equipment;

import com.google.common.collect.MapMaker;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.function.Supplier;

public final class UseScope {

    public static final String PREFIX = "use:";
    public static final String HELD_TICKS = PREFIX + "held_ticks";
    public static final String HELD_SECONDS = PREFIX + "held_seconds";
    public static final String CHARGE = PREFIX + "charge";
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double FULL_CHARGE = 1.0D;
    private static final double NONE = 0.0D;

    private static final Map<Entity, UseEvent> CAPTURED = new MapMaker().weakKeys().makeMap();
    private static final ThreadLocal<UseEvent> SCOPED = new ThreadLocal<>();

    private UseScope() {
    }

    public static boolean isSource(String source) {
        return source != null && source.startsWith(PREFIX);
    }

    public static UseEvent current() {
        UseEvent live = EquipmentActivation.currentUse();
        return live != null ? live : SCOPED.get();
    }

    public static void capture(Entity entity) {
        UseEvent use = current();
        if (entity != null && use != null) {
            CAPTURED.put(entity, use);
        }
    }

    public static void inherit(Entity source, Entity copy) {
        UseEvent use = source == null ? null : CAPTURED.get(source);
        if (copy != null && use != null) {
            CAPTURED.put(copy, use);
        }
    }

    public static <T> T within(Entity entity, Supplier<T> body) {
        UseEvent use = entity == null ? null : CAPTURED.get(entity);
        if (use == null || EquipmentActivation.currentUse() != null) {
            return body.get();
        }
        UseEvent previous = SCOPED.get();
        SCOPED.set(use);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                SCOPED.remove();
            } else {
                SCOPED.set(previous);
            }
        }
    }

    public static void within(Entity entity, Runnable body) {
        within(entity, () -> {
            body.run();
            return null;
        });
    }

    public static double valueOf(String source) {
        UseEvent use = current();
        if (use == null || source == null) {
            return NONE;
        }
        return switch (source) {
            case HELD_TICKS -> use.heldTicks();
            case HELD_SECONDS -> use.heldTicks() / TICKS_PER_SECOND;
            case CHARGE -> chargeOf(use);
            default -> NONE;
        };
    }

    private static double chargeOf(UseEvent use) {
        EquipmentDefinition definition = EquipmentProviders.definitionFor(use.weapon());
        int hold = definition == null ? UseSpec.NO_HOLD : definition.use().holdTicks();
        if (hold <= 0) {
            return use.heldTicks() > 0 ? FULL_CHARGE : NONE;
        }
        return Math.min(FULL_CHARGE, use.heldTicks() / (double) hold);
    }
}
