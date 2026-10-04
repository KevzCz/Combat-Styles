package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public enum EventKind implements StringRepresentable {

    ON_HURT("on_hurt", false, EquipmentTrigger::reactsToHurt),
    ON_SHOOT("on_shoot", false, trigger -> trigger.activation().reactsToShoot()),
    ON_SHIELD_BLOCK("on_shield_block", false, trigger -> trigger.activation().reactsToBlock()),
    ON_PROJECTILE_HIT("on_projectile_hit", false,
            trigger -> trigger.activation().reactsToProjectileHit()),
    ON_USE("on_use", false, trigger -> trigger.activation().usesItem()),
    ON_CAST("on_cast", false, trigger -> trigger.activation().reactsToCast()),
    ON_KILL("on_kill", true, trigger -> trigger.onKill().orElse(false)),
    ON_MISS("on_miss", true, trigger -> trigger.onMiss().orElse(false)),
    ON_SUMMON_HIT("on_summon_hit", true, trigger -> trigger.onSummonHit().orElse(false)),
    ON_SUMMON_SPAWN(SummonGate.ON_SPAWN, false,
            trigger -> trigger.activation().summon().hears(SummonEvent.Kind.SPAWN)),
    ON_SUMMON_DEATH(SummonGate.ON_DEATH, false,
            trigger -> trigger.activation().summon().hears(SummonEvent.Kind.DEATH)),
    ON_SUMMON_KILL(SummonGate.ON_KILL, false,
            trigger -> trigger.activation().summon().hears(SummonEvent.Kind.KILL));

    public static final Codec<EventKind> CODEC = Codec.STRING.comapFlatMap(EventKind::parse,
            EventKind::serializedName);

    private static final String LABEL_PREFIX = "tooltip.combat_styles.event_payload.";

    private final String name;
    private final boolean melee;
    private final Predicate<EquipmentTrigger> heard;

    EventKind(String name, boolean melee, Predicate<EquipmentTrigger> heard) {
        this.name = name;
        this.melee = melee;
        this.heard = heard;
    }

    private static DataResult<EventKind> parse(String raw) {
        for (EventKind kind : values()) {
            if (kind.name.equals(raw)) {
                return DataResult.success(kind);
            }
        }
        return DataResult.error(() -> "Unknown event " + raw);
    }

    public String serializedName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String labelKey() {
        return LABEL_PREFIX + name;
    }

    public boolean melee() {
        return melee;
    }

    public boolean heardBy(EquipmentTrigger trigger) {
        return trigger != null && trigger.inTree(heard);
    }

    public static EventKind activationOf(EquipmentContext context) {
        if (context == null) {
            return null;
        }
        if (context.hurt() != null) {
            return ON_HURT;
        }
        if (context.shot() != null) {
            return ON_SHOOT;
        }
        if (context.blocked() != null) {
            return ON_SHIELD_BLOCK;
        }
        if (context.projectileHit() != null) {
            return ON_PROJECTILE_HIT;
        }
        if (context.used() != null) {
            return ON_USE;
        }
        if (context.cast() != null) {
            return ON_CAST;
        }
        if (context.summoned() != null) {
            return switch (context.summoned().kind()) {
                case SPAWN -> ON_SUMMON_SPAWN;
                case DEATH -> ON_SUMMON_DEATH;
                case KILL -> ON_SUMMON_KILL;
            };
        }
        return null;
    }

    public static List<EventKind> meleeOf(EquipmentContext context, boolean onHit,
                                          boolean missPhase) {
        List<EventKind> kinds = new ArrayList<>(1);
        if (context == null) {
            return kinds;
        }
        if (missPhase) {
            kinds.add(ON_MISS);
            return kinds;
        }
        if (!onHit) {
            return kinds;
        }
        if (context.summonHit()) {
            kinds.add(ON_SUMMON_HIT);
        } else if (context.killed()) {
            kinds.add(ON_KILL);
        }
        return kinds;
    }

    public static List<EventKind> activationHeardBy(EquipmentTrigger trigger) {
        List<EventKind> kinds = new ArrayList<>();
        for (EventKind kind : values()) {
            if (!kind.melee && kind.heardBy(trigger)) {
                kinds.add(kind);
            }
        }
        return kinds;
    }
}
