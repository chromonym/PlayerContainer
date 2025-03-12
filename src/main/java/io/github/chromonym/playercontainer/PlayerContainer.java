package io.github.chromonym.playercontainer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.Category;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.doublekekse.area_lib.Area;
import dev.doublekekse.area_lib.AreaLib;
import io.github.chromonym.playercontainer.registries.*;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.networking.ReleaseRequestPayload;

public class PlayerContainer implements ModInitializer {
	public static final String MOD_ID = "playercontainer";

	public static final Identifier VALID_AREA = identifier("booth");
	public static final Identifier INVALID_AREA = identifier("restrict");

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static boolean sendToAll = false;

	public static final GameRules.Key<GameRules.BooleanRule> RESTRICT_TO_BOOTH =
	GameRuleRegistry.register("playercontainer:restrictToBooth", Category.PLAYER, GameRuleFactory.createBooleanRule(true));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		PayloadTypeRegistry.playS2C().register(ContainerInstancesPayload.ID, ContainerInstancesPayload.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(ReleaseRequestPayload.ID, ReleaseRequestPayload.CODEC);

		Blocks.initialize();
		BlockEntities.initialize();
		ItemComponents.initialize();
		Items.initialize();
		Containers.initialize();
		ItemGroups.initialize();
		Commands.intialize();
		Events.initialize();
		DispenserBehaviour.initialize();

		ServerPlayNetworking.registerGlobalReceiver(ReleaseRequestPayload.ID, (payload, context) -> {
			if (ContainerInstance.players.containsKey(context.player().getGameProfile())) {
				ContainerInstance.releasePlayer(context.player());
			}
		});

	}

	public static void sendCIPtoAll(PlayerManager players) {
		sendToAll = true;
	}

	public static void cleanContainers(PlayerManager players) {
		//LOGGER.info("Cleaning containers");
		Set<UUID> toRemove = new HashSet<UUID>();
		for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
			if (entry.getValue().getPlayerCount() == 0 && !ContainerInstance.playersToRecapture.values().contains(entry.getKey()) && !ContainerInstance.playersToRelease.values().contains(entry.getKey())) {
				// if container does not contain any players and is not currently pending release or recapture
				toRemove.add(entry.getKey());
			}
		}
		for (UUID cont : toRemove) {
			//LOGGER.info("Removing "+cont.toString());
			ContainerInstance.containers.remove(cont);
		}
		sendCIPtoAll(players);
	}

	public static void destroyMissingContainers(PlayerManager players) {
		Set<ContainerInstance<?>> toDestroy = new HashSet<ContainerInstance<?>>();
		for (ContainerInstance<?> ci : ContainerInstance.containers.values()) {
			Area invalidArea = AreaLib.getServerArea(players.getServer(), PlayerContainer.INVALID_AREA);
			if (ci.getWorld() != null) {
				if (ci.getWorld().getGameRules().getBoolean(PlayerContainer.RESTRICT_TO_BOOTH) && invalidArea != null && invalidArea.contains(ci.getWorld(), ci.getBlockPos().toCenterPos())) {
					ci.releaseAll(players, false, ci.getBlockPos());
				}
			}
			ci.getOwner().ifLeft(entity -> {
				if (entity instanceof PlayerEntity owner) {
					ServerPlayerEntity player = players.getPlayer(owner.getUuid());
					if (player != null) {
						PlayerInventory inv = player.getInventory();
						// owning player exists and is online
						boolean found = false;
						if (player.currentScreenHandler != null) {
							ItemStack stack = player.currentScreenHandler.getCursorStack();
							if (!stack.isEmpty() && stack.getItem() instanceof ContainerInstanceHolder<?>) {
								found = true; // pwease don't destroy it :(
							}
						}
						for(int i = 0; i < inv.size(); ++i) {
							ItemStack stack = inv.getStack(i);
							if (!stack.isEmpty() && stack.getItem() instanceof ContainerInstanceHolder<?> containerItem) {
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
			ci.destroy(players, ci.getBlockPos());
		}
	}

	public static Identifier identifier(String id) {
		return Identifier.of(MOD_ID, id);
	}
}