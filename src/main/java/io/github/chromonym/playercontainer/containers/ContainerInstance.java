package io.github.chromonym.playercontainer.containers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.chromonym.playercontainer.registries.Containers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ContainerInstance<C extends AbstractContainer> {

    public static BiMap<UUID, ContainerInstance<?>> containers = HashBiMap.create();
    public static Map<GameProfile, UUID> players = new HashMap<GameProfile, UUID>(); // PLAYERS TO CONTAINERS!!
    public static Map<UUID, UUID> playersToRecapture = new HashMap<UUID, UUID>(); // players that need to be recaptured by a given container when next possible
    public static Map<UUID, UUID> playersToRelease = new HashMap<UUID, UUID>(); // players that need to be released when next possible
    public static Set<UUID> disconnectedPlayers = new HashSet<UUID>(); // players that have disconnected this tick (should be ignored in checking recap/decap)

    public static final Codec<ContainerInstance<?>> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Containers.REGISTRY.getCodec().fieldOf("container").forGetter(ContainerInstance::getContainer),
            Uuids.CODEC.fieldOf("uuid").forGetter(ContainerInstance::getID)
        ).apply(instance, ContainerInstance::new)
    );

    public static final PacketCodec<PacketByteBuf, ContainerInstance<?>> PACKET_CODEC = PacketCodec.tuple(
        RegistryKey.createPacketCodec(Containers.REGISTRY_KEY), ContainerInstance::getContainerKey,
        Uuids.PACKET_CODEC, ContainerInstance::getID,
        ContainerInstance::new);
    

    private final C container;
    private UUID ID;
    private Entity ownerEntity;
    private BlockEntity ownerBlockEntity;

    public ContainerInstance(C container) {
        this(container, UUID.randomUUID());
    }

    public ContainerInstance(RegistryKey<AbstractContainer> containerKey, UUID id) {
        this((C)Containers.REGISTRY.get(containerKey), id); // surely it's fiiiiiine
    }

    public ContainerInstance(C container, UUID id) {
        this.container = container;
        this.ID = id;
        if (!containers.containsKey(id)) {
            containers.put(id, this);
        }
    }

    public UUID getID() {
        UUID uid = containers.inverse().get(this);
        if (uid == null) {
            return ID;
        } else {
            ID = uid;
            return uid;
        }
    }

    public C getContainer() {
        return container;
    }

    public int getMaxPlayerCount() {
        return container.maxPlayers;
    }

    public Set<GameProfile> getPlayers() {
        Set<GameProfile> containedPlayers = new HashSet<GameProfile>();
        for (Entry<GameProfile, UUID> entry : players.entrySet()) {
            if (entry.getValue() == this.getID()) {
                containedPlayers.add(entry.getKey());
            }
        }
        return containedPlayers;
    }

    public int getPlayerCount() {
        return getPlayerCount(null);
    }

    public int getPlayerCount(@Nullable PlayerEntity player) {
        int containedPlayers = 0;
        for (Entry<GameProfile, UUID> entry : players.entrySet()) {
            if (entry.getValue() == this.getID() && (player == null || player.getUuid() != entry.getKey().getId())) {
                containedPlayers++;
            }
        }
        return containedPlayers;
    }

    public RegistryKey<AbstractContainer> getContainerKey() {
        Optional<RegistryKey<AbstractContainer>> keyOpt = Containers.REGISTRY.getKey(container);
        if (keyOpt.isPresent()) {
            return keyOpt.get();
        } else {
            return null;
        }
    }

    public Either<Entity,BlockEntity> getOwner() {
        if (ownerEntity != null) {
            return Either.left(ownerEntity);
        } else {
            return Either.right(ownerBlockEntity);
        }
    }

    public static void checkRecaptureDecapture(World world) {
        Map<PlayerEntity, ContainerInstance<?>> recaptured = new HashMap<PlayerEntity, ContainerInstance<?>>();
        Map<PlayerEntity, ContainerInstance<?>> released = new HashMap<PlayerEntity, ContainerInstance<?>>();
        for (Entry<UUID, UUID> entry : playersToRecapture.entrySet()) {
            PlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null && !disconnectedPlayers.contains(player.getUuid())) {
                ContainerInstance<?> cont = containers.get(entry.getValue());
                cont.getOwner().ifLeft(entity -> {
                    if (world.getEntityById(entity.getId()) != null) {
                        // both owner entity and player to recapture exist
                        recaptured.put(player, cont);
                        //cont.capture(player, true);
                    }
                }).ifRight(blockEntity -> {
                    BlockPos pos = blockEntity.getPos();
                    if (world.isPosLoaded(pos.getX(), pos.getZ())) {
                        recaptured.put(player, cont);
                        //cont.capture(player, true);
                    }
                });
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : recaptured.entrySet()) {
            if (!entry.getValue().capture(entry.getKey(), true)) {
                entry.getValue().release(entry.getKey(), false);
            }
            ContainerInstance.playersToRecapture.remove(entry.getKey().getUuid());
        }
        for (Entry<UUID, UUID> entry : playersToRelease.entrySet()) {
            PlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                ContainerInstance<?> cont = containers.get(entry.getValue());
                cont.getOwner().ifLeft(entity -> {
                    if (world.getEntityById(entity.getId()) != null) {
                        // both owner entity and player to recapture exist
                        released.put(player, cont);
                        //cont.release(player, false);
                    }
                }).ifRight(blockEntity -> {
                    BlockPos pos = blockEntity.getPos();
                    if (world.isPosLoaded(pos.getX(), pos.getZ())) {
                        released.put(player, cont);
                        //cont.release(player, false);
                    }
                });
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : released.entrySet()) {
            entry.getValue().release(entry.getKey(), false);
            ContainerInstance.playersToRelease.remove(entry.getKey().getUuid());
        }
    }

    public static void releasePlayer(PlayerEntity player) {
        for (GameProfile profile : ContainerInstance.players.keySet()) {
            if (profile.getId() == player.getUuid()) {
                UUID contID = players.get(profile);
                if (contID != null && containers.containsKey(contID)) {
                    ContainerInstance<?> ci = containers.get(contID);
                    ci.release(player, false);
                }
            }
        }
        for (Entry<UUID,UUID> entry : ContainerInstance.playersToRecapture.entrySet()) {
            if (entry.getKey() == player.getUuid()) {
                ContainerInstance.playersToRelease.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void setOwner(Entity entity) {
        if (entity != ownerEntity) {
            container.onOwnerChange(getOwner(), Either.left(entity), this);
            ownerBlockEntity = null;
            ownerEntity = entity;
        }
    }

    public void setOwner(BlockEntity blockEntity) {
        if (blockEntity != ownerBlockEntity) {
            container.onOwnerChange(getOwner(), Either.right(blockEntity), this);
            ownerBlockEntity = blockEntity;
            ownerEntity = null;
        }
    }

    // write methods in here that call the relevant AbstractContainer method

    public boolean capture(PlayerEntity player, boolean temp) {
        return container.capture(player, this, temp);
    }

    public void release(PlayerEntity player, boolean temp) {
        container.release(player, this, temp);
    }

    public void releaseAll(PlayerManager players, boolean temp) {
        container.releaseAll(players, this, temp);
    }

    public void destroy(PlayerManager players) {
        containers.remove(ID);
        container.destroy(players, this);
    }

    public void onPlayerTick(ServerPlayerEntity player) {
        container.onPlayerTick(player, this);
    }
}
