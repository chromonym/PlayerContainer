package io.github.chromonym.playercontainer.registries;

import java.util.HashSet;
import java.util.Map.Entry;

import dev.doublekekse.area_lib.Area;
import dev.doublekekse.area_lib.AreaLib;

import java.util.Set;
import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.networking.ContainerPersistentState;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class Events {
    
    public static void initialize() {
		/*ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			ContainerInstance.checkRecaptureDecapture(world);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ContainerInstance.checkRecaptureDecapture(handler.getPlayer().getWorld());
		});*/
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ContainerPersistentState.getServerState(server).updateToCI(); // copy the saved containerinstances to the server's CI
        });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            ContainerPersistentState.getServerState(server).updateFromCI(); // copy the server's CI to the saved containerinstances
        });
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			ContainerInstance.checkRecaptureDecapture(world);
			ContainerInstance.disconnectedPlayers.clear();
            if (PlayerContainer.sendToAll) {
                for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, new ContainerInstancesPayload(ContainerInstance.containers, ContainerInstance.players));
                }
            }
            PlayerContainer.sendToAll = false;
		});
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            Area area = AreaLib.getServerArea(entity.getServer(), PlayerContainer.VALID_AREA);
            if (entity instanceof ServerPlayerEntity player &&
            (!entity.getWorld().getGameRules().getBoolean(PlayerContainer.RESTRICT_TO_BOOTH) || (area != null && area.contains(entity)))) {
                ItemStack stack = Items.playerEssence.getDefaultStack();
                String str = player.getNameForScoreboard();
                stack.set(ItemComponents.PLAYER_NAME, str.substring(0, 1).toUpperCase() + str.substring(1));
                ItemEntity itemEntity = new ItemEntity(player.getWorld(), player.getX(), player.getY(), player.getZ(), stack);
                itemEntity.setToDefaultPickupDelay();
                entity.getWorld().spawnEntity(itemEntity);
            }
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
                ContainerInstance<?> ci = ContainerInstance.containers.get(ContainerInstance.players.get(player.getGameProfile())); // temporarily release that player
                if (ci != null) {
                    ci.release(player, false, ci.getBlockPos(), true); // FULL RELEASE TEMPORARY FOR BC25 VERSION!!
                }
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
				cont.releaseAll(world.getServer().getPlayerManager(), true, blockEntity.getPos());
			}
		});
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof Inventory inv) {
                if (blockEntity instanceof LootableInventory linv && linv.getLootTable() != null) {
                    return;
                }
                for(int i = 0; i < inv.size(); ++i) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ContainerInstanceHolder<?> containerItem) {
                        containerItem.getOrMakeContainerInstance(stack, blockEntity.getWorld()).setOwner(blockEntity);
                    }
                }
            }
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            Set<UUID> toRemove = new HashSet<UUID>();
            for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
                if (entry.getValue().getContainer() instanceof SpectatorContainer) {
                    entry.getValue().getOwner().ifLeft(owner -> {
                        if (owner != null && owner.getUuid() == player.getUuid()) {
                            /*for (GameProfile pe : entry.getValue().getPlayers()) {
                                ServerPlayerEntity capturedPlayer = destination.getServer().getPlayerManager().getPlayer(pe.getId());
                                if (capturedPlayer != null && capturedPlayer.isSpectator() && capturedPlayer.getCameraEntity() != null && capturedPlayer.getCameraEntity() != capturedPlayer) {
                                    capturedPlayer.setCameraEntity(player);
                                }
                            }*/
                            toRemove.add(entry.getValue().getID());
                        }
                    });
                }
            }
            for (UUID uuid : toRemove) {
                ContainerInstance.containers.get(uuid).destroy(player.getServer().getPlayerManager(), ContainerInstance.containers.get(uuid).getCachedBlockPos());
            }
        });
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((originalEntity, newEntity, origin, destination) -> {
            Set<UUID> toRemove = new HashSet<UUID>();
            for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
                if (entry.getValue().getContainer() instanceof SpectatorContainer) {
                    entry.getValue().getOwner().ifLeft(owner -> {
                        if (owner != null && owner.getUuid() == originalEntity.getUuid()) {
                            toRemove.add(entry.getValue().getID());
                        }
                    });
                }
            }
            for (UUID uuid : toRemove) {
                ContainerInstance.containers.get(uuid).destroy(newEntity.getServer().getPlayerManager(), originalEntity.getBlockPos());
            }
        });
    }

}
