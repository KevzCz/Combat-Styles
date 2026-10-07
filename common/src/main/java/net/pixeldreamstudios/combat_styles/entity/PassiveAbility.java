package net.pixeldreamstudios.combat_styles.entity;

public interface PassiveAbility extends SummonAbility {

    float ALWAYS = 1.0F;

    PassiveTrigger on();

    default float chance() {
        return ALWAYS;
    }

    @Override
    default AbilityKind kind() {
        return AbilityKind.PASSIVE;
    }
}
