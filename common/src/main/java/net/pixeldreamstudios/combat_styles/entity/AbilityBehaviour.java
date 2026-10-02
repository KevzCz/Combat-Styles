package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

import java.util.Map;

@FunctionalInterface
public interface AbilityBehaviour {

    void tick(ServerLevel level, ConfigurableSummon summon, Player owner,
              int elapsed, float damage, AbilityConfig.Entry tuned,
              Map<String, Double> params);
}
