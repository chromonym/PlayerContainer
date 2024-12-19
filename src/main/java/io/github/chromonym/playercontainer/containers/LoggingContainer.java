package io.github.chromonym.playercontainer.containers;

import com.mojang.datafixers.util.Either;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class LoggingContainer extends AbstractContainer {

    @Override
    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Properly captured player "+player.getNameForScoreboard());
    }

    @Override
    public void onRelease(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Actually released player "+player.getNameForScoreboard());
    }

    @Override
    public void onTempRelease(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Temporarliy released player "+player.getNameForScoreboard());
    }

    @Override
    public void onTempRecapture(PlayerEntity player, ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Recaptured "+player.getNameForScoreboard());
    }

    @Override
    public void onOwnerChange(Either<Entity, BlockEntity> oldOwner, Either<Entity, BlockEntity> newOwner,
            ContainerInstance<?> ci) {
        if (ci != null) {
            newOwner.ifLeft(entity -> {
                if (!(entity instanceof PlayerEntity pe && pe.getGameProfile() == null)) { // this occurs if a player dies with keepinventory
                    PlayerContainer.LOGGER.info("Container "+ci.getID().toString()+" now owned by "+entity.getNameForScoreboard());
                }
            }).ifRight(blockEntity -> {
                PlayerContainer.LOGGER.info("Container "+ci.getID().toString()+" now owned by "+blockEntity.getPos().toShortString());
            });
        }
    }

    @Override
    public void onDestroy(ContainerInstance<?> ci) {
        PlayerContainer.LOGGER.info("Destroyed container "+ci.getID().toString());
    }
    
}
