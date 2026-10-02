package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackShaping;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.InheritGroups;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class PendingExtraHit {

    private static final ThreadLocal<Integer> ACTIVE =
            ThreadLocal.withInitial(() -> SwingDrag.NO_EXTRA);

    private static final Map<UUID, Deque<Integer>> QUEUED = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> STRUCK = new ConcurrentHashMap<>();

    private PendingExtraHit() {
    }

    public static void enqueue(Player player, int hitIndex) {
        if (player == null || hitIndex < 0) {
            return;
        }
        QUEUED.computeIfAbsent(player.getUUID(), key -> new ConcurrentLinkedDeque<>())
                .addLast(hitIndex);
    }

    public static void beginHit(Player player) {
        if (player == null) {
            ACTIVE.set(SwingDrag.NO_EXTRA);
            return;
        }
        Deque<Integer> queue = QUEUED.get(player.getUUID());
        Integer next = queue == null ? null : queue.pollFirst();
        ACTIVE.set(next == null ? SwingDrag.NO_EXTRA : next);
    }

    public static void markStruck(Player player) {
        if (player != null) {
            STRUCK.put(player.getUUID(), player.tickCount);
        }
    }

    public static boolean struck(Player player) {
        if (player == null) {
            return false;
        }
        Integer tick = STRUCK.get(player.getUUID());
        return tick != null && tick == player.tickCount;
    }

    public static void forget(Player player) {
        if (player != null) {
            QUEUED.remove(player.getUUID());
        }
    }

    public static void forget(UUID playerId) {
        QUEUED.remove(playerId);
        STRUCK.remove(playerId);
    }

    public static void push(int hitIndex) {
        ACTIVE.set(hitIndex);
    }

    public static void pop() {
        ACTIVE.set(SwingDrag.NO_EXTRA);
    }

    public static int index() {
        return ACTIVE.get();
    }

    public static List<ExtraAttack> hitsOf(Player player, WeaponAttributes.Attack attack) {
        AttackBehaviour behaviour = AttackShaping.behaviourFor(player, attack);
        List<ExtraAttack> hits = new ArrayList<>(behaviour.extras());
        for (SlashPlacement slash : behaviour.slashes()) {
            if (slash.mode().dealsDamage()) {
                hits.add(slash.asAttack());
            }
        }
        hits.addAll(stagedBoxes(behaviour, AttackShaping.overrideFor(player, attack)));
        return hits;
    }

    public static List<ExtraAttack> stagedBoxes(AttackBehaviour behaviour, AttackOverride parent) {
        HitboxSet boxes = behaviour.boxesOrNone();
        if (!boxes.staggered()) {
            return List.of();
        }
        AttackOverride base = parent == null ? AttackOverride.EMPTY : parent;
        List<ExtraAttack> staged = new ArrayList<>();
        for (int step : boxes.delaySteps()) {
            if (step <= 0) {
                continue;
            }
            HitboxSet group = boxes.atDelay(step);
            if (group.isEmpty()) {
                continue;
            }
            AttackOverride shaped = base.withBehaviour(
                    base.behaviourOrDefault().withBoxes(group));
            staged.add(ExtraAttack.DEFAULT.withDelay(step)
                    .withInherit(InheritGroups.NONE)
                    .withOverride(shaped));
        }
        return staged;
    }

    public static WeaponAttributes.Attack shapedAttack(WeaponAttributes.Attack origin,
                                                       AttackOverride shape) {
        if (origin == null) {
            return null;
        }
        StylePage shaped = AttackOverrides.applyTo(new StylePage("", "", 0, origin, null, null),
                shape);
        return shaped == null ? origin : shaped.attack();
    }

    public static List<String> affixesFor(Player player, WeaponAttributes.Attack attack) {
        int index = index();
        if (index < 0) {
            return List.of();
        }
        List<ExtraAttack> hits = hitsOf(player, attack);
        return index >= hits.size() ? List.of() : hits.get(index).affixes();
    }

    public static Map<String, AffixTuning> tuningFor(Player player,
                                                     WeaponAttributes.Attack attack) {
        int index = index();
        if (index < 0) {
            return Map.of();
        }
        List<ExtraAttack> hits = hitsOf(player, attack);
        return index >= hits.size() ? Map.of() : hits.get(index).tuning();
    }

    public static boolean inheritsAffixes(Player player, WeaponAttributes.Attack attack) {
        int index = index();
        if (index < 0) {
            return true;
        }
        List<ExtraAttack> hits = hitsOf(player, attack);
        return index >= hits.size() || hits.get(index).inheritOrAll().affixes().inherits();
    }

    public static AttackOverride shapeFor(Player player, WeaponAttributes.Attack attack,
                                          AttackOverride parent) {
        int index = index();
        if (index < 0) {
            return null;
        }
        List<ExtraAttack> hits = hitsOf(player, attack);
        if (index >= hits.size()) {
            return null;
        }
        return hits.get(index).shapeFrom(parent);
    }

    public static double ratioOf(AttackOverride shaped, WeaponAttributes.Attack attack) {
        if (shaped == null) {
            return ExtraAttack.DEFAULT_RATIO;
        }
        double base = attack == null ? ExtraAttack.DEFAULT_RATIO : attack.damageMultiplier();
        double ratio = shaped.damageMultiplier().orElse(ExtraAttack.DEFAULT_RATIO);
        if (base <= 0.0D) {
            return ExtraAttack.DEFAULT_RATIO;
        }
        return ratio / base;
    }
}
