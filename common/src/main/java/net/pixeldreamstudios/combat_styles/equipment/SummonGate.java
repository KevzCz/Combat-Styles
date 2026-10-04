package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Locale;

public record SummonGate(boolean onSpawn, boolean onDeath, boolean onKill, List<String> entities) {

    public static final String ON_SPAWN = "on_summon_spawn";
    public static final String ON_DEATH = "on_summon_death";
    public static final String ON_KILL = "on_summon_kill";
    public static final String ENTITIES = "summon_entities";

    public static final SummonGate ANY = new SummonGate(false, false, false, List.of());

    public static final MapCodec<SummonGate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf(ON_SPAWN, Boolean.FALSE).forGetter(SummonGate::onSpawn),
            Codec.BOOL.optionalFieldOf(ON_DEATH, Boolean.FALSE).forGetter(SummonGate::onDeath),
            Codec.BOOL.optionalFieldOf(ON_KILL, Boolean.FALSE).forGetter(SummonGate::onKill),
            Codec.STRING.listOf().optionalFieldOf(ENTITIES, List.of()).forGetter(SummonGate::entities)
    ).apply(instance, SummonGate::new));

    public SummonGate {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }

    public boolean isEmpty() {
        return !onSpawn && !onDeath && !onKill;
    }

    public boolean hears(SummonEvent.Kind kind) {
        return switch (kind) {
            case SPAWN -> onSpawn;
            case DEATH -> onDeath;
            case KILL -> onKill;
        };
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        SummonEvent event = context == null ? null : context.summoned();
        if (event == null) {
            return "summon event";
        }
        if (!hears(event.kind())) {
            return "summon event (" + event.kind().name().toLowerCase(Locale.ROOT) + ")";
        }
        if (!entities.isEmpty() && !entities.contains(event.summonId())) {
            return ENTITIES;
        }
        return null;
    }
}
