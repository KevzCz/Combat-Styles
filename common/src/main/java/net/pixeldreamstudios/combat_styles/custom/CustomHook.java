package net.pixeldreamstudios.combat_styles.custom;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.pixeldreamstudios.combat_styles.config.OptionField;

import java.util.List;
import java.util.Set;

public interface CustomHook {

    default Set<String> scopes() {
        return Set.of();
    }

    default List<OptionField> options() {
        return List.of();
    }

    default void onApply(CustomContext context) {
    }

    default void onSpawn(CustomContext context, Entity spawned) {
    }

    default void onTick(CustomContext context, Entity entity) {
    }

    default void onHit(CustomContext context, Entity source, LivingEntity target) {
    }

    default void onLand(CustomContext context, Entity entity) {
    }

    default boolean test(CustomContext context) {
        return true;
    }

    default float modifyDamage(CustomContext context, float amount) {
        return amount;
    }

    default List<Component> tooltip(JsonObject options) {
        return List.of();
    }
}
