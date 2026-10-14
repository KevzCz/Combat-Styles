package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.pixeldreamstudios.combat_styles.skill.SkillGrants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GrantRewards {

    private static final String KEY_EQUIPMENT = "equipment";
    private static final String KEY_REPLACE = "replace";
    private static final String KEY_STYLES = "styles";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_ICON = "icon";

    private GrantRewards() {
    }

    static final class Equipment extends PlayerReward {

        private final List<String> equipment;
        private final List<String> replaces;
        private final Map<String, String> icons = new LinkedHashMap<>();

        Equipment(JsonElement data, RewardConfigContext context) {
            JsonObject object = RewardData.object(data);
            this.equipment = RewardData.strings(object, KEY_EQUIPMENT);
            this.replaces = RewardData.strings(object, KEY_REPLACE);
            if (object.has(KEY_ICON)) {
                String icon = object.get(KEY_ICON).getAsString();
                equipment.forEach(id -> icons.put(id, icon));
            }
            if (equipment.isEmpty() && replaces.isEmpty()) {
                throw new IllegalArgumentException("Expected \"" + KEY_EQUIPMENT + "\" or \"" + KEY_REPLACE + "\"");
            }
        }

        @Override
        protected void apply(ServerPlayer player, int count) {
            SkillGrants.put(player, key, count > 0
                    ? new SkillGrants.Grant(equipment, replaces, List.of(), 0.0D, icons)
                    : SkillGrants.Grant.EMPTY);
        }
    }

    static final class Style extends PlayerReward {

        private final List<String> styles;

        Style(JsonElement data, RewardConfigContext context) {
            this.styles = RewardData.strings(RewardData.object(data), KEY_STYLES);
            if (styles.isEmpty()) {
                throw new IllegalArgumentException("Expected \"" + KEY_STYLES + "\"");
            }
        }

        @Override
        protected void apply(ServerPlayer player, int count) {
            SkillGrants.put(player, key, count > 0
                    ? new SkillGrants.Grant(List.of(), List.of(), styles, 0.0D, Map.of())
                    : SkillGrants.Grant.EMPTY);
        }
    }

    static final class Budget extends PlayerReward {

        private final double amount;

        Budget(JsonElement data, RewardConfigContext context) {
            JsonObject object = RewardData.object(data);
            if (!object.has(KEY_AMOUNT)) {
                throw new IllegalArgumentException("Expected \"" + KEY_AMOUNT + "\"");
            }
            this.amount = object.get(KEY_AMOUNT).getAsDouble();
        }

        @Override
        protected void apply(ServerPlayer player, int count) {
            SkillGrants.put(player, key, count > 0
                    ? new SkillGrants.Grant(List.of(), List.of(), List.of(), amount * count, Map.of())
                    : SkillGrants.Grant.EMPTY);
        }
    }
}
