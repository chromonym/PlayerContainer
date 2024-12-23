package io.github.chromonym.playercontainer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.chromonym.playercontainer.registries.*;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.networking.ContainerPersistentState;

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
		ItemGroups.initialize();
		Commands.intialize();
		Events.initialize();

	}

	public static void sendCIPtoAll(PlayerManager players) {
		ContainerPersistentState state = ContainerPersistentState.getServerState(players.getServer());
		state.updateFromCI();
		for (ServerPlayerEntity player : players.getPlayerList()) {
            ServerPlayNetworking.send(player, new ContainerInstancesPayload(ContainerInstance.containers, ContainerInstance.players));
		}
	}

	public static void cleanContainers(PlayerManager players) {
		LOGGER.info("Cleaning containers");
		Set<UUID> toRemove = new HashSet<UUID>();
		for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
			if (entry.getValue().getPlayerCount() == 0 && !ContainerInstance.playersToRecapture.values().contains(entry.getKey()) && !ContainerInstance.playersToRelease.values().contains(entry.getKey())) {
				// if container does not contain any players and is not currently pending release or recapture
				toRemove.add(entry.getKey());
			}
		}
		for (UUID cont : toRemove) {
			LOGGER.info("Removing "+cont.toString());
			ContainerInstance.containers.remove(cont);
		}
		sendCIPtoAll(players);
	}

	public static void destroyMissingContainers(PlayerManager players) {
		Set<ContainerInstance<?>> toDestroy = new HashSet<ContainerInstance<?>>();
		for (ContainerInstance<?> ci : ContainerInstance.containers.values()) {
			ci.getOwner().ifLeft(entity -> {
				if (entity instanceof PlayerEntity owner) {
					ServerPlayerEntity player = players.getPlayer(owner.getUuid());
					if (player != null) {
						PlayerInventory inv = player.getInventory();
						// owning player exists and is online
						boolean found = false;
						if (player.currentScreenHandler != null) {
							ItemStack stack = player.currentScreenHandler.getCursorStack();
							if (!stack.isEmpty() && stack.getItem() instanceof AbstractContainerItem<?>) {
								found = true; // pwease don't destroy it :(
							}
						}
						for(int i = 0; i < inv.size(); ++i) {
							ItemStack stack = inv.getStack(i);
							if (!stack.isEmpty() && stack.getItem() instanceof AbstractContainerItem<?> containerItem) {
								ContainerInstance<?> checkCont = containerItem.getOrMakeContainerInstance(stack, player.getWorld(), true);
								if (checkCont != null && checkCont.getID() == ci.getID()) {
									found = true;
								}
							}
						}
						if (!found) {
							toDestroy.add(ci);
						}
					}
				}
			});
		}
		for (ContainerInstance<?> ci : toDestroy) {
			ci.destroy(players);
		}
	}

	public static Identifier identifier(String id) {
		return Identifier.of(MOD_ID, id);
	}
}