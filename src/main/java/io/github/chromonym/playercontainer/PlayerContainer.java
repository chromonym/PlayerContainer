package io.github.chromonym.playercontainer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.chromonym.playercontainer.registries.Items;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.registries.Containers;

public class PlayerContainer implements ModInitializer {
	public static final String MOD_ID = "playercontainer";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		PayloadTypeRegistry.playS2C().register(ContainerInstancesPayload.ID, ContainerInstancesPayload.PACKET_CODEC);

		ItemComponents.initialize();
		Items.initialize();
		Containers.initialize();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
			.register((itemGroup) -> {
				itemGroup.add(Items.simpleContainer);
				itemGroup.add(Items.testContainer);
			});
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
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PlayerContainer.LOGGER.info("Player "+player.getNameForScoreboard()+" disconnected");
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
					PlayerContainer.LOGGER.info("BlockEntity unloaded: "+blockEntity.getPos().toShortString());
                    toRemoveAll.add(entry.getValue()); // entry.getValue().releaseAll(world, true);
				}
			}
			for (ContainerInstance<?> cont : toRemoveAll) {
				cont.releaseAll(world, true);
			}
		});

		LOGGER.info("Hello Fabric world!");
	}
}