package io.github.chromonym.playercontainer.containers;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.entity.player.PlayerEntity;

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
        PlayerContainer.LOGGER.info("Captured "+player.getNameForScoreboard()+" in container "+Integer.toString(ci.getID()));
        //((ServerWorld)player.getWorld()).getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_INVENTORIES);
    }

    public void onRelease(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Released "+player.getNameForScoreboard()+" in container "+Integer.toString(ci.getID()));
    }

    public void onReleaseAll(ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Released all players from container "+Integer.toString(ci.getID()));
    }

    public void onDestroy(ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Destroyed container "+Integer.toString(ci.getID()));
    }
}
