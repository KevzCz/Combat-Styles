package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.pixeldreamstudios.combat_styles.skill.SkillActions;
import net.pixeldreamstudios.combat_styles.skill.SkillCounters;
import net.pixeldreamstudios.combat_styles.skill.SkillEventFilter;
import net.pixeldreamstudios.combat_styles.skill.SkillEvents;
import net.pixeldreamstudios.combat_styles.skill.SkillScaling;

import java.util.ArrayList;
import java.util.List;

final class CounterReward extends PlayerReward {

    private static final String KEY_COUNTER = "counter";
    private static final String KEY_MAX = "max";
    private static final String KEY_DECAY = "decay_ticks";
    private static final String KEY_RESET_AT_MAX = "reset_at_max";
    private static final String KEY_SOURCES = "sources";
    private static final String KEY_RESETS = "resets";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_PER_STACK = "per_stack";
    private static final String KEY_AT_MAX = "at_max";
    private static final int DEFAULT_AMOUNT = 1;

    private record Source(SkillEventFilter filter, int amount) {
    }

    private final String counter;
    private final SkillCounters.Spec spec;
    private final List<Source> sources = new ArrayList<>();
    private final List<SkillEventFilter> resets = new ArrayList<>();
    private final List<SkillScaling.PerStack> perStack;
    private final SkillActions.Actions atMax;

    CounterReward(JsonElement data, RewardConfigContext context) {
        JsonObject object = RewardData.object(data);
        if (!object.has(KEY_COUNTER) || !object.has(KEY_MAX)) {
            throw new IllegalArgumentException("Expected \"" + KEY_COUNTER + "\" and \"" + KEY_MAX + "\"");
        }
        this.counter = object.get(KEY_COUNTER).getAsString();
        this.spec = new SkillCounters.Spec(object.get(KEY_MAX).getAsInt(),
                object.has(KEY_DECAY) ? object.get(KEY_DECAY).getAsInt() : 0,
                object.has(KEY_RESET_AT_MAX) && object.get(KEY_RESET_AT_MAX).getAsBoolean());
        for (JsonObject source : RewardData.objects(object, KEY_SOURCES)) {
            sources.add(new Source(SkillEventFilter.parse(source),
                    source.has(KEY_AMOUNT) ? source.get(KEY_AMOUNT).getAsInt() : DEFAULT_AMOUNT));
        }
        for (JsonObject reset : RewardData.objects(object, KEY_RESETS)) {
            resets.add(SkillEventFilter.parse(reset));
        }
        this.perStack = object.has(KEY_PER_STACK)
                ? SkillScaling.parse(object.getAsJsonArray(KEY_PER_STACK))
                : List.of();
        this.atMax = object.has(KEY_AT_MAX) ? SkillActions.parse(object.getAsJsonObject(KEY_AT_MAX))
                : SkillActions.Actions.NONE;
    }

    @Override
    protected void apply(ServerPlayer player, int count) {
        if (count > 0) {
            SkillCounters.define(player, counter, key, spec);
            SkillScaling.define(player, key, counter, perStack);
            SkillEvents.listen(player, key, this::on);
        } else {
            SkillEvents.unlisten(player, key);
            SkillScaling.define(player, key, counter, null);
            SkillCounters.define(player, counter, key, null);
        }
    }

    private void on(SkillEvents.Event event) {
        ServerPlayer player = event.player();
        if (event.type() == SkillEvents.Type.COUNTER_MAX && counter.equals(event.counter())) {
            SkillActions.run(atMax, player, event.other());
        }
        for (SkillEventFilter reset : resets) {
            if (reset.matches(event)) {
                SkillCounters.reset(player, counter);
                return;
            }
        }
        for (Source source : sources) {
            if (source.filter().matches(event)) {
                SkillCounters.add(player, counter, source.amount());
            }
        }
    }
}
