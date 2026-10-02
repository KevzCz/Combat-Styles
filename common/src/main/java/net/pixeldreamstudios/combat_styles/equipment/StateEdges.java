package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StateEdges {

    private static final long NOT_LIVE = -1L;

    private static final Map<UUID, Set<String>> LIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> SINCE = new ConcurrentHashMap<>();

    private StateEdges() {
    }

    public static void forget(UUID playerId) {
        LIVE.remove(playerId);
        SINCE.remove(playerId);
    }

    public static String groupKey(String groupName) {
        return GateScope.GROUP.prefix() + groupName;
    }

    private static boolean tracked(EquipmentTrigger trigger, EffectCooldown cooldown,
                                   boolean alwaysOn) {
        return alwaysOn && !trigger.isAlways() && !trigger.needsArmingAtRest()
                && !trigger.arms(cooldown);
    }

    public static boolean tracked(TriggeredEffect entry) {
        return tracked(entry.trigger(), entry.cooldown(), entry.effect().hasAlwaysOnParts())
                && !entry.armsOnSwing();
    }

    public static boolean tracked(EquipmentDefinition definition, EffectGroup group) {
        return tracked(group.trigger(), group.cooldown(), definition.groupAlwaysOn(group))
                && !definition.groupArmsOnSwing(group);
    }

    public static boolean held(Player player, EquipmentDefinition definition, String effectKey,
                               TriggeredEffect entry, int swingCount, boolean swingGates) {
        return held(player, definition, effectKey, entry.trigger(), entry.cooldown(),
                swingCount, swingGates);
    }

    public static boolean held(Player player, EquipmentDefinition definition, String groupName,
                               EffectGroup group, int swingCount, boolean swingGates) {
        return held(player, definition, groupKey(groupName), group.trigger(), group.cooldown(),
                swingCount, swingGates);
    }

    private static boolean held(Player player, EquipmentDefinition definition, String name,
                                EquipmentTrigger trigger, EffectCooldown cooldown,
                                int swingCount, boolean swingGates) {
        if (!cooldown.holdWhileActive()) {
            return Gates.windowOpen(player, GateScope.HELD, definition.id(), name);
        }
        if (Gates.windowOpen(player, GateScope.LOCKOUT, definition.id(), name)) {
            return false;
        }
        return trigger.readyAtRest(player, swingCount, definition, swingGates);
    }

    public static int remaining(Player player, String itemId, String name) {
        return Math.max(Gates.windowRemaining(player, GateScope.LOCKOUT, itemId, name),
                Gates.remaining(player, GateScope.HELD, itemId, name));
    }

    public static long liveSince(Player player, String itemId, String name) {
        Map<String, Long> since = player == null ? null : SINCE.get(player.getUUID());
        if (since == null) {
            return NOT_LIVE;
        }
        return since.getOrDefault(EffectKeys.of(itemId, name), NOT_LIVE);
    }

    public static void tick(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        int swings = EquipmentRuntime.swingCount(player);
        Set<String> previous = LIVE.getOrDefault(player.getUUID(), Set.of());
        Set<String> current = ConcurrentHashMap.newKeySet();
        Set<String> worn = new HashSet<>();
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null) {
                continue;
            }
            worn.add(definition.id());
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                if (tracked(entry)) {
                    step(player, definition, named.getKey(), entry.trigger(), entry.cooldown(),
                            swings, !EquipmentDefinition.substitutes(entry.effect()),
                            previous, current);
                }
            }
            for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
                EffectGroup group = named.getValue();
                if (tracked(definition, group)) {
                    step(player, definition, groupKey(named.getKey()), group.trigger(),
                            group.cooldown(), swings, !definition.groupSubstitutes(group),
                            previous, current);
                }
            }
        }
        GroupDraw.retainWorn(player, worn);
        Map<String, Long> since = SINCE.computeIfAbsent(player.getUUID(),
                key -> new ConcurrentHashMap<>());
        since.keySet().retainAll(current);
        for (String mark : current) {
            if (!previous.contains(mark)) {
                since.put(mark, player.level().getGameTime());
            }
        }
        if (current.isEmpty()) {
            LIVE.remove(player.getUUID());
            SINCE.remove(player.getUUID());
        } else {
            LIVE.put(player.getUUID(), current);
        }
    }

    private static void step(Player player, EquipmentDefinition definition, String name,
                             EquipmentTrigger trigger, EffectCooldown cooldown, int swings,
                             boolean swingGates, Set<String> previous, Set<String> current) {
        String mark = EffectKeys.of(definition.id(), name);
        boolean was = previous.contains(mark);
        if (cooldown.holdWhileActive()) {
            if (held(player, definition, name, trigger, cooldown, swings, swingGates)) {
                current.add(mark);
            } else if (was) {
                Gates.lockOut(player, GateScope.LOCKOUT, definition.id(), name, cooldown.ticks());
            }
            return;
        }
        if (trigger.readyAtRest(player, swings, definition, swingGates)) {
            current.add(mark);
            if (!was && cooldown.windowed()) {
                Gates.arm(player, GateScope.HELD, definition.id(), name, cooldown);
            }
        }
    }
}
