package io.github.chromonym.playercontainer.containers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import io.github.chromonym.blockentities.CageBlockEntity;
import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.registries.Containers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
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
            Uuids.CODEC.fieldOf("uuid").forGetter(ContainerInstance::getID),
            Codecs.GAME_PROFILE_WITH_PROPERTIES.listOf().fieldOf("players").forGetter(ContainerInstance::getPlayersForCodec),
            BlockPos.CODEC.fieldOf("cachedPos").forGetter(ContainerInstance::getBlockPos)
        ).apply(instance, ContainerInstance::new)
    );

    public static final PacketCodec<PacketByteBuf, ContainerInstance<?>> PACKET_CODEC = PacketCodec.tuple( // S2C ONLY!!
        RegistryKey.createPacketCodec(Containers.REGISTRY_KEY), ContainerInstance::getContainerKey,
        Uuids.PACKET_CODEC, ContainerInstance::getID,
        PacketCodecs.collection(HashSet::new, PacketCodecs.GAME_PROFILE), ContainerInstance::getCachedPlayers,
        PacketCodecs.either(PacketCodecs.INTEGER, BlockPos.PACKET_CODEC), ContainerInstance::getOwnerDetail,
        ContainerInstance::new);
    

    private final C container;
    private UUID ID;
    private Entity ownerEntity;
    private BlockEntity ownerBlockEntity;
    private BlockPos cachedBlockPos;
    public Set<GameProfile> playerCache = new HashSet<GameProfile>();
    public Integer tempOwnerId; // used for client-side stuff - ignore otherwise

    public ContainerInstance(C container) {
        this(container, UUID.randomUUID());
    }

    public ContainerInstance(RegistryKey<AbstractContainer> containerKey, UUID id, Set<GameProfile> playerCache, Either<Integer,BlockPos> cachedOwner) {
        this((C)Containers.REGISTRY.get(containerKey), id); // surely it's fiiiiiine
        this.playerCache = playerCache;
        cachedOwner.ifLeft(tempID -> {
            this.tempOwnerId = tempID;
        }).ifRight(blockPos -> {
            this.cachedBlockPos = blockPos;
        });
    }

    public ContainerInstance(C container, UUID id, List<GameProfile> players, BlockPos cachedPos) {
        this(container, id);
        this.playerCache = new HashSet<GameProfile>(players);
        this.cachedBlockPos = cachedPos;
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
            if (ID == null) {
                return UUID.fromString("00000000-0000-0000-0000-000000000000");
            }
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
        return getPlayers(false);
    }

    public List<GameProfile> getPlayersForCodec() {
        return List.copyOf(getPlayers());
    }

    public Set<GameProfile> getCachedPlayers() {
        return getPlayers(true);
    }

    public Set<GameProfile> getPlayers(boolean useCache) {
        if (playerCache != null && useCache) {
            return playerCache;
        } else {
            Set<GameProfile> containedPlayers = new HashSet<GameProfile>();
            for (Entry<GameProfile, UUID> entry : players.entrySet()) {
                if (entry.getValue() == this.getID()) {
                    containedPlayers.add(entry.getKey());
                }
            }
            return containedPlayers;
        }
    }

    public int getPlayerCount() {
        return getPlayerCount(false);
    }

    public int getPlayerCount(boolean useCache) {
        if (playerCache != null && useCache) {
            return playerCache.size();
        } else {
            return getPlayerCount(null);
        }
    }

    public int getPlayerCount(@Nullable PlayerEntity player) {
        int containedPlayers = 0;
        if (playerCache == null) {
            for (Entry<GameProfile, UUID> entry : players.entrySet()) {
                if (entry.getValue() == this.getID() && (player == null || player.getUuid() != entry.getKey().getId())) {
                    containedPlayers++;
                }
            }
        } else {
            containedPlayers = playerCache.size();
            if (player != null) {
                for (GameProfile prof : playerCache) {
                    if (prof.getId() == player.getUuid()) {
                        containedPlayers--;
                    }
                }
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

    public Either<Integer, BlockPos> getOwnerDetail() {
        if (ownerEntity != null) {
            return Either.left(ownerEntity.getId());
        } else {
            return Either.right(getBlockPos());
        }
    }

    public BlockPos getBlockPos() {
        if (ownerEntity != null) {
            cachedBlockPos = ownerEntity.getBlockPos();
        } else if (ownerBlockEntity != null) {
            cachedBlockPos = ownerBlockEntity.getPos();
        }
        if (cachedBlockPos != null) {
            return cachedBlockPos;
        }
        return BlockPos.ORIGIN; // fallback to hopefully also stop crashes???
    }

    public BlockPos getCachedBlockPos() {
        return cachedBlockPos;
    }

    public World getWorld() {
        if (ownerEntity != null) {
            return ownerEntity.getWorld();
        }
        if (ownerBlockEntity != null) {
            return ownerBlockEntity.getWorld();
        }
        return null;
    }

    public static void checkRecaptureDecapture(World world) {
        Map<PlayerEntity, ContainerInstance<?>> recaptured = new HashMap<PlayerEntity, ContainerInstance<?>>();
        Map<PlayerEntity, ContainerInstance<?>> released = new HashMap<PlayerEntity, ContainerInstance<?>>();
        Map<PlayerEntity, ContainerInstance<?>> caged = new HashMap<PlayerEntity, ContainerInstance<?>>();
        Map<PlayerEntity, ContainerInstance<?>> uncaged = new HashMap<PlayerEntity, ContainerInstance<?>>();

        // Recapturing
        for (Entry<UUID, UUID> entry : playersToRecapture.entrySet()) {
            PlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null && !disconnectedPlayers.contains(player.getUuid())) {
                ContainerInstance<?> cont = containers.get(entry.getValue());
                if (cont != null) {
                    cont.getOwner().ifLeft(entity -> {
                        if (((ServerWorld)world).getEntity(entity.getUuid()) != null) {
                            // both owner entity and player to recapture exist
                            recaptured.put(player, cont);
                            //cont.capture(player, true);
                        }
                    }).ifRight(blockEntity -> {
                        if (blockEntity != null) {
                            BlockPos pos = blockEntity.getPos();
                            if (world.isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
                                recaptured.put(player, cont);
                                //cont.capture(player, true);
                            }
                        }
                    });
                }
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : recaptured.entrySet()) {
            if (!entry.getValue().capture(entry.getKey(), true)) {
                entry.getValue().release(entry.getKey(), false, entry.getValue().getBlockPos(), false);
            }
            ContainerInstance.playersToRecapture.remove(entry.getKey().getUuid());
        }

        // Caging
        for (Entry<UUID, UUID> entry : CageSpectatorContainer.playersToCage.entrySet()) {
            PlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                ContainerInstance<?> cont = containers.get(entry.getValue());
                if (cont != null && cont.getContainer() instanceof CageSpectatorContainer) {
                    cont.getOwner().ifRight(blockEntity -> {
                        if (blockEntity != null && blockEntity instanceof CageBlockEntity) {
                            BlockPos pos = blockEntity.getPos();
                            if (world.isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
                                caged.put(player, cont);
                            }
                        }
                    });
                }
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : caged.entrySet()) {
            CageSpectatorContainer.playersToCage.remove(entry.getKey().getUuid());
            if (entry.getKey() instanceof ServerPlayerEntity spe) {
                entry.getValue().getContainer().onPlaceBlock(entry.getValue(), spe.getServerWorld(), entry.getValue().getBlockPos(), spe.getServer().getPlayerManager());
            }
        }

        // Uncaging
        for (Entry<UUID, UUID> entry : CageSpectatorContainer.playersToUncage.entrySet()) {
            PlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                ContainerInstance<?> cont = containers.get(entry.getValue());
                uncaged.put(player, cont);
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : uncaged.entrySet()) {
            CageSpectatorContainer.playersToUncage.remove(entry.getKey().getUuid());
            if (entry.getKey() instanceof ServerPlayerEntity spe) {
                CageSpectatorContainer.uncagePlayer(spe);
            }
        }

        // Releasing
        for (Entry<UUID, UUID> entry : playersToRelease.entrySet()) {
            PlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                ContainerInstance<?> cont = containers.get(entry.getValue());
                if (cont != null) {
                    cont.getOwner().ifLeft(entity -> {
                        if (world.getEntityById(entity.getId()) != null) {
                            // both owner entity and player to recapture exist
                            released.put(player, cont);
                            //cont.release(player, false);
                        }
                    }).ifRight(blockEntity -> {
                        if (blockEntity != null) {
                            BlockPos pos = blockEntity.getPos();
                            if (world.isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
                                released.put(player, cont);
                                //cont.release(player, false);
                            }
                        }
                    });
                }
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : released.entrySet()) {
            ContainerInstance.playersToRelease.remove(entry.getKey().getUuid());
            entry.getValue().release(entry.getKey(), false, entry.getValue().getBlockPos(), false);
        }

        if (recaptured.size() > 0 || released.size() > 0) {
            PlayerContainer.sendCIPtoAll(world.getServer().getPlayerManager());
        }

    }

    public static boolean releasePlayer(PlayerEntity player) {
        Map<PlayerEntity,ContainerInstance<?>> toRelease = new HashMap<PlayerEntity,ContainerInstance<?>>();
        boolean success = false;
        for (GameProfile profile : ContainerInstance.players.keySet()) {
            if (profile.getId() == player.getUuid()) {
                UUID contID = players.get(profile);
                if (contID != null && containers.containsKey(contID)) {
                    ContainerInstance<?> ci = containers.get(contID);
                    if (ci != null) { toRelease.put(player, ci); success = true; }
                } else {
                    PlayerContainer.LOGGER.warn("COULD NOT RELEASE PLAYER");
                }
            }
        }
        for (Entry<PlayerEntity,ContainerInstance<?>> entry : toRelease.entrySet()) {
            entry.getValue().release(entry.getKey(), false, entry.getValue().getBlockPos(), true);
        }
        for (Entry<UUID,UUID> entry : ContainerInstance.playersToRecapture.entrySet()) {
            if (entry.getKey() == player.getUuid()) {
                ContainerInstance.playersToRelease.put(entry.getKey(), entry.getValue());
            }
        }
        return success;
    }

    public void setOwnerClient(World world) {
        if (this.tempOwnerId != null) {
            this.ownerEntity = world.getEntityById(this.tempOwnerId);
            this.ownerBlockEntity = null;
        }
    }

    public void setOwner(Entity entity) {
        if (entity != ownerEntity) {
            if (entity instanceof PlayerEntity pe) {
                if (getPlayers().contains(pe.getGameProfile())) {
                    release(pe, false, getBlockPos(), true);
                }
            }
            container.onOwnerChange(getOwner(), Either.left(entity), this);
            ownerBlockEntity = null;
            ownerEntity = entity;
            cachedBlockPos = entity.getBlockPos();
        }
    }

    public void setOwner(BlockEntity blockEntity) {
        if (blockEntity != ownerBlockEntity) {
            container.onOwnerChange(getOwner(), Either.right(blockEntity), this);
            ownerBlockEntity = blockEntity;
            ownerEntity = null;
            cachedBlockPos = blockEntity.getPos();
        }
    }

    // write methods in here that call the relevant AbstractContainer method

    public boolean capture(PlayerEntity player, boolean temp) {
        return container.capture(player, this, temp);
    }

    public void release(PlayerEntity player, boolean temp, BlockPos pos, boolean doSend) {
        cachedBlockPos = pos;
        container.release(player, this, temp, pos, doSend);
    }

    public void releaseAll(PlayerManager players, boolean temp, BlockPos pos) {
        cachedBlockPos = pos;
        container.releaseAll(players, this, temp, pos);
    }

    public void destroy(PlayerManager players, BlockPos pos) {
        cachedBlockPos = pos;
        container.destroy(players, this, pos);
        containers.remove(ID);
        PlayerContainer.sendCIPtoAll(players);
    }

    public void onPlayerTick(ServerPlayerEntity player) {
        container.onPlayerTick(player, this);
    }
}
