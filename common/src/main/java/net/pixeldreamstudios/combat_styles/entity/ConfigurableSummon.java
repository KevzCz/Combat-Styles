package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.Optional;
import java.util.UUID;

public interface ConfigurableSummon extends OwnableEntity {

    void configure(Player owner, LivingEntity target, float damage, int lifespanTicks);

    default void order(Player owner, LivingEntity target, float damage,
                       String ability, AbilityTuning tuning) {
        configure(owner, target, damage, KEEP_LIFESPAN);
        orderedWith(tuning);
        acceptAbility(ability);
    }

    int KEEP_LIFESPAN = 0;

    default double spawnLift() {
        return 0.0D;
    }

    default boolean canSpawn(Player owner, int index) {
        return true;
    }

    default float damageScale(Player owner) {
        return 1.0F;
    }

    default double formationAngle(int index, int spread) {
        return Double.NaN;
    }

    default void onSpawned(Player owner, int index) {
    }

    default void companionSpawned(Player owner, int slot, int count) {
    }

    default String abilityFor(Player owner, SummonProfile profile, String declared, int index) {
        return declared;
    }

    default boolean despawnsWithLifespan() {
        return true;
    }

    default boolean beginDespawn() {
        return false;
    }

    default void acceptAbility(String ability) {
    }

    default void orderedWith(AbilityTuning tuning) {
    }

    default AbilityTuning orderTuning() {
        return null;
    }

    default void retarget(LivingEntity target) {
    }

    default boolean acting() {
        return false;
    }

    default boolean temporary() {
        return false;
    }

    default boolean inCrown() {
        return false;
    }

    default boolean fading() {
        return false;
    }

    default boolean busy() {
        return inCrown() || fading();
    }

    default boolean canAct() {
        return !acting() && !busy();
    }

    default SummonStance stance() {
        return SummonStance.SHOULDERED;
    }

    default void stopActing() {
    }

    default void dismiss() {
    }

    default boolean reusable() {
        return false;
    }

    default UUID summonOwner() {
        UUID tracked = trackedOwner();
        return tracked != null ? tracked : syncedOwner().orElse(null);
    }

    default UUID trackedOwner() {
        return null;
    }

    default Optional<UUID> syncedOwner() {
        return Optional.empty();
    }

    default Player ownerPlayer(Level level) {
        UUID id = summonOwner();
        return id == null || level == null ? null : level.getPlayerByUUID(id);
    }

    default boolean ownedBy(Player player) {
        return player != null && player.getUUID().equals(summonOwner());
    }

    @Override
    default UUID getOwnerUUID() {
        return summonOwner();
    }
}
