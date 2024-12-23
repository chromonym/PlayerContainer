package io.github.chromonym.playercontainer.registries;

import java.util.HashSet;
import java.util.Map.Entry;

import com.mojang.authlib.GameProfile;

import java.util.Set;
import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class Events {
    
    public static void initialize() {
		/*ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			ContainerInstance.checkRecaptureDecapture(world);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ContainerInstance.checkRecaptureDecapture(handler.getPlayer().getWorld());
		});*/
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			ContainerInstance.checkRecaptureDecapture(world);
			ContainerInstance.disconnectedPlayers.clear();
		});
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server.getTicks() % 1200 == 0) {
                // every one minute (for testing - increase later)
                PlayerContainer.cleanContainers(server.getPlayerManager());
            }
            PlayerContainer.destroyMissingContainers(server.getPlayerManager());
            /*PlayerContainer.LOGGER.info(Integer.toString(ContainerInstance.players.size())+", "+Integer.toString(ContainerInstance.playersToRecapture.size())
            +", "+Integer.toString(ContainerInstance.playersToRelease.size())+", "+Integer.toString(ContainerInstance.disconnectedPlayers.size()));*/
        });
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sender.sendPacket(new ContainerInstancesPayload(ContainerInstance.containers, ContainerInstance.players), null);

		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			ContainerInstance.disconnectedPlayers.add(player.getUuid());
            if (ContainerInstance.players.keySet().contains(player.getGameProfile())) {
                // player is in a container
                ContainerInstance.containers.get(ContainerInstance.players.get(player.getGameProfile())).release(player, true); // temporarily release that player
            }
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
			Set<ContainerInstance<?>> toRemoveAll = new HashSet<ContainerInstance<?>>();
			for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
                entry.getValue().getOwner().ifRight(owner -> {
                    if (owner != null && blockEntity != null && owner.getPos() == blockEntity.getPos()) {
                        toRemoveAll.add(entry.getValue()); // entry.getValue().releaseAll(world, true);
                    }
                });
			}
			for (ContainerInstance<?> cont : toRemoveAll) {
				cont.releaseAll(world.getServer().getPlayerManager(), true);
			}
		});
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
                if (entry.getValue().getContainer() instanceof SpectatorContainer) {
                    entry.getValue().getOwner().ifLeft(owner -> {
                        if (owner != null && owner.getUuid() == player.getUuid()) {
                            for (GameProfile pe : entry.getValue().getPlayers()) {
                                ServerPlayerEntity capturedPlayer = destination.getServer().getPlayerManager().getPlayer(pe.getId());
                                if (capturedPlayer != null && capturedPlayer.isSpectator() && capturedPlayer.getCameraEntity() != null && capturedPlayer.getCameraEntity() != capturedPlayer) {
                                    capturedPlayer.setCameraEntity(player);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

}
