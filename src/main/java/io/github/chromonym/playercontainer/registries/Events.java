package io.github.chromonym.playercontainer.registries;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.entity.BlockEntity;
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
				Optional<BlockEntity> owner = entry.getValue().getOwner().right();
				if (owner.isPresent() && owner.get().getPos() == blockEntity.getPos()) {
                    toRemoveAll.add(entry.getValue()); // entry.getValue().releaseAll(world, true);
				}
			}
			for (ContainerInstance<?> cont : toRemoveAll) {
				cont.releaseAll(world.getServer().getPlayerManager(), true);
			}
		});
    }

}
