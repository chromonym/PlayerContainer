package io.github.chromonym.playercontainer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;

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

	public static Identifier identifier(String id) {
		return Identifier.of(MOD_ID, id);
	}
}