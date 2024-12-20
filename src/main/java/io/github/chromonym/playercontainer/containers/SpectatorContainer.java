package io.github.chromonym.playercontainer.containers;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class SpectatorContainer extends AbstractContainer {

    private final double distance;

    public SpectatorContainer(double distance) {
        this(1, distance);
    }

    public SpectatorContainer(int maxPlayers, double distance) {
        super(maxPlayers);
        this.distance = distance;
    }

    @Override
    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.changeGameMode(GameMode.SPECTATOR);
            ci.getOwner().ifLeft(entity -> {
                if (entity.canBeSpectated(serverPlayer)) {
                    serverPlayer.setCameraEntity(entity);
                } else {
                    serverPlayer.setCameraEntity(null);
                    serverPlayer.teleport((ServerWorld)entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
                }
            }).ifRight(blockEntity -> {
                serverPlayer.setCameraEntity(null);
                Vec3d blockPos = blockEntity.getPos().toCenterPos();
                serverPlayer.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-1, blockPos.getZ(), player.getYaw(), player.getPitch());
            });
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
        for (GameProfile profile : ci.getPlayers()) {
            newOwner.ifRight(blockEntity -> {
                if (!blockEntity.getWorld().isClient()) {
                        ServerPlayerEntity player = blockEntity.getWorld().getServer().getPlayerManager().getPlayer(profile.getId());
                        if (player != null && player.isSpectator() && player.getCameraEntity() != null && player.getCameraEntity() != player) {
                            // if a player is in spectator and spectating something (entity -> blockEntity or blockEntity -> blockEntity)
                            player.setCameraEntity(null);
                            Vec3d blockPos = blockEntity.getPos().toCenterPos();
                            player.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-1, blockPos.getZ(), player.getYaw(), player.getPitch());
                        }
                }
            }).ifLeft(entity -> {
                ServerPlayerEntity player = entity.getServer().getPlayerManager().getPlayer(profile.getId());
                if (player.getWorld() != entity.getWorld()) {
                    player.teleport((ServerWorld)entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
                }
                oldOwner.ifRight(blockEntity -> {
                    if (player != null && player.isSpectator() && entity.canBeSpectated(player)) {
                        // if blockEntity -> entity (entity -> entity is alredy handled by onPlayerTick)
                        player.setCameraEntity(entity);
                    }
                });
            });
        }
    }

    @Override
    public void onDestroy(ContainerInstance<?> ci) {
        // Does nothing (releases players anyway)
    }

    @Override
    public void onPlayerTick(ServerPlayerEntity player, ContainerInstance<?> ci) {
        if (player.isSpectator()) {
            ci.getOwner().ifLeft(entity -> {
                if (player.getCameraEntity() == null || player.getCameraEntity() == player) {
                    // not spectating anything - restrict distance
                    if (!player.getPos().isInRange(entity.getPos(), distance)) {
                        PlayerContainer.LOGGER.info("Player is outside distance");
                    }
                } else if (player.getCameraEntity() != entity) {
                    if (entity.canBeSpectated(player)) {
                        player.setCameraEntity(entity);
                    } else {
                        player.setCameraEntity(null);
                    }
                }
            }).ifRight(blockEntity -> {
                if (player.getCameraEntity() != null && player.getCameraEntity() != player) {
                    player.setCameraEntity(null);
                }
                if (!player.getPos().isInRange(blockEntity.getPos().toCenterPos(), distance)) {
                    PlayerContainer.LOGGER.info("Player is outside distance");
                }
            });
            /*ci.getOwner().ifLeft(entity -> {
                if (entity.canBeSpectated(player)) { // will return true *most* of the time (not if owner is in spectator themselves, etc)
                    if (player.getCameraEntity() != entity) {
                        player.setCameraEntity(entity);
                    }
                } else {
                    player.teleportTo(new TeleportTarget((ServerWorld)entity.getWorld(), entity, entity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                }
            }).ifRight(blockEntity -> {
                player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos(), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            });*/
        }
    }
    
}
