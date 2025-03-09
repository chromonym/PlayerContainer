package io.github.chromonym.playercontainer.networking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.CageSpectatorContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ContainerPersistentState extends PersistentState {
    // i know it shouldn't really be in the /networking/ folder but shhh
    public BiMap<UUID, ContainerInstance<?>> containers = HashBiMap.create();
    public Map<GameProfile, UUID> players = new HashMap<GameProfile, UUID>(); // PLAYERS TO CONTAINERS!!
    public Map<UUID, UUID> playersToRecapture = new HashMap<UUID, UUID>(); // players that need to be recaptured by a given container when next possible
    public Map<UUID, UUID> playersToRelease = new HashMap<UUID, UUID>(); // players that need to be released when next possible
    public Map<UUID, UUID> playersToCage = new HashMap<UUID, UUID>();
    public Map<UUID, UUID> playersToUncage = new HashMap<UUID, UUID>();

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

        //NbtCompound nbtPlayers = new NbtCompound();
        NbtList nbtPlayers = new NbtList();
        for (Entry<GameProfile, UUID> entry : players.entrySet()) {
            NbtCompound nbtPlayer = new NbtCompound();
            //nbtPlayer.putString("name", entry.getKey().getName());
            nbtPlayer.put("profile",Codecs.GAME_PROFILE_WITH_PROPERTIES.encodeStart(NbtOps.INSTANCE, entry.getKey()).getOrThrow());
            nbtPlayer.putUuid("container", entry.getValue());
            //nbtPlayers.put(entry.getKey().getId().toString(), nbtPlayer);
            nbtPlayers.add(nbtPlayer);
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

        NbtCompound nbtCage = new NbtCompound();
        for (Entry<UUID, UUID> entry : playersToCage.entrySet()) {
            nbtCage.putUuid(entry.getKey().toString(), entry.getValue());
        }
        newNbt.put("playersToCage", nbtCage);

        NbtCompound nbtUncage = new NbtCompound();
        for (Entry<UUID, UUID> entry : playersToUncage.entrySet()) {
            nbtUncage.putUuid(entry.getKey().toString(), entry.getValue());
        }
        newNbt.put("playersToUncage", nbtUncage);

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

        NbtList nbtPlayers = newNbt.getList("players", NbtList.COMPOUND_TYPE);
        nbtPlayers.forEach(nbtObj -> {
            NbtCompound player = (NbtCompound)nbtObj;
            UUID containerID = player.getUuid("container");
            GameProfile playerProfile = Codecs.GAME_PROFILE_WITH_PROPERTIES.decode(NbtOps.INSTANCE, player.get("profile")).getOrThrow().getFirst();
            state.players.put(playerProfile, containerID);
        });

        NbtCompound nbtRecapture = newNbt.getCompound("playersToRecapture");
        nbtRecapture.getKeys().forEach(key -> {
            state.playersToRecapture.put(UUID.fromString(key), nbtRecapture.getUuid(key));
        });

        NbtCompound nbtRelease = newNbt.getCompound("playersToRelease");
        nbtRelease.getKeys().forEach(key -> {
            state.playersToRelease.put(UUID.fromString(key), nbtRelease.getUuid(key));
        });

        NbtCompound nbtCage = newNbt.getCompound("playersToCage");
        nbtCage.getKeys().forEach(key -> {
            state.playersToCage.put(UUID.fromString(key), nbtCage.getUuid(key));
        });

        NbtCompound nbtUncage = newNbt.getCompound("playersToUncage");
        nbtUncage.getKeys().forEach(key -> {
            state.playersToUncage.put(UUID.fromString(key), nbtUncage.getUuid(key));
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
        this.playersToCage = new HashMap<UUID,UUID>(CageSpectatorContainer.playersToCage);
        this.playersToUncage = new HashMap<UUID,UUID>(CageSpectatorContainer.playersToUncage);
        this.markDirty();
    }

    public void updateToCI() {
        // ContainerPersistentState -> ContainerInstance
        ContainerInstance.containers = HashBiMap.create(this.containers);
        ContainerInstance.players = new HashMap<GameProfile, UUID>(this.players);
        ContainerInstance.playersToRecapture = new HashMap<UUID, UUID>(this.playersToRecapture);
        ContainerInstance.playersToRelease = new HashMap<UUID, UUID>(this.playersToRelease);
        CageSpectatorContainer.playersToCage = new HashMap<UUID, UUID>(this.playersToCage);
        CageSpectatorContainer.playersToUncage = new HashMap<UUID, UUID>(this.playersToUncage);
    }
}
