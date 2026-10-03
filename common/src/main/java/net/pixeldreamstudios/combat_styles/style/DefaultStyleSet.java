package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultStyleSet {

    public static final DefaultStyleSet EMPTY = new DefaultStyleSet();

    private final Map<ResourceLocation, StyleSequence> sequences = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> slotLimits = new LinkedHashMap<>();
    private final Set<ResourceLocation> appending = new HashSet<>();
    private final Set<ResourceLocation> tuning = new HashSet<>();
    private final Set<ResourceLocation> sticky = new HashSet<>();
    private final Set<ResourceLocation> locked = new HashSet<>();
    private final Set<ResourceLocation> targets = new HashSet<>();
    private final Map<ResourceLocation, List<SlotTuning>> slotTunings = new HashMap<>();
    private int unresolved;

    private DefaultStyleSet() {
    }

    public static DefaultStyleSet compile(List<DefaultStyleLoader.Definition> definitions) {
        DefaultStyleSet set = new DefaultStyleSet();
        Map<ResourceLocation, Integer> priorities = new HashMap<>();
        List<DefaultStyleLoader.Definition> ordered = new ArrayList<>(definitions);
        ordered.sort(Comparator.comparingInt(DefaultStyleLoader.Definition::priority).reversed());

        for (DefaultStyleLoader.Definition definition : ordered) {
            List<StyleEntry> resolvedEntries = new ArrayList<>(definition.entries().size());
            for (StyleEntry entry : definition.entries()) {
                if (entry.customStyleId().isEmpty()
                        && StyleRegistry.get(entry.styleId()) == null) {
                    set.unresolved++;
                    continue;
                }
                resolvedEntries.add(entry);
            }

            String name = definition.nameKey()
                    .map(key -> Component.translatable(key).getString())
                    .orElse(definition.name());
            StyleSequence sequence = resolvedEntries.isEmpty()
                    ? null
                    : new StyleSequence(name, resolvedEntries);

            for (ResourceLocation itemId : DefaultStyleLoader.targetsOf(definition)) {
                int priority = definition.priority();
                Integer existing = priorities.get(itemId);
                if (existing != null && existing > priority) {
                    continue;
                }
                priorities.put(itemId, priority);
                set.targets.add(itemId);

                if (sequence != null) {
                    set.sequences.put(itemId, sequence);
                    toggle(set.appending, itemId, definition.appends());
                    toggle(set.tuning, itemId, definition.tunes());
                    if (!definition.tunings().isEmpty()) {
                        set.slotTunings.put(itemId, definition.tunings());
                    }
                    toggle(set.sticky, itemId, definition.keepThroughRolls());
                }
                if (definition.slots() > 0) {
                    set.slotLimits.put(itemId, definition.slots());
                }
                if (definition.locked()) {
                    set.locked.add(itemId);
                }
            }
        }
        return set;
    }

    private static void toggle(Set<ResourceLocation> set, ResourceLocation itemId, boolean on) {
        if (on) {
            set.add(itemId);
        } else {
            set.remove(itemId);
        }
    }

    public boolean owns(ResourceLocation itemId) {
        return itemId != null && targets.contains(itemId);
    }

    public Map<ResourceLocation, StyleSequence> sequences() {
        return sequences;
    }

    public Map<ResourceLocation, Integer> slotLimits() {
        return slotLimits;
    }

    public Set<ResourceLocation> appending() {
        return appending;
    }

    public Set<ResourceLocation> tuning() {
        return tuning;
    }

    public Set<ResourceLocation> sticky() {
        return sticky;
    }

    public Set<ResourceLocation> locked() {
        return locked;
    }

    public Set<ResourceLocation> targets() {
        return targets;
    }

    public Map<ResourceLocation, List<SlotTuning>> slotTunings() {
        return slotTunings;
    }

    public int unresolved() {
        return unresolved;
    }
}
