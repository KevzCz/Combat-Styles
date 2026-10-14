package net.pixeldreamstudios.combat_styles.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.combat_styles.skill.SkillChoices;

import java.util.List;

public final class SkillChoiceCommand {

    private static final String ROOT = "skillchoice";
    private static final String ARG_CHOICE = "choice";
    private static final String ARG_OPTION = "option";
    private static final int TICKS_PER_SECOND = 20;
    private static final String KEY_NONE = "command.combat_styles.skillchoice.none";
    private static final String KEY_ENTRY = "command.combat_styles.skillchoice.entry";
    private static final String KEY_CHANGED = "command.combat_styles.skillchoice.changed";
    private static final String KEY_UNCHANGED = "command.combat_styles.skillchoice.unchanged";
    private static final String KEY_UNKNOWN_CHOICE = "command.combat_styles.skillchoice.unknown_choice";
    private static final String KEY_UNKNOWN_OPTION = "command.combat_styles.skillchoice.unknown_option";
    private static final String KEY_COOLDOWN = "command.combat_styles.skillchoice.cooldown";
    private static final String KEY_IN_COMBAT = "command.combat_styles.skillchoice.in_combat";

    private static final SuggestionProvider<CommandSourceStack> CHOICES = (context, builder) ->
            SharedSuggestionProvider.suggest(SkillChoices.choicesOf(context.getSource().getPlayer()), builder);

    private static final SuggestionProvider<CommandSourceStack> OPTIONS = (context, builder) -> {
        SkillChoices.Spec spec = SkillChoices.specOf(context.getSource().getPlayer(),
                StringArgumentType.getString(context, ARG_CHOICE));
        return SharedSuggestionProvider.suggest(spec == null ? List.of() : spec.options(), builder);
    };

    private SkillChoiceCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .executes(SkillChoiceCommand::list)
                .then(Commands.argument(ARG_CHOICE, StringArgumentType.string())
                        .suggests(CHOICES)
                        .then(Commands.argument(ARG_OPTION, StringArgumentType.word())
                                .suggests(OPTIONS)
                                .executes(SkillChoiceCommand::select))));
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> choices = SkillChoices.choicesOf(player);
        if (choices.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(KEY_NONE));
            return 0;
        }
        for (String choice : choices) {
            SkillChoices.Spec spec = SkillChoices.specOf(player, choice);
            context.getSource().sendSuccess(() -> Component.translatable(KEY_ENTRY, choice,
                    SkillChoices.selected(player, choice), String.join(", ", spec.options())), false);
        }
        return choices.size();
    }

    private static int select(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String choice = StringArgumentType.getString(context, ARG_CHOICE);
        String option = StringArgumentType.getString(context, ARG_OPTION);
        SkillChoices.Result result = SkillChoices.select(player, choice, option);
        switch (result) {
            case CHANGED -> context.getSource().sendSuccess(() -> Component.translatable(KEY_CHANGED, choice, option), false);
            case UNCHANGED -> context.getSource().sendSuccess(() -> Component.translatable(KEY_UNCHANGED, choice, option), false);
            case UNKNOWN_CHOICE -> context.getSource().sendFailure(Component.translatable(KEY_UNKNOWN_CHOICE, choice));
            case UNKNOWN_OPTION -> context.getSource().sendFailure(Component.translatable(KEY_UNKNOWN_OPTION, option, choice));
            case ON_COOLDOWN -> context.getSource().sendFailure(Component.translatable(KEY_COOLDOWN, choice,
                    SkillChoices.cooldownLeft(player, choice) / TICKS_PER_SECOND));
            case IN_COMBAT -> context.getSource().sendFailure(Component.translatable(KEY_IN_COMBAT, choice));
        }
        return result == SkillChoices.Result.CHANGED || result == SkillChoices.Result.UNCHANGED ? 1 : 0;
    }
}
