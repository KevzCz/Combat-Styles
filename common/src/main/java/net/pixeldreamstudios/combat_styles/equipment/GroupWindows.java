package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GroupWindows {

    private static final String SEPARATOR = "|";

    private static final Map<String, Long> OPENED_BY = new ConcurrentHashMap<>();

    private GroupWindows() {
    }

    public static void forget(UUID playerId) {
        Gates.forget(playerId);
        OPENED_BY.keySet().removeIf(key -> key.startsWith(playerId + SEPARATOR));
    }

    public static void clear() {
        Gates.clear();
        OPENED_BY.clear();
    }

    private static String openerKey(Player player, String itemId, String group) {
        return player.getUUID() + SEPARATOR + itemId + SEPARATOR + group;
    }

    public static boolean armed(Player player, String itemId, String group) {
        return Gates.windowOpen(player, GateScope.GROUP, itemId, group);
    }

    public static boolean openOnSwing(Player player, String itemId, String group,
                                      EffectGroup definition, EquipmentContext context) {
        if (definition == null || !definition.armsOnSwing() || player.level().isClientSide()
                || armed(player, itemId, group)) {
            return false;
        }
        if (context == null || !definition.trigger().matches(context, definition.cooldown())) {
            return false;
        }
        if (!Gates.arm(player, GateScope.GROUP, itemId, group, definition.cooldown())) {
            return false;
        }
        OPENED_BY.put(openerKey(player, itemId, group), CombatState.swungAt(player));
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "group window — " + itemId + "#" + group
                    + " opened by this attack; the next attack draws");
        }
        return true;
    }

    private static boolean openedByThisAttack(Player player, String itemId, String group) {
        Long opener = OPENED_BY.get(openerKey(player, itemId, group));
        return opener != null && opener == CombatState.swungAt(player);
    }

    public static boolean claimUse(Player player, String itemId, String group,
                                   EffectGroup definition) {
        if (definition == null || player.level().isClientSide()) {
            return true;
        }
        return Gates.claim(player, GateScope.GROUP, itemId, group, definition.cooldown(), true);
    }

    public static boolean claim(Player player, String itemId, String group,
                                EffectGroup definition, EquipmentContext context,
                                boolean chancePassed) {
        if (definition == null) {
            return true;
        }
        if (player.level().isClientSide()) {
            return true;
        }
        if (definition.armsOnSwing()) {
            if (!armed(player, itemId, group) || openedByThisAttack(player, itemId, group)) {
                return false;
            }
            return claimUse(player, itemId, group, definition);
        }
        if (context != null
                && !definition.trigger().matches(context, definition.cooldown())) {
            return false;
        }
        boolean arms = definition.trigger().arms(definition.cooldown());
        if (arms && context != null && definition.trigger().eventFiring(context)) {
            if (chancePassed) {
                Gates.arm(player, GateScope.GROUP, itemId, group, definition.cooldown());
            }
            return false;
        }
        boolean insideWindow = definition.windowed() && armed(player, itemId, group);
        if (!chancePassed && !insideWindow) {
            return false;
        }
        return Gates.claim(player, GateScope.GROUP, itemId, group,
                definition.cooldown(), arms);
    }

    public static boolean wouldClaim(Player player, String itemId, String group,
                                     EffectGroup definition, EquipmentContext context) {
        if (definition == null) {
            return true;
        }
        if (definition.armsOnSwing()) {
            return armed(player, itemId, group)
                    && (player.level().isClientSide()
                    || !openedByThisAttack(player, itemId, group));
        }
        if (context != null
                && !definition.trigger().matches(context, definition.cooldown())) {
            return false;
        }
        EffectCooldown cooldown = definition.cooldown();
        boolean open = armed(player, itemId, group);
        if (definition.trigger().arms(cooldown)) {
            return open && (context == null || !definition.trigger().eventFiring(context));
        }
        return open || !Gates.cooling(player, GateScope.GROUP, itemId, group, cooldown);
    }

    public static int remaining(Player player, String itemId, String group) {
        int window = Gates.windowRemaining(player, GateScope.GROUP, itemId, group);
        return window > 0 ? window
                : Gates.remaining(player, GateScope.GROUP, itemId, group);
    }

    public static int used(Player player, String itemId, String group) {
        return Gates.used(player, GateScope.GROUP, itemId, group);
    }
}
