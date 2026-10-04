package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.EffectChance;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class GroupDraw {

    private static final String SEPARATOR = "|";
    private static final int MARK_FIELDS = 3;
    private static final long STANDING_PASS = 0L;

    private record Drawn(Set<String> effects, int cycle, long pass) {
    }

    private static final Map<String, Drawn> DRAWS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> MIRRORED = new ConcurrentHashMap<>();

    private GroupDraw() {
    }

    public static void forget(UUID playerId) {
        DRAWS.keySet().removeIf(key -> key.startsWith(playerId + SEPARATOR));
    }

    public static void clear() {
        DRAWS.clear();
    }

    public static void retainWorn(Player player, Set<String> wornIds) {
        String prefix = player.getUUID() + SEPARATOR;
        DRAWS.keySet().removeIf(key -> key.startsWith(prefix)
                && !wornIds.contains(itemOf(key.substring(prefix.length()))));
    }

    private static String itemOf(String local) {
        int split = local.indexOf(SEPARATOR);
        return split < 0 ? local : local.substring(0, split);
    }

    public static boolean holds(Player player, EquipmentDefinition definition,
                                String effectKey) {
        return holds(player, definition, 0, effectKey);
    }

    public static boolean holds(Player player, EquipmentDefinition definition, int copy,
                                String effectKey) {
        EffectGroup group = definition.groupOf(effectKey);
        if (group == null) {
            return true;
        }
        String name = nameOf(definition, group);
        if (name == null) {
            return true;
        }
        if (player.level().isClientSide()) {
            if (definition.groupSubstitutes(group)) {
                return effectKey.equals(SubstitutionChoice.of(player, definition.id()));
            }
            Set<String> mirrored = MIRRORED.get(localKey(
                    EffectKeys.ownerOf(definition.id(), copy), name));
            return mirrored != null && mirrored.contains(effectKey);
        }
        return drawFor(player, definition, copy, group, name).contains(effectKey);
    }

    private static String localKey(String itemId, String name) {
        return itemId + SEPARATOR + name;
    }

    private static Set<String> drawFor(Player player, EquipmentDefinition definition, int copy,
                                       EffectGroup group, String name) {
        String owner = EffectKeys.ownerOf(definition.id(), copy);
        String key = player.getUUID() + SEPARATOR + localKey(owner, name);
        boolean perCharge = perCycle(definition, group);
        if (perCharge && EquipmentRuntime.awaitingCycle(player, definition.id())) {
            return Set.of();
        }
        int cycle = EquipmentRuntime.comboCycle(player);
        long pass = perCharge ? STANDING_PASS : passOf(player, definition, owner, group, name);
        Drawn cached = DRAWS.get(key);
        if (cached != null) {
            if (perCharge && cached.cycle() == cycle) {
                return cached.effects();
            }
            if (!perCharge && cached.pass() == pass) {
                return cached.effects();
            }
        }
        Set<String> drawn = roll(player, definition, group);
        DRAWS.put(key, new Drawn(drawn, cycle, pass));
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "groupdraw — " + owner + "#" + name
                    + " drew " + drawn + " of " + group.entries().size()
                    + (perCharge
                            ? " held for combo cycle " + cycle
                            : " held for pass " + pass));
        }
        return drawn;
    }

    public static boolean perCycle(EquipmentDefinition definition, EffectGroup group) {
        return definition.groupSubstitutes(group) && !group.armsOnSwing();
    }

    private static long passOf(Player player, EquipmentDefinition definition, String owner,
                               EffectGroup group, String name) {
        if (group.trigger().arms(group.cooldown()) || definition.groupArmsOnSwing(group)) {
            return Gates.windowOpenedAt(player, GateScope.GROUP, owner, name);
        }
        if (StateEdges.tracked(definition, group)) {
            String edge = StateEdges.groupKey(name);
            return group.cooldown().holdWhileActive()
                    ? StateEdges.liveSince(player, owner, edge)
                    : Gates.windowOpenedAt(player, GateScope.HELD, owner, edge);
        }
        return STANDING_PASS;
    }

    public static void endPass(Player player, String itemId, String groupName) {
        if (player == null || itemId == null || groupName == null) {
            return;
        }
        DRAWS.remove(player.getUUID() + SEPARATOR + localKey(itemId, groupName));
    }

    public static void endPass(Player player, String itemId) {
        if (player == null || itemId == null) {
            return;
        }
        String prefix = player.getUUID() + SEPARATOR;
        DRAWS.keySet().removeIf(key -> key.startsWith(prefix)
                && EffectKeys.ownedBy(itemOf(key.substring(prefix.length())), itemId));
    }

    private static Set<String> roll(Player player, EquipmentDefinition definition,
                                    EffectGroup group) {
        if (!EffectChance.rolls(player, group.chance())
                || EffectChance.rolls(player, group.noneChance())) {
            return new LinkedHashSet<>();
        }
        List<String> alive = new ArrayList<>();
        for (EffectGroup.Entry entry : group.entries()) {
            String effect = entry.effect();
            if (definition.effects().containsKey(effect) && entry.share() > 0.0F) {
                alive.add(effect);
            }
        }
        int wanted = group.picksFor(alive.size(), player.getRandom().nextFloat());
        if (wanted >= alive.size()) {
            return new LinkedHashSet<>(alive);
        }
        Set<String> drawn = new LinkedHashSet<>();
        List<String> pool = new ArrayList<>(alive);
        for (int i = 0; i < wanted && !pool.isEmpty(); i++) {
            drawn.add(takeWeighted(player, pool, group));
        }
        return drawn;
    }

    private static String takeWeighted(Player player, List<String> pool, EffectGroup group) {
        float total = 0.0F;
        for (String effect : pool) {
            total += shareOf(group, effect);
        }
        if (total <= 0.0F) {
            return pool.removeFirst();
        }
        float roll = player.getRandom().nextFloat() * total;
        for (int i = 0; i < pool.size(); i++) {
            roll -= shareOf(group, pool.get(i));
            if (roll < 0.0F) {
                return pool.remove(i);
            }
        }
        return pool.removeLast();
    }

    private static float shareOf(EffectGroup group, String effect) {
        EffectGroup.Entry entry = group.entryFor(effect);
        return entry == null ? EffectGroup.ALWAYS : entry.share();
    }

    private static String nameOf(EquipmentDefinition definition, EffectGroup group) {
        for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
            if (named.getValue() == group) {
                return named.getKey();
            }
        }
        return null;
    }

    public static Set<String> marksFor(Player player) {
        if (player == null || player.level().isClientSide()) {
            return Set.of();
        }
        Set<String> marks = new LinkedHashSet<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
                EffectGroup group = named.getValue();
                if (definition.groupSubstitutes(group)) {
                    continue;
                }
                String local = localKey(worn.owner(), named.getKey());
                Set<String> drawn = drawFor(player, definition, worn.copy(), group,
                        named.getKey());
                if (drawn.isEmpty()) {
                    marks.add(ChargeMirror.DRAW_MARK + local + SEPARATOR);
                    continue;
                }
                for (String effect : drawn) {
                    marks.add(ChargeMirror.DRAW_MARK + local + SEPARATOR + effect);
                }
            }
        }
        return marks;
    }

    public static void acceptMarks(Set<String> marks) {
        MIRRORED.clear();
        for (String mark : marks) {
            String[] fields = mark.substring(ChargeMirror.DRAW_MARK.length())
                    .split(Pattern.quote(SEPARATOR), MARK_FIELDS);
            if (fields.length < MARK_FIELDS) {
                continue;
            }
            Set<String> drawn = MIRRORED.computeIfAbsent(localKey(fields[0], fields[1]),
                    ignored -> new HashSet<>());
            if (!fields[2].isEmpty()) {
                drawn.add(fields[2]);
            }
        }
    }

    public static void clearMirror() {
        MIRRORED.clear();
    }
}
