package io.github.chromonym.playercontainer.registries;

import static net.minecraft.server.command.CommandManager.*;

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
            .then(argument("player", EntityArgumentType.player())
                .executes(context -> {
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    ContainerInstance.releasePlayer(player);
                    context.getSource().sendFeedback(() -> Text.literal("Released player "+player.getNameForScoreboard()), false);
                    return 1;
                }))
        )));
    }
}
