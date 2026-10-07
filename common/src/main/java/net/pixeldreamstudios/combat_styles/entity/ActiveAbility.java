package net.pixeldreamstudios.combat_styles.entity;

public interface ActiveAbility extends SummonAbility {

    @Override
    default AbilityKind kind() {
        return AbilityKind.ACTIVE;
    }
}
