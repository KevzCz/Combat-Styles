package net.pixeldreamstudios.combat_styles.client;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class PreviewAnimationBinding {

    private static final int LAYER_PRIORITY = 9000;

    private PreviewAnimationBinding() {
    }

    public static void bind(AbstractClientPlayer player, AnimationPreview preview) {
        if (!preview.isAvailable()) {
            return;
        }
        AnimationStack stack = stackFor(player);
        if (stack != null) {
            stack.addAnimLayer(LAYER_PRIORITY, preview.layer());
        }
    }

    public static void unbind(AbstractClientPlayer player, AnimationPreview preview) {
        if (!preview.isAvailable()) {
            return;
        }
        AnimationStack stack = stackFor(player);
        if (stack != null) {
            stack.removeLayer(preview.layer());
        }
    }

    private static AnimationStack stackFor(AbstractClientPlayer player) {
        if (!CombatStyles.isPlayerAnimatorLoaded()) {
            return null;
        }
        try {
            return PlayerAnimationAccess.getPlayerAnimLayer(player);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Unable to access the player animation stack", throwable);
            return null;
        }
    }
}
