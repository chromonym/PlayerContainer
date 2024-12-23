package io.github.chromonym.playercontainer.networking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ContainerPersistentState extends PersistentState {
    // i know it shouldn't really be in the /networking/ folder but shhh
    public BiMap<UUID, ContainerInstance<?>> containers = HashBiMap.create();
    public Map<GameProfile, UUID> players = new HashMap<GameProfile, UUID>(); // PLAYERS TO CONTAINERS!!
    public Map<UUID, UUID> playersToRecapture = new HashMap<UUID, UUID>(); // players that need to be recaptured by a given container when next possible
    public Map<UUID, UUID> playersToRelease = new HashMap<UUID, UUID>(); // players that need to be released when next possible

    /*
     *"modid": {
     *  "players": {
     *      "UUID": {
     *          "name": "PlayerName",
     *          "container": "UUID"
     *      }
     *  },
     *  "playersToRecapture": {
     *      "UUID": "UUID",
     *      "UUID": "UUID",
     *  },
     *  "playersToRelease": {
     *      "UUID": "UUID",
     *      "UUID": "UUID",
     *  }
     *}
     */

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        NbtCompound newNbt = new NbtCompound();

        NbtCompound nbtContainers = new NbtCompound();
        for (Entry<UUID, ContainerInstance<?>> entry : containers.entrySet()) {
            NbtElement nbtContainer = ContainerInstance.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow();
            nbtContainers.put(entry.getKey().toString(), nbtContainer);
        }
        newNbt.put("containers",nbtContainers);

        NbtCompound nbtPlayers = new NbtCompound();
        for (Entry<GameProfile, UUID> entry : players.entrySet()) {
            NbtCompound nbtPlayer = new NbtCompound();
            nbtPlayer.putString("name", entry.getKey().getName());
            nbtPlayer.putUuid("container", entry.getValue());
            nbtPlayers.put(entry.getKey().getId().toString(), nbtPlayer);
        }
        newNbt.put("players",nbtPlayers);

        NbtCompound nbtRecapture = new NbtCompound();
        for (Entry<UUID, UUID> entry : playersToRecapture.entrySet()) {
            nbtRecapture.putUuid(entry.getKey().toString(), entry.getValue());
        }
        newNbt.put("playersToRecapture", nbtRecapture);

        NbtCompound nbtRelease = new NbtCompound();
        for (Entry<UUID, UUID> entry : playersToRelease.entrySet()) {
            nbtRelease.putUuid(entry.getKey().toString(), entry.getValue());
        }
        newNbt.put("playersToRelease", nbtRelease);

        nbt.put(PlayerContainer.MOD_ID, newNbt);
        return nbt;
    }

    public static ContainerPersistentState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound newNbt = nbt.getCompound(PlayerContainer.MOD_ID);
        ContainerPersistentState state = new ContainerPersistentState();

        NbtCompound nbtContainers = newNbt.getCompound("containers");
        nbtContainers.getKeys().forEach(key -> {
            UUID containerID = UUID.fromString(key);
            NbtElement nbtContainer = nbtContainers.get(key);
            ContainerInstance<?> container = ContainerInstance.CODEC.decode(NbtOps.INSTANCE, nbtContainer).getOrThrow().getFirst();
            state.containers.put(containerID, container);
        });

        NbtCompound nbtPlayers = newNbt.getCompound("players");
        nbtPlayers.getKeys().forEach(key -> {
            UUID playerID = UUID.fromString(key);
            NbtCompound player = nbtPlayers.getCompound(key);
            String playerName = player.getString("name");
            UUID containerID = player.getUuid("container");
            state.players.put(new GameProfile(playerID, playerName), containerID);
        });

        NbtCompound nbtRecapture = newNbt.getCompound("playersToRecapture");
        nbtRecapture.getKeys().forEach(key -> {
            state.playersToRecapture.put(UUID.fromString(key), nbtRecapture.getUuid(key));
        });

        NbtCompound nbtRelease = newNbt.getCompound("playersToRelease");
        nbtRelease.getKeys().forEach(key -> {
            state.playersToRelease.put(UUID.fromString(key), nbtRelease.getUuid(key));
        });

        return state;
    }

    private static Type<ContainerPersistentState> type = new Type<ContainerPersistentState>(
        ContainerPersistentState::new,
        ContainerPersistentState::createFromNbt,
        null);

    public static ContainerPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        ContainerPersistentState state = manager.getOrCreate(type, PlayerContainer.MOD_ID);
        state.markDirty();
        return state;
    }

    public void updateFromCI() {
        // ContainerInstance -> ContainerPersistentState
        this.containers = HashBiMap.create(ContainerInstance.containers);
        this.players = new HashMap<GameProfile, UUID>(ContainerInstance.players);
        this.playersToRecapture = new HashMap<UUID,UUID>(ContainerInstance.playersToRecapture);
        this.playersToRelease = new HashMap<UUID,UUID>(ContainerInstance.playersToRelease);
        PlayerContainer.LOGGER.info(containers.toString());
        PlayerContainer.LOGGER.info(players.toString());
        PlayerContainer.LOGGER.info(playersToRecapture.toString());
        PlayerContainer.LOGGER.info(playersToRelease.toString());
        this.markDirty();
    }

    public void updateToCI() {
        // ContainerPersistentState -> ContainerInstance
        ContainerInstance.containers = HashBiMap.create(this.containers);
        ContainerInstance.players = new HashMap<GameProfile, UUID>(this.players);
        ContainerInstance.playersToRecapture = new HashMap<UUID, UUID>(this.playersToRecapture);
        ContainerInstance.playersToRelease = new HashMap<UUID, UUID>(this.playersToRelease);
        PlayerContainer.LOGGER.info(ContainerInstance.containers.toString());
        PlayerContainer.LOGGER.info(ContainerInstance.players.toString());
        PlayerContainer.LOGGER.info(ContainerInstance.playersToRecapture.toString());
        PlayerContainer.LOGGER.info(ContainerInstance.playersToRelease.toString());
    }
}
