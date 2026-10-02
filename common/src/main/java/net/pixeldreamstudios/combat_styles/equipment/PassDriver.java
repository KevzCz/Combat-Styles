package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.style.SwingHolder;

public final class PassDriver {

    private PassDriver() {
    }

    public static void onSwingStart(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        EquipmentDefinition definition;
        SwingHolder.push(player);
        try {
            definition = SubstitutionScope.candidateFor(player, weapon);
        } finally {
            SwingHolder.pop();
        }
        if (definition == null) {
            ClientPass.release();
            return;
        }
        StyleSubstitution substitution =
                definition.substitutingEffect(player).substitution().orElse(null);
        if (substitution == null || substitution.isEmpty()) {
            ClientPass.release();
            return;
        }
        ClientPass.beginSwing(player, definition, substitution);
    }
}
