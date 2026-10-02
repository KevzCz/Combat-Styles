package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class EffectWindows {

    private EffectWindows() {
    }

    public static void forget(UUID playerId) {
        Gates.forget(playerId);
    }

    public static boolean armed(Player player, String itemId, String effectKey) {
        return Gates.windowOpen(player, GateScope.EFFECT, itemId, effectKey);
    }

    public static int remaining(Player player, String itemId, String effectKey) {
        return Gates.windowRemaining(player, GateScope.EFFECT, itemId, effectKey);
    }

    public static int used(Player player, String itemId, String effectKey) {
        return Gates.used(player, GateScope.EFFECT, itemId, effectKey);
    }

    public static boolean arms(EquipmentRuntime.Match match) {
        return match.trigger().arms(match.entry().cooldown());
    }

    public static boolean arm(Player player, EquipmentRuntime.Match match) {
        return Gates.arm(player, GateScope.EFFECT, match.id(), match.key(),
                match.entry().cooldown());
    }

    public static boolean claim(Player player, EquipmentRuntime.Match match) {
        EffectCooldown window = match.entry().cooldown();
        if (!window.windowed()) {
            return !EquipmentRuntime.consumeProcCooldown(player, match);
        }
        return Gates.claim(player, GateScope.EFFECT, match.id(), match.key(),
                window, arms(match));
    }

    public static boolean claimSwing(Player player, EquipmentRuntime.Match match) {
        return Gates.claim(player, GateScope.EFFECT, match.id(), match.key(),
                match.entry().cooldown(), false);
    }
}
