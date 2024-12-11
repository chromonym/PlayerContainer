package io.github.chromonym.playercontainer.containers;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.entity.player.PlayerEntity;

public class TestContainer extends AbstractContainer {
    @Override
    public boolean onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Attempted captured with a TestContainer");
        return super.onCapture(player, ci);
    }
}
