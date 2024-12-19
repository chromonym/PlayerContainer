package io.github.chromonym.playercontainer.containers;

import com.mojang.datafixers.util.Either;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

public class SpectatorContainer extends AbstractContainer {

    @Override
    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.changeGameMode(GameMode.SPECTATOR);
        }
    }

    @Override
    public void onRelease(PlayerEntity player, ContainerInstance<?> ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.changeGameMode(serverPlayer.getServer().getDefaultGameMode());
        }
    }

    @Override
    public void onTempRelease(PlayerEntity player, ContainerInstance<?> ci) {
        onRelease(player, ci);
    }

    @Override
    public void onTempRecapture(PlayerEntity player, ContainerInstance<?> ci) {
        onCapture(player, ci);
    }

    @Override
    public void onOwnerChange(Either<Entity, BlockEntity> oldOwner, Either<Entity, BlockEntity> newOwner,
            ContainerInstance<?> ci) {
        // Does nothing (handled by onPlayerTick)
    }

    @Override
    public void onDestroy(ContainerInstance<?> ci) {
        // Does nothing (releases players anyway)
    }

    @Override
    public void onPlayerTick(ServerPlayerEntity player, ContainerInstance<?> ci) {
        if (player.isSpectator()) {
            ci.getOwner().ifLeft(entity -> {
                if (entity.canBeSpectated(player)) { // will return true *most* of the time (not if owner is in spectator themselves, etc)
                    if (player.getCameraEntity() != entity) {
                        player.setCameraEntity(entity);
                    }
                } else {
                    player.teleportTo(new TeleportTarget((ServerWorld)entity.getWorld(), entity, entity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                }
            }).ifRight(blockEntity -> {
                player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos(), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            });
        }
    }
    
}
