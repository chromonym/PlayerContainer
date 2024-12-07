package io.github.chromonym.playercontainer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.chromonym.playercontainer.containers.ContainerInstance;

public class PlayerContainer implements ModInitializer {
	public static final String MOD_ID = "playercontainer";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static BiMap<Integer, ContainerInstance<?>> containers = HashBiMap.create();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		PlayerContainerComponents.initialize();
		PlayerContainerItems.initialize();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
			.register((itemGroup) -> itemGroup.add(PlayerContainerItems.simpleContainer));

		LOGGER.info("Hello Fabric world!");
	}

	public static int getNextAvailableContainerID() {
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			if (!containers.containsKey(i)) {
				return i;
			} else {
				LOGGER.info("ID "+Integer.toString(i)+" already taken");
			}
		}
		return 0;
	}
}