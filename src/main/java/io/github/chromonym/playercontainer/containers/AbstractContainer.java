package io.github.chromonym.playercontainer.containers;

import java.util.UUID;
import java.util.Map.Entry;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class AbstractContainer {
    /*
     * an abstract container:
     * - has an id
     * - IS NOT AN ITEM(stack)
     * - has the ability to Do Things on capture and on release
     */

    public final int maxPlayers;

    public AbstractContainer() {
        this(1);
    }

    public AbstractContainer(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public final void setOwner(Either<Entity,BlockEntity> newOwner, ContainerInstance<?> ci) {
        if (ci != null) {
            newOwner.ifLeft(entity -> {
                PlayerContainer.LOGGER.info("Container "+ci.getID().toString()+" now owned by "+entity.getNameForScoreboard());
            }).ifRight(blockEntity -> {
                PlayerContainer.LOGGER.info("Container "+ci.getID().toString()+" now owned by "+blockEntity.getPos().toShortString());
            });
        }
    }

    public final boolean capture(PlayerEntity player, ContainerInstance<?> ci) {
        return capture(player, ci, false);
    }

    public final boolean capture(PlayerEntity player, ContainerInstance<?> ci, boolean recapturing) {
        if (ci.getOwner().left().isPresent() && ci.getOwner().left().get().getUuid() == player.getUuid()) {
            PlayerContainer.LOGGER.warn("Player "+player.getNameForScoreboard()+" attempted capturing themselves!");
            return false;
        }
        for (GameProfile profile : ci.getPlayers(player.getWorld())) {
            if (profile.getId() == player.getUuid()) {
                // attempting to capture player already captured
                PlayerContainer.LOGGER.info("Player "+profile.getName()+" already captured.");
                return false;
            }
        }
        if (ci.getPlayerCount() < this.maxPlayers) {
            PlayerContainer.LOGGER.info("Captured "+player.getNameForScoreboard()+" in container "+ci.getID().toString());
            ContainerInstance.players.put(player.getGameProfile(), ci.getID());
            if (!recapturing) { // if not attempting to recapture a player that was offline
                this.onCapture(player, ci); // run onCapture (still do everything else the same tho)
            }
            if (!player.getWorld().isClient()) {
                ServerPlayNetworking.send((ServerPlayerEntity)player, new ContainerInstancesPayload(ContainerInstance.containers, ContainerInstance.players));
            }
            //((ServerWorld)player.getWorld()).getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_INVENTORIES);
            return true;
        }
        return false;
    }

    public final void release(PlayerEntity player, ContainerInstance<?> ci) {
        release(player, ci, false);
    }
    
    public final void release(PlayerEntity player, ContainerInstance<?> ci, boolean recaptureLater) {
        release(player.getGameProfile(), player.getWorld(), ci, recaptureLater);
    }

    public final void release(GameProfile profile, World world, ContainerInstance<?> ci, boolean recaptureLater) {
        if (recaptureLater) { // if the player has just logged off (not actually released)
            ContainerInstance.playersToRecapture.put(profile.getId(), ci.getID()); // add them to recapture list
            ContainerInstance.players.remove(profile); // remove them from this container temporarily
        } else if (world.getPlayerByUuid(profile.getId()) == null) { // otherwise, if the player is *already* offline,
            ContainerInstance.playersToRecapture.remove(profile.getId()); // remove them from the recapture list
            ContainerInstance.playersToRelease.put(profile.getId(), ci.getID()); // add them to the to release list
            PlayerContainer.LOGGER.warn("Attempted to release player who is offline!"); // (this shouldn't happen because they shouldn't be captured if offline)
        } else if (ContainerInstance.players.get(profile) == ci.getID()) { // otherwise if the player is online and captured
            ContainerInstance.playersToRecapture.remove(profile.getId());
            onRelease(world.getPlayerByUuid(profile.getId()), ci); // run onRelease
            ContainerInstance.players.remove(profile); // remove them from the captured players
            PlayerContainer.LOGGER.info("Released "+profile.getName()+" from container "+ci.getID().toString()); // log
        }
    }

    public final void releaseAll(World world, ContainerInstance<?> ci) {
        releaseAll(world, ci, false);
    }

    public final void releaseAll(World world, ContainerInstance<?> ci, boolean recaptureLater) {
        for (GameProfile profile : ContainerInstance.players.keySet()) {
            release(profile, world, ci, recaptureLater);
        }
        for (Entry<UUID,UUID> entry : ContainerInstance.playersToRecapture.entrySet()) {
            if (entry.getValue() == ci.getID()) {
                ContainerInstance.playersToRelease.put(entry.getKey(), ci.getID());
            }
        }
        PlayerContainer.LOGGER.info("Released all players from container "+ci.getID().toString());
    }

    public final void destroy(World world, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Destroyed container "+ci.getID().toString());
        releaseAll(world, ci);
    }

    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Actually captured player "+player.getNameForScoreboard());
    }

    public void onRelease(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Actually released player "+player.getNameForScoreboard());
    }
}
