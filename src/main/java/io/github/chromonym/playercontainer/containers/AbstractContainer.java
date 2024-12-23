package io.github.chromonym.playercontainer.containers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;

import io.github.chromonym.playercontainer.PlayerContainer;
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
        Optional<Entity> thisOwner = ci.getOwner().left();
        GameProfile toRemove = null;
        UUID reAdd = null;
        for (GameProfile prof : ContainerInstance.players.keySet()) {
            if (prof.getId() == player.getUuid()) { // hopefully this should deal with people changing their username??? hopefully(tm)
                reAdd = ContainerInstance.players.get(prof);
                toRemove = prof;
                break;
            }
        }
        if (reAdd != null && toRemove != null) {
            ContainerInstance.players.remove(toRemove);
            ContainerInstance.players.put(player.getGameProfile(), reAdd);
        }
        if (recapturing) {
            onTempRecapture(player, ci);
            if (!player.getWorld().isClient()) {
                PlayerContainer.sendCIPtoAll(player.getServer().getPlayerManager());
            }
            return true;
        }
        for (GameProfile capturedPlayer : ContainerInstance.players.keySet()) {
            if (thisOwner.isPresent() && thisOwner.get().getUuid() == capturedPlayer.getId()) {
                // if attempting to capture someone while you yourself are captured
                return false;
            }
        }
        for (ContainerInstance<?> cont : ContainerInstance.containers.values()) {
            Optional<Entity> contOwner = cont.getOwner().left();
            if (cont.getPlayerCount() > 0 && contOwner.isPresent() && contOwner.get().getUuid() == player.getUuid()) {
                // if attempting to capture player that are themselves an owner
                return false;
            }
        }
        if (ContainerInstance.players.containsKey(player.getGameProfile()) && ContainerInstance.players.get(player.getGameProfile()) != ci.getID() && !recapturing) {
            // player is already captured
            ContainerInstance.containers.get(ContainerInstance.players.get(player.getGameProfile())).release(player, false);
        }
        if (ContainerInstance.playersToRecapture.containsKey(player.getUuid()) && ContainerInstance.playersToRecapture.get(player.getUuid()) != ci.getID()) {
            // player is already captured but offline
            PlayerContainer.LOGGER.warn("Unsuccessfully attempted to capture a player who is offline *and* already caught");
            return false;
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
        if (ci.getPlayerCount(player) < this.maxPlayers || recapturing) {
            ContainerInstance.players.put(player.getGameProfile(), ci.getID());
            onCapture(player, ci);
            if (!player.getWorld().isClient()) {
                PlayerContainer.sendCIPtoAll(player.getServer().getPlayerManager());
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
            if (ContainerInstance.players.containsKey(profile) && player != null) { // remove them from this container temporarily
                onTempRelease(player, ci);
                if (!player.getWorld().isClient()) {
                    PlayerContainer.sendCIPtoAll(player.getServer().getPlayerManager());
                }
            }
        } else if (players.getPlayer(profile.getId()) == null) { // otherwise, if the player is *already* offline, DO NOTHING
            // ContainerInstance.playersToRecapture.remove(profile.getId()); // remove them from the recapture list
            //ContainerInstance.playersToRelease.put(profile.getId(), ci.getID()); // add them to the to release list
            //PlayerContainer.LOGGER.warn("Attempted to release player who is offline!"); // (this shouldn't happen because they shouldn't be captured if offline)
            return;
        } else if (ContainerInstance.players.get(profile) == ci.getID()) { // otherwise if the player is online and captured
            if (ContainerInstance.playersToRecapture.containsKey(profile.getId())) {
                // if they haven't been recaptured yet, do that first!
                ContainerInstance.playersToRelease.put(profile.getId(), ci.getID());
            } else {
                onRelease(players.getPlayer(profile.getId()), ci); // run onRelease
                ContainerInstance.players.remove(profile); // remove them from the captured players
                if (!player.getWorld().isClient()) {
                    PlayerContainer.sendCIPtoAll(player.getServer().getPlayerManager());
                }
            }
        }
    }

    public final void releaseAll(PlayerManager players, ContainerInstance<?> ci) {
        releaseAll(players, ci, false);
    }

    public final void releaseAll(PlayerManager players, ContainerInstance<?> ci, boolean recaptureLater) {
        Set<GameProfile> toRelease = new HashSet<GameProfile>();
        for (GameProfile profile : ContainerInstance.players.keySet()) {
            toRelease.add(profile);
            //release(profile, players, ci, recaptureLater);
        }
        for (GameProfile profile : toRelease) {
            release(profile, players, ci, recaptureLater);
        }
        /*if (!recaptureLater) {
            for (Entry<UUID,UUID> entry : ContainerInstance.playersToRecapture.entrySet()) {
                if (entry.getValue() == ci.getID()) {
                    ContainerInstance.playersToRelease.put(entry.getKey(), entry.getValue());
                }
            }
        }*/
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
