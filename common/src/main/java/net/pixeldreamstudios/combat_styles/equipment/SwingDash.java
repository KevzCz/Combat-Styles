package net.pixeldreamstudios.combat_styles.equipment;
import net.bettercombat.api.AttackHand;
import net.pixeldreamstudios.combat_styles.affix.AffixDamageDealer;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.mixin.AttackStrengthAccessor;
import net.pixeldreamstudios.combat_styles.style.AttackDrag;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.SwingHolder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SwingDash {

    private static final class Drag {
        private final AttackDrag attack;
        private final UUID owner;
        private final Vec3 origin;
        private final int combo;
        private final int extraIndex;
        private final long armedAtSwing;
        private final Item weapon;
        private AttackHand hand;
        private Vec3 lastSample;
        private final Set<Entity> dragged = new LinkedHashSet<>();
        private int ticksLeft;
        private double travelled;

        private Drag(AttackDrag attack, UUID owner, Vec3 origin, int combo,
                     int extraIndex, long armedAtSwing, int ticksLeft, Item weapon) {
            this.weapon = weapon;
            this.attack = attack;
            this.owner = owner;
            this.origin = origin;
            this.combo = combo;
            this.extraIndex = extraIndex;
            this.armedAtSwing = armedAtSwing;
            this.ticksLeft = ticksLeft;
        }

        private boolean ownedBy(Player player) {
            return player != null && player.getUUID().equals(owner) && stillHeld(player);
        }

        private boolean stillHeld(Player player) {
            return weapon == null || player.getMainHandItem().is(weapon);
        }

        private boolean full() {
            return dragged.size() >= attack.maxTargets();
        }
    }

    private static final int MAX_CONCURRENT_DRAGS = 8;

    private static final List<Drag> ACTIVE = new ArrayList<>();

    private static long swings;
    private static int lastArmedCombo = Integer.MIN_VALUE;
    private static UUID lastArmedOwner;
    private static long lastArmedSwings = -1L;

    private SwingDash() {
    }

    public static void atFullStrength(Player player, Runnable action) {
        if (player == null) {
            action.run();
            return;
        }
        AttackStrengthAccessor accessor = (AttackStrengthAccessor) player;
        int previous = accessor.combat_styles$attackStrengthTicker();
        accessor.combat_styles$setAttackStrengthTicker(
                (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()) + 1);
        try {
            action.run();
        } finally {
            accessor.combat_styles$setAttackStrengthTicker(previous);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static AttackDrag active() {
        return ACTIVE.isEmpty() ? AttackDrag.NONE : ACTIVE.getLast().attack;
    }

    public static double travelled() {
        return ACTIVE.isEmpty() ? 0.0D : ACTIVE.getLast().travelled;
    }

    public static Vec3 advanceSample(Player player) {
        for (Drag drag : ACTIVE) {
            if (!drag.ownedBy(player) || drag.ticksLeft <= 0) {
                continue;
            }
            Vec3 from = drag.lastSample == null ? drag.origin : drag.lastSample;
            drag.lastSample = player.position();
            return from;
        }
        return null;
    }

    public static AttackHand armedHand(Player player) {
        for (Drag drag : ACTIVE) {
            if (drag.ownedBy(player) && drag.ticksLeft > 0 && drag.hand != null) {
                return drag.hand;
            }
        }
        return null;
    }

    public static void rememberHand(Player player, AttackHand hand) {
        if (hand == null) {
            return;
        }
        for (Drag drag : ACTIVE) {
            if (drag.ownedBy(player) && drag.hand == null) {
                drag.hand = hand;
            }
        }
    }

    public static int ticksLeft() {
        return ACTIVE.isEmpty() ? 0 : ACTIVE.getLast().ticksLeft;
    }

    public static int armedCombo() {
        return ACTIVE.isEmpty() ? -1 : ACTIVE.getLast().combo;
    }

    public static int armedExtra() {
        return ACTIVE.isEmpty() ? SwingDrag.NO_EXTRA : ACTIVE.getLast().extraIndex;
    }

    private static final List<Entity> STRUCK = new ArrayList<>();

    public static void rememberStruck(List<? extends Entity> targets) {
        STRUCK.clear();
        if (targets != null) {
            STRUCK.addAll(targets);
        }
    }

    public static List<Entity> newlyDragged(Player player, List<? extends Entity> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        int skippedOwner = 0;
        int skippedExpired = 0;
        int skippedFull = 0;
        int skippedSeen = 0;
        List<Entity> fresh = new ArrayList<>();
        for (Drag drag : ACTIVE) {
            if (!drag.ownedBy(player)) {
                skippedOwner++;
                continue;
            }
            if (drag.ticksLeft <= 0) {
                skippedExpired++;
                continue;
            }
            for (Entity entity : targets) {
                if (drag.full()) {
                    skippedFull++;
                    break;
                }
                if (drag.dragged.add(entity)) {
                    if (!fresh.contains(entity)) {
                        fresh.add(entity);
                    }
                } else {
                    skippedSeen++;
                }
            }
        }
        if (fresh.isEmpty() && CombatDebug.enabled(player)) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "drag miss — targets " + targets.size()
                        + " active " + ACTIVE.size()
                        + " skipped owner=" + skippedOwner
                        + " expired=" + skippedExpired
                        + " full=" + skippedFull
                        + " seen=" + skippedSeen
                        + " swings=" + swings);
            }
        }
        return fresh;
    }

    public static List<Entity> merge(Player player, List<Entity> targets) {
        if (targets == null) {
            return targets;
        }
        for (Drag drag : ACTIVE) {
            if (!drag.ownedBy(player)) {
                continue;
            }
            for (Entity entity : targets) {
                if (drag.full()) {
                    break;
                }
                drag.dragged.add(entity);
            }
        }
        return targets;
    }

    public static void tick(Player player) {
        if (player == null || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Drag> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Drag drag = iterator.next();
            if (player == null || !player.getUUID().equals(drag.owner)) {
                continue;
            }
            if (!drag.stillHeld(player)) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "drag DROPPED — combo " + drag.combo
                            + " weapon swapped mid-drag");
                }
                iterator.remove();
                continue;
            }
            if (drag.origin != null) {
                drag.travelled = Math.max(drag.travelled,
                        drag.origin.distanceTo(player.position()));
            }
            if (--drag.ticksLeft <= 0) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "drag EXPIRED — combo " + drag.combo
                            + " travelled " + String.format("%.2f", drag.travelled)
                            + " dragged " + drag.dragged.size()
                            + "/" + drag.attack.maxTargets());
                }
                iterator.remove();
            }
        }
    }

    public static boolean sweeping(Player player) {
        for (Drag drag : ACTIVE) {
            if (drag.ownedBy(player) && drag.ticksLeft > 0) {
                return true;
            }
        }
        return false;
    }

    public static void markSwung() {
        swings++;
    }

    public static String describe(Player player) {
        StringBuilder out = new StringBuilder();
        for (Drag drag : ACTIVE) {
            if (!drag.ownedBy(player)) {
                continue;
            }
            out.append(out.isEmpty() ? "" : ", ")
                    .append("combo ").append(drag.combo)
                    .append(" ticks ").append(drag.ticksLeft)
                    .append(" armedAt ").append(drag.armedAtSwing)
                    .append(" dragged ").append(drag.dragged.size())
                    .append(" hand ").append(drag.hand == null ? "-"
                            : drag.hand.attack().animation());
        }
        return out.isEmpty() ? "none" : out.toString();
    }

    public static boolean trailing(Player player) {
        for (Drag drag : ACTIVE) {
            if (drag.ownedBy(player) && drag.attack.trail()) {
                return true;
            }
        }
        return false;
    }

    public static void armExtra(Player player, int comboCount, int extraIndex,
                                AttackDrag drag, boolean moves,
                                List<? extends Entity> struckNow) {
        if (player == null || !player.level().isClientSide()
                || drag == null || !drag.enabled()) {
            return;
        }
        int bonus = moves ? CombatStylesConfig.get().motionDragWindowBonus() : 0;
        Drag armed = new Drag(drag, player.getUUID(), player.position(), comboCount,
                extraIndex, swings, drag.ticks() + bonus,
                player.getMainHandItem().getItem());
        for (Entity struck : STRUCK) {
            if (armed.full()) {
                break;
            }
            armed.dragged.add(struck);
        }
        if (struckNow != null) {
            for (Entity struck : struckNow) {
                if (armed.full()) {
                    break;
                }
                armed.dragged.add(struck);
            }
        }
        if (ACTIVE.size() >= MAX_CONCURRENT_DRAGS) {
            ACTIVE.removeFirst();
        }
        ACTIVE.add(armed);
        if (CombatDebug.enabled(player)) {
            CombatDebug.fired(player, "drag ARMED (extra)", "ticks=" + armed.ticksLeft
                    + " maxTargets=" + drag.maxTargets() + " combo=" + comboCount
                    + " active=" + ACTIVE.size());
        }
    }

    public static void onUpswing(Player player, int comboCount) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        if (comboCount == lastArmedCombo && swings == lastArmedSwings
                && player.getUUID().equals(lastArmedOwner)) {
            return;
        }
        ItemStack weapon = AffixDamageDealer.swingingWeapon(player, comboCount);
        if (weapon.isEmpty()) {
            CombatDebug.skipped(player, "drag", "no weapon");
            return;
        }
        AttackDrag drag = dragFor(player, weapon, comboCount);
        if (!drag.enabled()) {
            StyleSequence seq;
            SwingHolder.push(player);
            try {
                seq = SequenceService.sequenceOf(weapon, player.getUUID());
            } finally {
                SwingHolder.pop();
            }
            int slots = seq.entries().size();
            if (slots > 0) {
                int slot = SequenceSlots.slotFor(
                        SequenceSlots.sequenceComboOf(player, comboCount), slots);
                AttackOverride bySlot = seq.entries().get(slot).attackOverride().orElse(null);
                if (bySlot == null || !bySlot.dragOrDefault().enabled()) {
                    bySlot = SequenceService.attackOverridesOf(weapon)
                            .get(String.valueOf(slot));
                }
                if (bySlot != null && bySlot.dragOrDefault().enabled()) {
                    drag = bySlot.dragOrDefault();
                }
            }
        }
        boolean moves = AffixDamageDealer.swingMoves(player, weapon, comboCount);
        if (!drag.enabled() && !moves) {
            StyleSequence sequence = SequenceService.sequenceOf(weapon, player.getUUID());
            int slots = sequence.entries().size();
            int seqCombo = SequenceSlots.sequenceComboOf(player, comboCount);
            if (CombatDebug.enabled(player)) {
                CombatDebug.skipped(player, "drag", "no drag on slot "
                        + CombatDebug.slot(SequenceSlots.slotFor(seqCombo, slots), slots)
                        + " (combo=" + comboCount + " seqCombo=" + seqCombo
                        + " slots=" + slots
                        + " keys=" + SequenceService.attackOverridesOf(weapon).keySet() + ")");
            }
            return;
        }
        int bonus = moves ? CombatStylesConfig.get().motionDragWindowBonus() : 0;
        AttackDrag attack = drag.enabled() ? drag : AttackDrag.ENABLED;
        Drag armed = new Drag(attack, player.getUUID(), player.position(), comboCount,
                SwingDrag.NO_EXTRA, swings, attack.ticks() + bonus,
                player.getMainHandItem().getItem());
        for (Entity struck : STRUCK) {
            if (armed.full()) {
                break;
            }
            armed.dragged.add(struck);
        }
        if (ACTIVE.size() >= MAX_CONCURRENT_DRAGS) {
            ACTIVE.removeFirst();
        }
        ACTIVE.add(armed);
        lastArmedCombo = comboCount;
        lastArmedOwner = player.getUUID();
        lastArmedSwings = swings;
        if (CombatDebug.enabled(player)) {
            CombatDebug.fired(player, "drag ARMED", "ticks=" + armed.ticksLeft
                    + (bonus > 0 ? " (+" + bonus + " motion)" : "")
                    + " maxTargets=" + attack.maxTargets() + " combo=" + comboCount
                    + " active=" + ACTIVE.size());
        }
    }

    public static AttackDrag dragFor(Player player, ItemStack weapon, int comboCount) {
        AttackOverride override = overrideFor(player, weapon, comboCount);
        if (CombatDebug.enabled(player)) {
            StyleSequence seq;
            SwingHolder.push(player);
            try {
                seq = SequenceService.sequenceOf(weapon, player.getUUID());
            } finally {
                SwingHolder.pop();
            }
            int slots = seq.entries().size();
            int slot = slots <= 0 ? -1 : SequenceSlots.slotFor(
                    SequenceSlots.sequenceComboOf(player, comboCount), slots);
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "dragFor — combo " + comboCount
                        + " slot " + slot + "/" + slots
                        + " seq " + seq.name()
                        + " side " + (player.level().isClientSide() ? "client" : "server")
                        + " entry " + (slot < 0 ? "-" : seq.entries().get(slot).styleId())
                        + " override " + (override == null ? "null"
                                : "drag=" + override.dragOrDefault().enabled()
                                        + " dash=" + override.behaviour()
                                                .map(value -> value.motion().isPresent())
                                                .orElse(false)));
            }
        }
        return override == null ? AttackDrag.NONE : override.dragOrDefault();
    }

    private static AttackOverride overrideFor(Player player, ItemStack weapon, int comboCount) {
        StyleSequence sequence;
        SwingHolder.push(player);
        try {
            sequence = SequenceService.sequenceOf(weapon, player.getUUID());
        } finally {
            SwingHolder.pop();
        }
        if (sequence.entries().isEmpty()) {
            return null;
        }
        return AffixDamageDealer.attackOverrideFor(player, comboCount);
    }
}
