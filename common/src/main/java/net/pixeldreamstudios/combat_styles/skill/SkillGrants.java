package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.equipment.WornItems;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SyncSkillGrantsPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillGrants {

    public record Grant(List<String> equipment, List<String> replaces, List<String> styles, double budget,
                        Map<String, String> icons) {

        public static final Grant EMPTY = new Grant(List.of(), List.of(), List.of(), 0.0D, Map.of());

        public Grant {
            equipment = equipment == null ? List.of() : List.copyOf(equipment);
            replaces = replaces == null ? List.of() : List.copyOf(replaces);
            styles = styles == null ? List.of() : List.copyOf(styles);
            icons = icons == null ? Map.of() : Map.copyOf(icons);
        }

        public boolean isEmpty() {
            return equipment.isEmpty() && replaces.isEmpty() && styles.isEmpty() && budget == 0.0D;
        }
    }

    public record View(List<String> equipment, List<String> styles, double budget, Map<String, String> icons) {

        public static final View EMPTY = new View(List.of(), List.of(), 0.0D, Map.of());

        public View {
            equipment = equipment == null ? List.of() : List.copyOf(equipment);
            styles = styles == null ? List.of() : List.copyOf(styles);
            icons = icons == null ? Map.of() : Map.copyOf(icons);
        }
    }

    private static final Map<UUID, Map<String, Grant>> SOURCES = new ConcurrentHashMap<>();
    private static final Map<UUID, View> RESOLVED = new ConcurrentHashMap<>();
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();
    private static volatile View mirrored = View.EMPTY;

    private SkillGrants() {
    }

    public static void put(ServerPlayer player, String source, Grant grant) {
        if (player == null || source == null) {
            return;
        }
        Map<String, Grant> sources = SOURCES.computeIfAbsent(player.getUUID(),
                key -> new ConcurrentHashMap<>());
        Grant previous = grant == null || grant.isEmpty() ? sources.remove(source) : sources.put(source, grant);
        if (previous == null ? grant != null && !grant.isEmpty() : !previous.equals(grant)) {
            DIRTY.add(player.getUUID());
        }
    }

    public static void remove(ServerPlayer player, String source) {
        put(player, source, Grant.EMPTY);
    }

    public static View viewOf(Player player) {
        if (player == null) {
            return View.EMPTY;
        }
        if (player.level().isClientSide()) {
            return mirrored;
        }
        return RESOLVED.getOrDefault(player.getUUID(), View.EMPTY);
    }

    public static List<String> equipmentOf(Player player) {
        return viewOf(player).equipment();
    }

    public static List<String> stylesOf(Player player) {
        return viewOf(player).styles();
    }

    public static double budgetOf(Player player) {
        return viewOf(player).budget();
    }

    public static String iconOf(Player player, String equipment) {
        return viewOf(player).icons().get(equipment);
    }

    public static void flush(ServerPlayer player) {
        if (player == null || !DIRTY.remove(player.getUUID())) {
            return;
        }
        View resolved = resolve(SOURCES.getOrDefault(player.getUUID(), Map.of()));
        View previous = RESOLVED.put(player.getUUID(), resolved);
        if (resolved.equals(previous)) {
            return;
        }
        WornItems.forget(player.getUUID());
        NetworkManager.sendSkillGrantsToClient(player, SyncSkillGrantsPayload.of(resolved));
        if (previous == null || !resolved.styles().equals(previous.styles())) {
            NetworkManager.syncLearned(player);
        }
    }

    public static void accept(View view) {
        mirrored = view == null ? View.EMPTY : view;
    }

    public static void clearMirror() {
        mirrored = View.EMPTY;
    }

    public static void forget(UUID playerId) {
        SOURCES.remove(playerId);
        RESOLVED.remove(playerId);
        DIRTY.remove(playerId);
    }

    private static View resolve(Map<String, Grant> sources) {
        Set<String> equipment = new LinkedHashSet<>();
        Set<String> replaced = new LinkedHashSet<>();
        Set<String> styles = new LinkedHashSet<>();
        Map<String, String> icons = new LinkedHashMap<>();
        double budget = 0.0D;
        for (Grant grant : new LinkedHashMap<>(sources).values()) {
            equipment.addAll(grant.equipment());
            replaced.addAll(grant.replaces());
            styles.addAll(grant.styles());
            grant.icons().forEach(icons::putIfAbsent);
            budget += grant.budget();
        }
        List<String> live = new ArrayList<>(equipment);
        live.removeAll(replaced);
        icons.keySet().retainAll(live);
        return new View(live, List.copyOf(styles), budget, icons);
    }
}
