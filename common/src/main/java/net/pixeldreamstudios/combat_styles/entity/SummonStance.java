package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public enum SummonStance {

    CROWNED,
    LOOSE,
    SHOULDERED,
    HELD,
    INDEPENDENT;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SummonStance of(ConfigurableSummon summon, Player owner, boolean rallied) {
        if (summon == null) {
            return SHOULDERED;
        }
        if (summon.inCrown()) {
            return CROWNED;
        }
        if (summon.acting()) {
            return HELD;
        }
        return summon.temporary() && !rallied ? LOOSE : SHOULDERED;
    }

    public boolean follows() {
        return this == LOOSE || this == SHOULDERED || this == INDEPENDENT;
    }

    public boolean canBeOrdered() {
        return this != CROWNED && this != HELD && this != INDEPENDENT;
    }

    public boolean idle() {
        return this == LOOSE || this == SHOULDERED || this == INDEPENDENT;
    }

    public boolean holdsPosition() {
        return this == HELD || this == CROWNED;
    }
}
