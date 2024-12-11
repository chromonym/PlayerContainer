package io.github.chromonym.playercontainer.containers;

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

    public void onOwnerChange(Either<Entity,BlockEntity> newOwner, ContainerInstance<?> ci) {
        if (ci != null) {
            newOwner.ifLeft(entity -> {
                PlayerContainer.LOGGER.info("Container "+ci.getID().toString()+" now owned by "+entity.getNameForScoreboard());
            }).ifRight(blockEntity -> {
                PlayerContainer.LOGGER.info("Container "+ci.getID().toString()+" now owned by "+blockEntity.getPos().toShortString());
            });
        }
    }

    public boolean onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        if (ci.getOwner().left().isPresent() && ci.getOwner().left().get() == player) {
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
            if (!player.getWorld().isClient()) {
                ServerPlayNetworking.send((ServerPlayerEntity)player, new ContainerInstancesPayload(ContainerInstance.containers, ContainerInstance.players));
            }
            //((ServerWorld)player.getWorld()).getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_INVENTORIES);
            return true;
        }
        return false;
    }
    
    public void onRelease(PlayerEntity player, ContainerInstance<?> ci) {
        onRelease(player.getGameProfile(), player.getWorld(), ci);
    }

    public void onRelease(GameProfile profile, World world, ContainerInstance<?> ci) {
        if (world.getPlayerByUuid(profile.getId()) == null) {
            PlayerContainer.LOGGER.warn("Attempted to release player who is offline!"); // TODO handle this
        }
        if (ContainerInstance.players.get(profile) == ci.getID()) {
            ContainerInstance.players.remove(profile);
            PlayerContainer.LOGGER.info("Released "+profile.getName()+" from container "+ci.getID().toString());
        }
    }

    public void onReleaseAll(World world, ContainerInstance<?> ci) {
        for (GameProfile profile : ContainerInstance.players.keySet()) {
            onRelease(profile, world, ci);
        }
        PlayerContainer.LOGGER.info("Released all players from container "+ci.getID().toString());
    }

    public void onDestroy(World world, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Destroyed container "+ci.getID().toString());
        onReleaseAll(world, ci);
    }
}
