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
                        ContainerInstance.releasePlayer(player);
                        context.getSource().sendFeedback(() -> Text.literal("Released player "+player.getNameForScoreboard()), false);
                    }
                    return 1;
                })
            .then(argument("player", EntityArgumentType.player()).requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    ContainerInstance.releasePlayer(player);
                    context.getSource().sendFeedback(() -> Text.literal("Released player "+player.getNameForScoreboard()), false);
                    return 1;
                }))
        ).then(
            literal("update").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    PlayerContainer.sendCIPtoAll(context.getSource().getServer().getPlayerManager());
                    context.getSource().sendFeedback(() -> Text.literal("Sent container data to all online players"), false);
                    return 1;
                })
        ).then(
            literal("clean").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    PlayerContainer.cleanContainers(context.getSource().getServer().getPlayerManager());
                    context.getSource().sendFeedback(() -> Text.literal("Cleaned empty containers"), false);
                    return 1;
                })
        )));
    }
}
