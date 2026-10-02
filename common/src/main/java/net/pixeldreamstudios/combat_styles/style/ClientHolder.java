package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.item.TooltipContextAccess;

import java.util.UUID;

public final class ClientHolder {

    private ClientHolder() {
    }

    public static Player localPlayer() {
        return TooltipContextAccess.viewer();
    }

    public static UUID localId() {
        UUID swinging = SwingHolder.currentId();
        if (swinging != null) {
            return swinging;
        }
        Player player = TooltipContextAccess.viewer();
        return player == null ? null : player.getUUID();
    }
}
