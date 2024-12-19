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
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class AbstractContainer {
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

    public final boolean capture(PlayerEntity player, ContainerInstance<?> ci) {
        return capture(player, ci, false);
    }

    public final boolean capture(PlayerEntity player, ContainerInstance<?> ci, boolean recapturing) {
        if (recapturing) {
            onTempRecapture(player, ci);
        }
        if (ci.getOwner().left().isPresent() && ci.getOwner().left().get().getUuid() == player.getUuid()) {
            PlayerContainer.LOGGER.warn("Player "+player.getNameForScoreboard()+" attempted capturing themselves!");
            return false;
        }
        for (GameProfile profile : ci.getPlayers()) {
            if (profile.getId() == player.getUuid() && !recapturing) {
                // attempting to capture player already captured
                PlayerContainer.LOGGER.warn("Player "+profile.getName()+" already captured.");
                return false;
            }
        }
        if (ci.getPlayerCount(player) < this.maxPlayers) {
            if (!recapturing) {
                ContainerInstance.players.put(player.getGameProfile(), ci.getID());
                onCapture(player, ci);
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
        release(player.getGameProfile(), player.getServer().getPlayerManager(), ci, recaptureLater);
    }

    public final void release(GameProfile profile, PlayerManager players, ContainerInstance<?> ci, boolean recaptureLater) {
        PlayerEntity player = players.getPlayer(profile.getId());
        if (recaptureLater) { // if the player has just logged off or container is unavailable (not actually released)
            ContainerInstance.playersToRecapture.put(profile.getId(), ci.getID()); // add them to recapture list
            if (ContainerInstance.players.remove(profile) != null && player != null) { // remove them from this container temporarily
                onTempRelease(player, ci);
            }
        } else if (players.getPlayer(profile.getId()) == null) { // otherwise, if the player is *already* offline,
            // ContainerInstance.playersToRecapture.remove(profile.getId()); // remove them from the recapture list
            ContainerInstance.playersToRelease.put(profile.getId(), ci.getID()); // add them to the to release list
            PlayerContainer.LOGGER.warn("Attempted to release player who is offline!"); // (this shouldn't happen because they shouldn't be captured if offline)
        } else if (ContainerInstance.players.get(profile) == ci.getID()) { // otherwise if the player is online and captured
            ContainerInstance.playersToRecapture.remove(profile.getId());
            onRelease(players.getPlayer(profile.getId()), ci); // run onRelease
            ContainerInstance.players.remove(profile); // remove them from the captured players
        }
    }

    public final void releaseAll(PlayerManager players, ContainerInstance<?> ci) {
        releaseAll(players, ci, false);
    }

    public final void releaseAll(PlayerManager players, ContainerInstance<?> ci, boolean recaptureLater) {
        for (GameProfile profile : ContainerInstance.players.keySet()) {
            release(profile, players, ci, recaptureLater);
        }
        if (!recaptureLater) {
            for (Entry<UUID,UUID> entry : ContainerInstance.playersToRecapture.entrySet()) {
                if (entry.getValue() == ci.getID()) {
                    ContainerInstance.playersToRelease.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public final void destroy(PlayerManager players, ContainerInstance<?> ci) {
        onDestroy(ci);
        releaseAll(players, ci);
    }

    public abstract void onCapture(PlayerEntity player, ContainerInstance<?> ci);
    // Ran whenever a player is captured in the given ContainerInstance

    public abstract void onRelease(PlayerEntity player, ContainerInstance<?> ci);
    // Ran whenever a player is released from the given ContainerInstance

    public abstract void onTempRelease(PlayerEntity player, ContainerInstance<?> ci);
    /* Ran whenever a player disconnects or the container becomes temporarily unavailable.
     * NOTE: DOES NOT RUN on a real release! */

    public abstract void onTempRecapture(PlayerEntity player, ContainerInstance<?> ci);
    /* Ran whenever a player is recaptured after being temporarily released
     * NOTE: DOES NOT RUN on a real capture! */

    public abstract void onOwnerChange(Either<Entity, BlockEntity> oldOwner, Either<Entity, BlockEntity> newOwner, ContainerInstance<?> ci);
    // Ran whenever this container's owner changes

    public abstract void onDestroy(ContainerInstance<?> ci);
    // Ran whenever this container is destroyed (through its owner despawning (NOT unloading) or its item entity being killed)
    // NOTE: onRelease is still ran for all contained players!

    public abstract void onPlayerTick(ServerPlayerEntity player, ContainerInstance<?> ci);
    // Ran every tick in the playerTick function for all captured players
}
