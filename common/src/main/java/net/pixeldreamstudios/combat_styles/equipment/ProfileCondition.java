package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public record ProfileCondition(Optional<String> effect,
                               Optional<String> holding,
                               Optional<String> wearing,
                               Optional<String> equipped,
                               boolean inverted) {

    public static final Codec<ProfileCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("effect").forGetter(ProfileCondition::effect),
            Codec.STRING.optionalFieldOf("holding").forGetter(ProfileCondition::holding),
            Codec.STRING.optionalFieldOf("wearing").forGetter(ProfileCondition::wearing),
            Codec.STRING.optionalFieldOf("equipped").forGetter(ProfileCondition::equipped),
            Codec.BOOL.optionalFieldOf("inverted", Boolean.FALSE)
                    .forGetter(ProfileCondition::inverted)
    ).apply(instance, ProfileCondition::new));

    public static final Codec<List<ProfileCondition>> LIST_CODEC = CODEC.listOf();

    public boolean test(Player player, ConditionContext context) {
        if (player == null) {
            return false;
        }
        boolean met = true;
        if (effect.isPresent()) {
            met = context.hasEffect(player, effect.get());
        }
        if (met && holding.isPresent()) {
            met = context.holding(player, holding.get());
        }
        if (met && wearing.isPresent()) {
            met = context.wearing(player, wearing.get());
        }
        if (met && equipped.isPresent()) {
            met = context.holding(player, equipped.get())
                    || context.wearing(player, equipped.get());
        }
        return inverted != met;
    }

    public static boolean all(List<ProfileCondition> conditions, Player player,
                              ConditionContext context) {
        if (conditions.isEmpty()) {
            return false;
        }
        for (ProfileCondition condition : conditions) {
            if (!condition.test(player, context)) {
                return false;
            }
        }
        return true;
    }

    public interface ConditionContext {

        boolean hasEffect(Player player, String effectId);

        boolean holding(Player player, String itemId);

        boolean wearing(Player player, String itemId);
    }
}
