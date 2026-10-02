package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.EffectChance;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CycleChance {

    private static final String SEPARATOR = "|";

    private record Roll(long epoch, boolean passed) {
    }

    private static final Map<String, Roll> ROLLS = new ConcurrentHashMap<>();
    private static final Set<String> MIRRORED = ConcurrentHashMap.newKeySet();

    private CycleChance() {
    }

    public static boolean applies(EquipmentDefinition definition, String effectKey,
                                  TriggeredEffect entry) {
        return entry.trigger().rollsChance() && definition.groupOf(effectKey) == null
                && !entry.trigger().isEvent() && !entry.cooldown().windowed()
                && (entry.effect().hasAlwaysOnParts() || entry.effect().rewritesAttack());
    }

    public static boolean passes(Player player, EquipmentDefinition definition, String effectKey,
                                 TriggeredEffect entry) {
        if (player == null || !applies(definition, effectKey, entry)) {
            return true;
        }
        String local = EffectKeys.of(definition.id(), effectKey);
        if (player.level().isClientSide()) {
            return MIRRORED.contains(local);
        }
        String key = player.getUUID() + SEPARATOR + local;
        long epoch = epochOf(player, definition, effectKey, entry);
        Roll held = ROLLS.get(key);
        if (held != null && held.epoch() == epoch) {
            return held.passed();
        }
        boolean passed = EffectChance.rolls(player,
                entry.trigger().chance().orElse(EffectChance.ALWAYS));
        ROLLS.put(key, new Roll(epoch, passed));
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "cycle chance — " + local + (passed ? " passed" : " failed")
                    + " until " + (stateDriven(entry) ? "the state turns off" : "the combo cycle ends"));
        }
        return passed;
    }

    private static boolean stateDriven(TriggeredEffect entry) {
        return !entry.trigger().swingDriven() && StateEdges.tracked(entry);
    }

    private static long epochOf(Player player, EquipmentDefinition definition, String effectKey,
                                TriggeredEffect entry) {
        return stateDriven(entry)
                ? StateEdges.liveSince(player, definition.id(), effectKey)
                : EquipmentRuntime.comboCycle(player);
    }

    public static Set<String> marksFor(Player player) {
        if (player == null || player.level().isClientSide()) {
            return Set.of();
        }
        Set<String> marks = new LinkedHashSet<>();
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null) {
                continue;
            }
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                if (applies(definition, named.getKey(), named.getValue())
                        && passes(player, definition, named.getKey(), named.getValue())) {
                    marks.add(ChargeMirror.ROLL_MARK + EffectKeys.of(definition.id(), named.getKey()));
                }
            }
        }
        return marks;
    }

    public static void acceptMarks(Set<String> marks) {
        MIRRORED.clear();
        for (String mark : marks) {
            MIRRORED.add(mark.substring(ChargeMirror.ROLL_MARK.length()));
        }
    }

    public static void clearMirror() {
        MIRRORED.clear();
    }

    public static void forget(UUID playerId) {
        ROLLS.keySet().removeIf(key -> key.startsWith(playerId + SEPARATOR));
    }
}
