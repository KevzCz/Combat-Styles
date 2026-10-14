package net.pixeldreamstudios.combat_styles.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.combat_styles.kit.KitHooks;

import java.util.Collection;

public final class ClassKitCommand {

    private static final String ROOT = "classkit";
    private static final String RESET = "reset";
    private static final String RECHOOSE = "rechoose";
    private static final String ARG_TARGETS = "targets";
    private static final String ARG_NEW_GEAR = "new_gear";
    private static final int PERMISSION_LEVEL = 2;
    private static final String KEY_RESET = "command.combat_styles.classkit.reset";
    private static final String KEY_RECHOOSE = "command.combat_styles.classkit.rechoose";
    private static final String KEY_NO_STARTER_KIT = "command.combat_styles.classkit.no_starter_kit";

    private ClassKitCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(Commands.literal(RESET)
                        .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                                .executes(ClassKitCommand::reset)))
                .then(Commands.literal(RECHOOSE)
                        .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                                .executes(context -> rechoose(context, false))
                                .then(Commands.argument(ARG_NEW_GEAR, BoolArgumentType.bool())
                                        .executes(context -> rechoose(context, BoolArgumentType.getBool(context, ARG_NEW_GEAR)))))));
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, ARG_TARGETS);
        for (ServerPlayer player : players) {
            KitHooks.reset(player);
        }
        context.getSource().sendSuccess(() -> Component.translatable(KEY_RESET, players.size()), true);
        return players.size();
    }

    private static int rechoose(CommandContext<CommandSourceStack> context, boolean newGear) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, ARG_TARGETS);
        int reopened = 0;
        for (ServerPlayer player : players) {
            if (KitHooks.rechoose(player, newGear)) {
                reopened++;
            }
        }
        if (reopened == 0) {
            context.getSource().sendFailure(Component.translatable(KEY_NO_STARTER_KIT));
            return 0;
        }
        int count = reopened;
        context.getSource().sendSuccess(() -> Component.translatable(KEY_RECHOOSE, count), true);
        return count;
    }
}
