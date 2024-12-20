package io.github.chromonym.playercontainer.containers;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

    @Override
    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
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
                serverPlayer.setCameraEntity(null);
                player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos().add(0, -player.getEyeHeight(player.getPose()), 0), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                //Vec3d blockPos = blockEntity.getPos().toCenterPos();
                //serverPlayer.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-1, blockPos.getZ(), player.getYaw(), player.getPitch());
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
                            player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos().add(0, -player.getEyeHeight(player.getPose()), 0), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                            //Vec3d blockPos = blockEntity.getPos().toCenterPos();
                            //player.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-player.getEyeHeight(player.getPose())+0.5, blockPos.getZ(), player.getYaw(), player.getPitch());
                        }
                        oldOwner.ifRight(oldBlockEntity -> {
                            if (oldBlockEntity != null && isWithinBlock(player.getEyePos(), oldBlockEntity.getPos())) {
                                player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos().add(0, -player.getEyeHeight(player.getPose()), 0), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                            }
                        });
                }
            }).ifLeft(entity -> {
                ServerPlayerEntity player = entity.getServer().getPlayerManager().getPlayer(profile.getId());
                if (player.getWorld() != entity.getWorld()) {
                    player.teleportTo(new TeleportTarget((ServerWorld)entity.getWorld(), entity, entity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                    //player.teleport((ServerWorld)entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
                }
                oldOwner.ifRight(blockEntity -> {
                    if (player != null && player.isSpectator() && entity.canBeSpectated(player) && (blockEntity == null || isWithinBlock(player.getEyePos(), blockEntity.getPos()))) {
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
                    restrictDistance(player, entity.getPos(), entity.getWorld());
                } else {
                    if (player.getWorld() != entity.getWorld()) {
                        restrictDistance(player, entity.getPos(), entity.getWorld());
                    }
                    if (player.getCameraEntity() != entity) {
                        if (entity.canBeSpectated(player)) {
                            player.setCameraEntity(entity);
                        } else {
                            player.setCameraEntity(null);
                        }
                    }
                }
            }).ifRight(blockEntity -> {
                if (player.getCameraEntity() != null && player.getCameraEntity() != player) {
                    player.setCameraEntity(null);
                    player.teleportTo(new TeleportTarget((ServerWorld)blockEntity.getWorld(), blockEntity.getPos().toCenterPos().add(0, -player.getEyeHeight(player.getPose()), 0), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                    //player.teleport((ServerWorld)blockEntity.getWorld(), blockPos.getX(), blockPos.getY()-player.getEyeHeight(player.getPose()), blockPos.getZ(), player.getYaw(), player.getPitch());
                }
                restrictDistance(player, blockEntity.getPos().toCenterPos(), blockEntity.getWorld());
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

    public void restrictDistance(PlayerEntity player, Vec3d target, World targetWorld) {
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
            if (playerX > target.getX() + horizontalRadius) {
                playerX = target.getX() + horizontalRadius;
                shouldTeleport = true;
            } else if (playerX < target.getX() - horizontalRadius) {
                playerX = target.getX() - horizontalRadius;
                shouldTeleport = true;
            }
    
            if (playerY > target.getY() + verticalRadius) {
                playerY = target.getY() + verticalRadius;
                shouldTeleport = true;
            } else if (playerY < target.getY() - verticalRadius) {
                playerY = target.getY() - verticalRadius;
                shouldTeleport = true;
            }
    
            if (playerZ > target.getZ() + horizontalRadius) {
                playerZ = target.getZ() + horizontalRadius;
                shouldTeleport = true;
            } else if (playerZ < target.getZ() - horizontalRadius) {
                playerZ = target.getZ() - horizontalRadius;
                shouldTeleport = true;
            }
        }

        if (shouldTeleport && targetWorld instanceof ServerWorld sw) {
            player.teleportTo(new TeleportTarget(sw, new Vec3d(playerX, playerY, playerZ), Vec3d.ZERO, player.getYaw(), player.getPitch(), targetWorld == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            //player.teleport(sw, playerX, playerY, playerZ, PositionFlag.VALUES, player.getYaw(), player.getPitch());
        }
    }

    public static boolean isWithinBlock(Vec3d eyePos, BlockPos blockPos) {
        Vec3d blockVec = blockPos.toCenterPos();
        return Math.abs(blockVec.getX() - eyePos.getX()) <= 0.5 && 
            Math.abs(blockVec.getY() - eyePos.getY()) <= 0.5 && 
            Math.abs(blockVec.getZ() - eyePos.getZ()) <= 0.5;
    }
    
}
