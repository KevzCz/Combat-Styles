package net.pixeldreamstudios.combat_styles.custom;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record CustomContext(String scope, Object box, JsonObject options, Player owner, Entity actor,
                            LivingEntity target) {

    public Level level() {
        if (actor != null) {
            return actor.level();
        }
        if (owner != null) {
            return owner.level();
        }
        return target == null ? null : target.level();
    }

    public <T> T options(Codec<T> codec, T fallback) {
        return options == null
                ? fallback
                : codec.parse(JsonOps.INSTANCE, options).result().orElse(fallback);
    }

    public <T> T box(Class<T> type) {
        return type.isInstance(box) ? type.cast(box) : null;
    }
}
