package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EquipmentRegistry {

    private static final Map<String, EquipmentDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<String, EquipmentDefinition> FROM_DATA = new ConcurrentHashMap<>();
    private static final Map<String, EquipmentDefinition> SYNCED = new ConcurrentHashMap<>();
    private static final Set<String> SYNCED_FROM_DATA = ConcurrentHashMap.newKeySet();
    private static final Set<String> SYNCED_OVERRIDDEN = ConcurrentHashMap.newKeySet();

    private static volatile Map<String, EquipmentDefinition> merged = Map.of();
    private static volatile long generation;

    private EquipmentRegistry() {
    }

    public static long generation() {
        return generation;
    }

    private static void rebuild() {
        Map<String, EquipmentDefinition> combined = new LinkedHashMap<>(SYNCED);
        combined.putAll(DEFINITIONS);
        merged = Collections.unmodifiableMap(combined);
        generation++;
    }

    public static void accept(Map<String, EquipmentDefinition> loaded) {
        DEFINITIONS.clear();
        if (loaded != null) {
            DEFINITIONS.putAll(loaded);
        }
        rebuild();
    }

    public static void acceptSync(Map<String, EquipmentDefinition> synced,
                                  Set<String> fromData, Set<String> overridden) {
        SYNCED_FROM_DATA.clear();
        SYNCED_FROM_DATA.addAll(fromData);
        SYNCED_OVERRIDDEN.clear();
        SYNCED_OVERRIDDEN.addAll(overridden);
        SYNCED.clear();
        if (synced != null) {
            synced.forEach((id, definition) -> {
                if (definition != null) {
                    SYNCED.put(id, definition);
                }
            });
        }
        rebuild();
    }

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_FROM_DATA.clear();
        SYNCED_OVERRIDDEN.clear();
        rebuild();
    }

    public static Set<String> dataIds() {
        return Set.copyOf(FROM_DATA.keySet());
    }

    public static boolean isOverridden(String id) {
        return id != null && (EquipmentOverrides.has(id) || SYNCED_OVERRIDDEN.contains(id));
    }

    public static void acceptLocal(EquipmentDefinition definition) {
        if (definition != null && definition.id() != null) {
            DEFINITIONS.put(definition.id(), definition);
            rebuild();
        }
    }

    public static void forgetLocal(String id) {
        if (id != null) {
            DEFINITIONS.remove(id);
            SYNCED.remove(id);
            rebuild();
        }
    }

    public static void acceptData(Map<String, EquipmentDefinition> data) {
        FROM_DATA.clear();
        if (data != null) {
            FROM_DATA.putAll(data);
        }
    }

    public static boolean fromData(String id) {
        return id != null && (FROM_DATA.containsKey(id) || SYNCED_FROM_DATA.contains(id));
    }

    public static void restoreLocal(String id) {
        if (id == null) {
            return;
        }
        EquipmentDefinition original = FROM_DATA.get(id);
        if (original == null) {
            DEFINITIONS.remove(id);
            SYNCED.remove(id);
        } else {
            DEFINITIONS.put(id, original);
            SYNCED.remove(id);
        }
        rebuild();
    }

    public static Map<String, EquipmentDefinition> snapshot() {
        return Map.copyOf(DEFINITIONS);
    }

    private static Map<String, EquipmentDefinition> merged() {
        return merged;
    }

    public static EquipmentDefinition get(String id) {
        if (id == null) {
            return null;
        }
        EquipmentDefinition local = DEFINITIONS.get(id);
        return local != null ? local : SYNCED.get(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static EquipmentDefinition matching(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isEmpty()) {
            return null;
        }
        for (EquipmentDefinition definition : merged().values()) {
            if (definition.matches(stack)) {
                return definition;
            }
        }
        return null;
    }

    public static List<EquipmentDefinition> all() {
        return new ArrayList<>(merged().values());
    }

    public static boolean isEmpty() {
        return DEFINITIONS.isEmpty() && SYNCED.isEmpty();
    }

    public static int size() {
        return merged().size();
    }
}
