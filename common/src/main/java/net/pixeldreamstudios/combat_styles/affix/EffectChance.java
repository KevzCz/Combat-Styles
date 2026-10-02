package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.world.entity.player.Player;

public final class EffectChance {

    public static final float ALWAYS = 1.0F;

    private EffectChance() {
    }

    public static boolean rolls(Player player, float chance) {
        if (chance >= ALWAYS) {
            return true;
        }
        if (chance <= 0.0F) {
            return false;
        }
        return player.getRandom().nextFloat() < chance;
    }
}
