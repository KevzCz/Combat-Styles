package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EventPayloads(Map<EventKind, List<AffixEffects.OnHit>> payloads) {

    public static final String KEY = "events";

    private static final Codec<List<AffixEffects.OnHit>> ONE_OR_MANY = Codec.either(
                    AffixEffects.OnHit.CODEC, AffixEffects.OnHit.CODEC.listOf())
            .xmap(either -> either.map(List::of, list -> list),
                    list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list));

    public static final Codec<EventPayloads> CODEC = Customs.wrap(EventPayloads.class, Codec.unboundedMap(EventKind.CODEC, ONE_OR_MANY)
            .xmap(EventPayloads::new, EventPayloads::payloads));

    public static final EventPayloads EMPTY = new EventPayloads(Map.of());

    public EventPayloads {
        Map<EventKind, List<AffixEffects.OnHit>> kept = new EnumMap<>(EventKind.class);
        if (payloads != null) {
            for (Map.Entry<EventKind, List<AffixEffects.OnHit>> entry : payloads.entrySet()) {
                List<AffixEffects.OnHit> list = entry.getValue() == null
                        ? List.of()
                        : entry.getValue().stream()
                                .filter(payload -> payload != null
                                        && (!payload.equals(AffixEffects.OnHit.EMPTY)
                                                || Customs.has(payload)))
                                .toList();
                if (entry.getKey() != null && !list.isEmpty()) {
                    kept.put(entry.getKey(), list);
                }
            }
        }
        payloads = Map.copyOf(kept);
    }

    public boolean isEmpty() {
        return payloads.isEmpty();
    }

    public boolean has(EventKind kind) {
        return kind != null && payloads.containsKey(kind);
    }

    public List<AffixEffects.OnHit> get(EventKind kind) {
        List<AffixEffects.OnHit> found = kind == null ? null : payloads.get(kind);
        return found == null ? List.of() : found;
    }

    public List<AffixEffects.OnHit> getAll(Collection<EventKind> kinds) {
        List<AffixEffects.OnHit> found = new ArrayList<>();
        for (EventKind kind : kinds) {
            found.addAll(get(kind));
        }
        return found;
    }

    public EventPayloads with(EventKind kind, List<AffixEffects.OnHit> list) {
        Map<EventKind, List<AffixEffects.OnHit>> next = new EnumMap<>(EventKind.class);
        next.putAll(payloads);
        if (list == null || list.isEmpty()) {
            next.remove(kind);
        } else {
            next.put(kind, List.copyOf(list));
        }
        return new EventPayloads(next);
    }

    public EventPayloads mergedWith(EventPayloads other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        Map<EventKind, List<AffixEffects.OnHit>> next = new EnumMap<>(EventKind.class);
        next.putAll(payloads);
        for (Map.Entry<EventKind, List<AffixEffects.OnHit>> entry : other.payloads().entrySet()) {
            next.merge(entry.getKey(), entry.getValue(), (mine, theirs) -> {
                List<AffixEffects.OnHit> joined = new ArrayList<>(mine);
                joined.addAll(theirs);
                return joined;
            });
        }
        return new EventPayloads(next);
    }

    public static TriggeredEffect migrated(TriggeredEffect entry, EquipmentTrigger groupTrigger) {
        EquipmentEffect effect = entry.effect();
        if (effect.onHit().isEmpty() && effect.onSwing().isEmpty()) {
            return entry;
        }
        List<EventKind> kinds = new ArrayList<>(EventKind.activationHeardBy(entry.trigger()));
        if (groupTrigger != null) {
            for (EventKind kind : EventKind.activationHeardBy(groupTrigger)) {
                if (!kinds.contains(kind)) {
                    kinds.add(kind);
                }
            }
        }
        if (kinds.isEmpty()) {
            return entry;
        }
        List<AffixEffects.OnHit> legacy = new ArrayList<>(2);
        effect.onSwing().ifPresent(legacy::add);
        effect.onHit().ifPresent(legacy::add);
        EventPayloads events = effect.events();
        for (EventKind kind : kinds) {
            if (!events.has(kind)) {
                events = events.with(kind, legacy);
            }
        }
        boolean eventOnly = entry.trigger().requiresActivation()
                || (groupTrigger != null && groupTrigger.requiresActivation());
        EquipmentEffect moved = EquipmentEffects.withEvents(effect, events);
        if (eventOnly) {
            moved = EquipmentEffects.withOnHit(EquipmentEffects.withOnSwing(moved,
                    Optional.empty()), Optional.empty());
        }
        return entry.withEffect(moved);
    }
}
