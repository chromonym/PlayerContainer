package io.github.chromonym.playercontainer.containers;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class AbstractContainer {
    /*
     * an abstract container:
     * - has an id
     * - IS NOT AN ITEM(stack)
     * - has the ability to Do Things on capture and on release
     */

    public AbstractContainer() {
    }

    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Captured "+player.getNameForScoreboard()+" in container "+ci.getID().toString());
        if (!player.getWorld().isClient()) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, new ContainerInstancesPayload(ContainerInstance.containers));
        }
        //((ServerWorld)player.getWorld()).getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_INVENTORIES);
    }

    public void onRelease(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Released "+player.getNameForScoreboard()+" in container "+ci.getID().toString());
    }

    public void onReleaseAll(ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Released all players from container "+ci.getID().toString());
    }

    public void onDestroy(ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Destroyed container "+ci.getID().toString());
    }
}
