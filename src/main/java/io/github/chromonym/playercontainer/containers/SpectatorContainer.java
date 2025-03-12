package io.github.chromonym.playercontainer.containers;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;

import dev.doublekekse.area_lib.Area;
import dev.doublekekse.area_lib.AreaLib;
import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Portal;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public class SpectatorContainer extends AbstractContainer {

    private final double horizontalRadius;
    private final double verticalRadius;

    public SpectatorContainer(double horizontalRadius, double verticalRadius) {
        this(1, horizontalRadius, verticalRadius);
    }

    public SpectatorContainer(int maxPlayers, double horizontalRadius, double verticalRadius) {
        super(maxPlayers);
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
    }

    public double getHorizontalRadius() {
        return horizontalRadius;
    }

    public double getVerticalRadius() {
        return verticalRadius;
    }

    @Override
    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        player.setSilent(true);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.changeGameMode(GameMode.SPECTATOR);
            ci.getOwner().ifLeft(entity -> {
                if (entity.canBeSpectated(serverPlayer)) {
                    serverPlayer.setCameraEntity(entity);
                } else {
                    serverPlayer.setCameraEntity(null);
                    player.teleportTo(new TeleportTarget((ServerWorld)entity.getWorld(), entity, entity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                    //serverPlayer.teleport((ServerWorld)entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
                }
            }).ifRight(blockEntity -> {
                if (blockEntity != null) {
                    moveToNewBlockEntity(serverPlayer, blockEntity);
                    //Vec3d blockPos = blockEntity.getPos().toCenterPos();
                    //serverPlayer.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-1, blockPos.getZ(), player.getYaw(), player.getPitch());
                }
            });
        }
    }

    @Override
    public void onRelease(PlayerEntity player, ContainerInstance<?> ci, BlockPos pos) {
        player.setSilent(false);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.teleportTo(new TeleportTarget(serverPlayer.getServerWorld(), pos.toBottomCenterPos(), Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP));
            serverPlayer.changeGameMode(serverPlayer.getServer().getDefaultGameMode());
        }
    }

    @Override
    public void onTempRelease(PlayerEntity player, ContainerInstance<?> ci, BlockPos pos) {
        onRelease(player, ci, pos);
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
                            moveToNewBlockEntity(player, blockEntity);
                            //Vec3d blockPos = blockEntity.getPos().toCenterPos();
                            //player.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-player.getEyeHeight(player.getPose())+0.5, blockPos.getZ(), player.getYaw(), player.getPitch());
                        }
                        oldOwner.ifRight(oldBlockEntity -> {
                            if (player != null && oldBlockEntity != null) {
                                // if player is inside old block entity
                                moveToNewBlockEntity(player, blockEntity);
                            }
                        });
                }
            }).ifLeft(entity -> {
                ServerPlayerEntity player = entity.getServer().getPlayerManager().getPlayer(profile.getId());
                oldOwner.ifRight(blockEntity -> {
                    if (player != null && player.isSpectator() && entity.canBeSpectated(player)) { // && (blockEntity == null || isWithinBlock(player.getEyePos(), blockEntity.getPos()))
                        // if blockEntity -> entity (entity -> entity is alredy handled by onPlayerTick)
                        player.setCameraEntity(entity);
                    }
                }).ifLeft(newEntity -> {
                    if (player != null && player.getCameraEntity() != null && player.getCameraEntity() != player) {
                        player.setCameraEntity(newEntity);
                    }
                });
            });
        }
    }

    @Override
    public void onDestroy(ContainerInstance<?> ci, BlockPos pos) {
        // Does nothing (releases players anyway)
    }

    @Override
    public void onPlayerTick(ServerPlayerEntity player, ContainerInstance<?> ci) {
        Area invalidArea = AreaLib.getServerArea(player.getServer(), PlayerContainer.INVALID_AREA);
        if (ci.getBlockPos().getY() < player.getWorld().getBottomY() // container is in the void
        || player.getWorld().getBlockState(ci.getBlockPos()).getBlock() instanceof Portal // container is inside a portal block
        || (player.getWorld().getGameRules().getBoolean(PlayerContainer.RESTRICT_TO_BOOTH) && invalidArea != null && invalidArea.contains(player.getWorld(), ci.getBlockPos().toCenterPos()))) {
            // container is in a custom invalid area
            ci.release(player, false, ci.getBlockPos(), true);
        }
        if (player.isSpectator()) {
            //ci.getOwner().left().ifPresent(ent -> PlayerContainer.LOGGER.info(ent.getUuidAsString()));
            //ci.getOwner().right().ifPresent(ent -> PlayerContainer.LOGGER.info(ent.getType().toString()));
            ci.getOwner().ifLeft(entity -> {
                if (player.getCameraEntity() == null || player.getCameraEntity() == player) {
                    // not spectating anything - restrict distance
                    restrictDistance(player, entity.getPos(), entity.getWorld());
                } else {
                    ServerPlayerEntity tempPlayer;
                    if (player.getWorld() != entity.getWorld()) {
                        tempPlayer = restrictDistance(player, entity.getPos(), entity.getWorld());
                    } else {
                        tempPlayer = player;
                    }
                    if (tempPlayer.getCameraEntity() != entity) {
                        if (entity.canBeSpectated(tempPlayer)) {
                            tempPlayer.setCameraEntity(entity);
                        } else {
                            tempPlayer.setCameraEntity(null);
                        }
                    }
                }
            }).ifRight(blockEntity -> {
                if (blockEntity != null) {
                    // && blockEntity.getWorld().getBlockEntity(blockEntity.getPos()) != null && blockEntity.getWorld().getBlockEntity(blockEntity.getPos()).getType() == blockEntity.getType()
                    if (player.getCameraEntity() != null && player.getCameraEntity() != player) {
                        // if owner is a block entity but player is trying to spectate something
                        moveToNewBlockEntity(player, blockEntity);
                    }
                    restrictDistance(player, blockEntity.getPos().toCenterPos(), blockEntity.getWorld());
                } else {
                    PlayerContainer.LOGGER.warn("Container owner could not be found, releasing players");
                    ci.release(player, false, ci.getBlockPos(), true);
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

    public ServerPlayerEntity restrictDistance(ServerPlayerEntity player, Vec3d target, World targetWorld) {
        boolean shouldTeleport = false;
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        if (player.getWorld() != targetWorld) {
            shouldTeleport = true;
            playerX = target.getX();
            playerY = target.getY();
            playerZ = target.getZ();
        } else {
            if (playerX > target.getX() + horizontalRadius + 1) {
                playerX = target.getX() + horizontalRadius - 1;
                shouldTeleport = true;
            } else if (playerX < target.getX() - horizontalRadius - 1) {
                playerX = target.getX() - horizontalRadius + 1;
                shouldTeleport = true;
            }
    
            if (playerY > target.getY() + verticalRadius + 1) {
                playerY = target.getY() + verticalRadius - 1;
                shouldTeleport = true;
            } else if (playerY < target.getY() - verticalRadius - 1) {
                playerY = target.getY() - verticalRadius + 1;
                shouldTeleport = true;
            }
    
            if (playerZ > target.getZ() + horizontalRadius + 1) {
                playerZ = target.getZ() + horizontalRadius - 1;
                shouldTeleport = true;
            } else if (playerZ < target.getZ() - horizontalRadius - 1) {
                playerZ = target.getZ() - horizontalRadius + 1;
                shouldTeleport = true;
            }
        }

        if (shouldTeleport && targetWorld instanceof ServerWorld sw) {
            return (ServerPlayerEntity)player.teleportTo(new TeleportTarget(sw, new Vec3d(playerX, playerY, playerZ), Vec3d.ZERO, player.getYaw(), player.getPitch(), targetWorld == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            //player.teleport(sw, playerX, playerY, playerZ, PositionFlag.VALUES, player.getYaw(), player.getPitch());
        }
        return player;
    }

    public static boolean isCameraSafe(BlockView view, BlockPos pos) {
        return isCameraSafe(view.getBlockState(pos));
    }

    public static boolean isCameraSafe(BlockState state) {
		return (!state.isOpaque() || state.getRenderType() != BlockRenderType.MODEL);
	}

    public static void moveToNewBlockEntity(ServerPlayerEntity player, BlockEntity blockEntity) {
        player.setCameraEntity(null);
        if (isCameraSafe(player.getWorld(), blockEntity.getPos()) && isCameraSafe(player.getWorld(), blockEntity.getPos().add(0, -1, 0))) {
            player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos().add(0, -player.getEyeHeight(player.getPose()), 0), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            //player.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-player.getEyeHeight(player.getPose()), blockPos.getZ(), player.getYaw(), player.getPitch());
        } else if (isCameraSafe(player.getWorld(), blockEntity.getPos()) && isCameraSafe(player.getWorld(), blockEntity.getPos().add(0, 1, 0))) {
            player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toBottomCenterPos(), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
        }
    }
    
}
