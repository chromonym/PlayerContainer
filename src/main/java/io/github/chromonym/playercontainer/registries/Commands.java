package io.github.chromonym.playercontainer.registries;

import static net.minecraft.server.command.CommandManager.*;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class Commands {
    public static void intialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("playercontainer")
        .then(
            literal("release")
                .executes(context -> {
                    if (context.getSource().isExecutedByPlayer()) {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        boolean success = ContainerInstance.releasePlayer(player);
                        if (success) {
                            context.getSource().sendFeedback(() -> Text.literal("Released player "+player.getNameForScoreboard()), false);
                        } else {
                            context.getSource().sendFeedback(() -> Text.literal("Could not release player "+player.getNameForScoreboard()), false);
                        }
                    }
                    return 1;
                })
            .then(argument("player", EntityArgumentType.player()).requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    boolean success = ContainerInstance.releasePlayer(player);
                    if (success) {
                        context.getSource().sendFeedback(() -> Text.literal("Released player "+player.getNameForScoreboard()), true);
                    } else {
                        context.getSource().sendFeedback(() -> Text.literal("Could not release player "+player.getNameForScoreboard()), true);
                    }
                    return 1;
                }))
        ).then(
            literal("update").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    PlayerContainer.sendCIPtoAll(context.getSource().getServer().getPlayerManager());
                    context.getSource().sendFeedback(() -> Text.literal("Sent container data to all online players"), true);
                    return 1;
                })
        ).then(
            literal("clean").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    PlayerContainer.cleanContainers(context.getSource().getServer().getPlayerManager());
                    context.getSource().sendFeedback(() -> Text.literal("Cleaned empty containers"), true);
                    return 1;
                })
        )));
    }
}
