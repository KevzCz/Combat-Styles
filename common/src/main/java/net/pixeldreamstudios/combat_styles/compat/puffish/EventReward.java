package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.pixeldreamstudios.combat_styles.skill.SkillActions;
import net.pixeldreamstudios.combat_styles.skill.SkillEventFilter;
import net.pixeldreamstudios.combat_styles.skill.SkillEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class EventReward extends PlayerReward {

    private static final String KEY_COOLDOWN = "cooldown";
    private static final String KEY_ACTIONS = "actions";

    private final SkillEventFilter filter;
    private final int cooldown;
    private final SkillActions.Actions actions;
    private final Map<UUID, Long> lastFired = new ConcurrentHashMap<>();

    EventReward(JsonElement data, RewardConfigContext context) {
        JsonObject object = RewardData.object(data);
        this.filter = SkillEventFilter.parse(object);
        this.cooldown = object.has(KEY_COOLDOWN) ? object.get(KEY_COOLDOWN).getAsInt() : 0;
        if (!object.has(KEY_ACTIONS) || !object.get(KEY_ACTIONS).isJsonObject()) {
            throw new IllegalArgumentException("Expected an \"" + KEY_ACTIONS + "\" object");
        }
        this.actions = SkillActions.parse(object.getAsJsonObject(KEY_ACTIONS));
    }

    @Override
    protected void apply(ServerPlayer player, int count) {
        if (count > 0) {
            SkillEvents.listen(player, key, this::on);
        } else {
            SkillEvents.unlisten(player, key);
            lastFired.remove(player.getUUID());
        }
    }

    private void on(SkillEvents.Event event) {
        if (!filter.matches(event)) {
            return;
        }
        ServerPlayer player = event.player();
        long now = player.level().getGameTime();
        Long last = lastFired.get(player.getUUID());
        if (cooldown > 0 && last != null && now - last < cooldown) {
            return;
        }
        lastFired.put(player.getUUID(), now);
        SkillActions.run(actions, player, event.other());
    }

    @Override
    protected void forget(UUID playerId) {
        super.forget(playerId);
        lastFired.remove(playerId);
    }
}
