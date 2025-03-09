package io.github.chromonym.playercontainer.containers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.blockentities.CageBlockEntity;
import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

public class CageSpectatorContainer extends SpectatorContainer {

    public static final Identifier CAGE_SCALE_MODIFIER_ID = PlayerContainer.identifier("cage_scale");

    public static Map<UUID, UUID> playersToCage = new HashMap<UUID, UUID>(); // players that need to be set to adventure (small) in a given cage when next possible
    public static Map<UUID, UUID> playersToUncage = new HashMap<UUID, UUID>(); // players that need to be set to spectator (regular size) around a given cage when next possible

    public CageSpectatorContainer(double horizontalRadius, double verticalRadius) {
        super(horizontalRadius, verticalRadius);
    }

    @Override
    public void onPlaceBlock(ContainerInstance<?> ci, ServerWorld serverWorld, BlockPos pos, PlayerManager players) {
        Set<GameProfile> offlinePlayers = new HashSet<GameProfile>();
        ci.getPlayers().forEach(profile -> {
            PlayerEntity player = players.getPlayer(profile.getId());
            if (player != null && player instanceof ServerPlayerEntity serverPlayer) {
                cagePlayer(serverPlayer, pos, serverWorld);
            } else {
                offlinePlayers.add(profile);
            }
        });
        offlinePlayers.forEach(profile -> {
            playersToCage.put(profile.getId(), ci.getID());
            if (!playersToUncage.containsKey(profile.getId())) {
                playersToUncage.remove(profile.getId());
            }
        });
    }

    public void cagePlayer(ServerPlayerEntity serverPlayer, BlockPos pos, ServerWorld serverWorld) {
        serverPlayer.getAttributeInstance(EntityAttributes.GENERIC_SCALE).addPersistentModifier(new EntityAttributeModifier(CAGE_SCALE_MODIFIER_ID, -0.75, Operation.ADD_MULTIPLIED_TOTAL));
        serverPlayer.teleportTo(new TeleportTarget(serverWorld, pos.toBottomCenterPos().add(0, 0.0625, 0), Vec3d.ZERO, serverPlayer.getYaw(), serverPlayer.getPitch(), serverWorld == serverPlayer.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
        serverPlayer.changeGameMode(GameMode.ADVENTURE);
    }

    public static void onBreakBlock(ContainerInstance<?> ci, PlayerManager players) {
        Set<GameProfile> offlinePlayers = new HashSet<GameProfile>();
        ci.getPlayers().forEach(profile -> {
            PlayerEntity player = players.getPlayer(profile.getId());
            if (player != null && player instanceof ServerPlayerEntity serverPlayer) {
                uncagePlayer(serverPlayer);
            } else {
                offlinePlayers.add(profile);
            }
        });
        offlinePlayers.forEach(profile -> {
            playersToUncage.put(profile.getId(), ci.getID());
            if (!playersToCage.containsKey(profile.getId())) {
                playersToCage.remove(profile.getId());
            }
        });
    }

    public static void uncagePlayer(ServerPlayerEntity serverPlayer) {
        serverPlayer.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(CAGE_SCALE_MODIFIER_ID);
        serverPlayer.changeGameMode(GameMode.SPECTATOR);
    }

    @Override
    public void onRelease(PlayerEntity player, ContainerInstance<?> ci, BlockPos pos) {
        player.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(CAGE_SCALE_MODIFIER_ID);
        if (playersToCage.containsKey(player.getUuid())) {
            playersToCage.remove(player.getUuid());
        }
        if (playersToUncage.containsKey(player.getUuid())) {
            playersToUncage.remove(player.getUuid());
        }
        super.onRelease(player, ci, pos);
    }

    @Override
    public void onCapture(PlayerEntity player, ContainerInstance<?> ci) {
        super.onCapture(player, ci);
        ci.getOwner().ifRight(blockEntity -> {
            if (blockEntity instanceof CageBlockEntity && player instanceof ServerPlayerEntity serverPlayer) {
                cagePlayer(serverPlayer, blockEntity.getPos(), (ServerWorld)blockEntity.getWorld());
            }
        });
    }

    @Override
    public void onPlayerTick(ServerPlayerEntity player, ContainerInstance<?> ci) {
        super.onPlayerTick(player, ci);
        if (!player.isSpectator()) {
            ci.getOwner().ifRight(blockEntity -> {
                if (blockEntity instanceof CageBlockEntity && !player.isCreative()) {
                    if (!player.getPos().isWithinRangeOf(blockEntity.getPos().toCenterPos(), 1.0, 1.0)) {
                        player.teleportTo(new TeleportTarget(player.getServerWorld(), blockEntity.getPos().toBottomCenterPos().add(0, 0.0625, 0), Vec3d.ZERO, player.getYaw(), player.getPitch(), blockEntity.getWorld() == player.getWorld() ? TeleportTarget.NO_OP : TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                    }
                    if (!player.getAttributeInstance(EntityAttributes.GENERIC_SCALE).hasModifier(CAGE_SCALE_MODIFIER_ID)) {
                        player.getAttributeInstance(EntityAttributes.GENERIC_SCALE).addPersistentModifier(new EntityAttributeModifier(CAGE_SCALE_MODIFIER_ID, -0.75, Operation.ADD_MULTIPLIED_TOTAL));
                    }
                }
            });
        }
    }
    
}
