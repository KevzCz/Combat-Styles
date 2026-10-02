package net.pixeldreamstudios.combat_styles.equipment;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientPass {

    private static volatile String runningId;
    private static volatile int played;
    private static volatile int size;

    private static final int NO_COMBO = -1;

    private static volatile int lastCombo = NO_COMBO;

    private ClientPass() {
    }

    public static void beginSwing(Player player, EquipmentDefinition definition,
                                  StyleSubstitution substitution) {
        if (player == null || !player.level().isClientSide()
                || definition == null || substitution == null) {
            return;
        }
        int styles = substitution.styles().size();
        if (styles <= 0) {
            release();
            return;
        }
        int combo = Minecraft.getInstance() instanceof MinecraftClient_BetterCombat client
                ? client.getComboCount()
                : NO_COMBO;
        if (combo == lastCombo) {
            return;
        }
        boolean rewound = combo != NO_COMBO && lastCombo != NO_COMBO && combo < lastCombo;
        lastCombo = combo;
        if (rewound && runningId != null) {
            trace(player, definition.id(), "combo reset at " + played + "/" + size
                    + ", restarting");
            release();
        }
        if (definition.id().equals(runningId) && styles != size) {
            trace(player, definition.id(), "moveset changed " + size + " -> " + styles
                    + ", restarting");
            release();
        }
        if (definition.id().equals(runningId)) {
            if (played >= size) {
                trace(player, definition.id(), "complete " + played + "/" + size + ", restarting");
                release();
            } else {
                played++;
                trace(player, definition.id(), "advance " + played + "/" + size);
                return;
            }
        }
        if (!ChargeMirror.charged(definition.id())) {
            trace(player, definition.id(), "not charged, no pass");
            release();
            return;
        }
        runningId = definition.id();
        size = styles;
        played = 1;
        if (player instanceof PlayerAttackProperties props
                && Math.floorMod(props.getComboCount(), styles) != 0) {
            props.setComboCount(0);
            lastCombo = 0;
            trace(player, definition.id(), "START 1/" + size + " (combo realigned)");
            return;
        }
        trace(player, definition.id(), "START 1/" + size);
    }

    public static void trace(Player player, String id, String what) {
        if (!CombatDebug.enabled(player)) {
            return;
        }
        String bc = "";
        if (Minecraft.getInstance() instanceof MinecraftClient_BetterCombat client) {
            AttackHand hand = client.getCurrentAttackHand();
            bc = " | bc combo " + client.getComboCount()
                    + " upswing " + client.getUpswingTicks()
                    + " swingProgress " + String.format("%.2f", client.getSwingProgress())
                    + " inSwing " + client.isWeaponSwingInProgress()
                    + " hand " + (hand == null ? "-"
                            : hand.attack().animation() + " " + hand.combo().current()
                                    + "/" + hand.combo().total());
        }
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "pass — " + id + " " + what + bc);
        }
    }

    public static boolean running(String id) {
        return id != null && id.equals(runningId);
    }

    public static int progress() {
        return runningId == null ? 0 : played;
    }

    public static void release() {
        runningId = null;
        played = 0;
        size = 0;
    }
}
