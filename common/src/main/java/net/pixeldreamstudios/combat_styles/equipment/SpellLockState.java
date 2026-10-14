package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.compat.SpellParentLoader;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SyncSpellLocksPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellLockState {

    private static final String NAMESPACE_SEPARATOR = ":";

    public record Lock(SpellLocks.Mode mode, String item) {
    }

    public record State(List<String> grants, Map<String, Lock> spells, Map<String, Lock> schools,
                        Map<String, SpellTuning> tunes, Map<String, String> inherits) {

        public static final State EMPTY = new State(List.of(), Map.of(), Map.of(), Map.of(), Map.of());

        public State {
            grants = List.copyOf(grants);
            spells = Map.copyOf(spells);
            schools = Map.copyOf(schools);
            tunes = Map.copyOf(tunes);
            inherits = Map.copyOf(inherits);
        }
    }

    private record Tracked(State state, int version) {
    }

    private static final Map<UUID, Tracked> SERVER = new ConcurrentHashMap<>();
    private static volatile State mirrored = State.EMPTY;
    private static volatile int mirroredVersion;

    private SpellLockState() {
    }

    public static State stateOf(Player player) {
        if (player == null) {
            return State.EMPTY;
        }
        if (player.level().isClientSide()) {
            return mirrored;
        }
        Tracked tracked = SERVER.get(player.getUUID());
        return tracked == null ? State.EMPTY : tracked.state();
    }

    public static int versionOf(Player player) {
        if (player == null) {
            return 0;
        }
        if (player.level().isClientSide()) {
            return mirroredVersion;
        }
        Tracked tracked = SERVER.get(player.getUUID());
        return tracked == null ? 0 : tracked.version();
    }

    public static List<String> grantsFor(Player player) {
        return stateOf(player).grants();
    }

    public static Map<String, SpellTuning> tunesFor(Player player) {
        return stateOf(player).tunes();
    }

    public static Map<String, String> inheritsFor(Player player) {
        return stateOf(player).inherits();
    }

    public static float tunedChance(Player player, String spellId, float base) {
        SpellTuning tuning = player == null || spellId == null ? null : tunesFor(player).get(spellId);
        return tuning == null ? base : tuning.chanceFrom(base);
    }

    public static Lock lockFor(Player player, String spellId, String school) {
        State state = stateOf(player);
        Lock bySpell = spellId == null ? null : state.spells().get(spellId);
        if (bySpell != null) {
            return bySpell;
        }
        return school == null ? null : state.schools().get(schoolKey(school));
    }

    public static String schoolKey(String raw) {
        if (raw == null) {
            return "";
        }
        int split = raw.indexOf(NAMESPACE_SEPARATOR);
        return split < 0 ? raw : raw.substring(split + NAMESPACE_SEPARATOR.length());
    }

    public static void update(ServerPlayer player) {
        State computed = compute(player);
        Tracked current = SERVER.get(player.getUUID());
        if (current != null && current.state().equals(computed)) {
            return;
        }
        if (current == null && computed.equals(State.EMPTY)) {
            return;
        }
        SERVER.put(player.getUUID(), new Tracked(computed,
                current == null ? 1 : current.version() + 1));
        NetworkManager.sendSpellLocksToClient(player, SyncSpellLocksPayload.of(computed));
    }

    private static State compute(Player player) {
        List<String> grants = new ArrayList<>();
        Map<String, Lock> spells = new LinkedHashMap<>();
        Map<String, Lock> schools = new LinkedHashMap<>();
        Map<String, SpellTuning> tunes = new LinkedHashMap<>();
        Map<String, String> inherits = new LinkedHashMap<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            ItemStack stack = worn.stack();
            EquipmentDefinition definition = worn.definition();
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                SpellLocks locks = entry.effect().spells();
                if (locks.isEmpty() || !EquipmentRuntime.liveAtRest(player, definition,
                        worn.copy(), named.getKey(), entry)) {
                    continue;
                }
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                Lock lock = new Lock(locks.disableMode(), itemId.toString());
                for (String spell : locks.grant()) {
                    if (!grants.contains(spell)) {
                        grants.add(spell);
                    }
                }
                for (String spell : locks.disable()) {
                    spells.putIfAbsent(spell, lock);
                }
                for (String school : locks.disableSchools()) {
                    schools.putIfAbsent(schoolKey(school), lock);
                }
                for (SpellTune tuned : locks.tune()) {
                    tunes.putIfAbsent(tuned.spell(), tuned.tuning());
                }
                Lock removal = new Lock(SpellLocks.Mode.REMOVE, itemId.toString());
                for (Map.Entry<String, String> replaced : locks.replace().entrySet()) {
                    if (!grants.contains(replaced.getValue())) {
                        grants.add(replaced.getValue());
                    }
                    spells.putIfAbsent(replaced.getKey(), removal);
                    inherits.putIfAbsent(replaced.getValue(), replaced.getKey());
                }
            }
        }
        for (Map.Entry<String, String> parent : SpellParentLoader.parents().entrySet()) {
            inherits.putIfAbsent(parent.getKey(), parent.getValue());
            SpellTuning tuned = tunes.get(parent.getValue());
            if (tuned != null) {
                tunes.putIfAbsent(parent.getKey(), tuned);
            }
        }
        return new State(grants, spells, schools, tunes, inherits);
    }

    public static void accept(State state) {
        mirrored = state == null ? State.EMPTY : state;
        mirroredVersion++;
    }

    public static void forget(UUID playerId) {
        SERVER.remove(playerId);
    }

    public static void clearMirror() {
        accept(State.EMPTY);
    }
}
