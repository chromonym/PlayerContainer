package io.github.chromonym.playercontainer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroups;

import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;

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
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			LOGGER.info("Entity loaded: "+entity.getNameForScoreboard());
			if (entity instanceof PlayerEntity player) {
				if (ContainerInstance.playersToRecapture.keySet().contains(player.getUuid())) {
					ContainerInstance<?> ci = ContainerInstance.containers.get(ContainerInstance.playersToRecapture.get(player.getUuid()));
					if (ci.capture(player, true)) {
						ContainerInstance.playersToRecapture.remove(player.getUuid());
					} else {
						LOGGER.info("Could not recapture player "+player.getNameForScoreboard()+", releasing instead");
						ci.release(player, false);
					}
				}
			}
			for (Entry<UUID, UUID> entry : ContainerInstance.playersToRecapture.entrySet()) {
				PlayerEntity player = world.getPlayerByUuid(entry.getKey());
				if (player != null) {
					ContainerInstance<?> ci = ContainerInstance.containers.get(entry.getValue());
					Optional<Entity> owner = ci.getOwner().left();
					if (owner.isPresent() && owner.get().getUuid() == entity.getUuid()) {
						ci.capture(player, true);
					}
				}
			}
		});

		LOGGER.info("Hello Fabric world!");
	}
}