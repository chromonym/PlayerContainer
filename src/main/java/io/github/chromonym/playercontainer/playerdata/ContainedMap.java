package io.github.chromonym.playercontainer.playerdata;

import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class ContainedMap extends PersistentState {

    public BiMap<UUID, Integer> containedPlayers = HashBiMap.create();

    public static ContainedMap getServerState(ServerWorld server) {
        PersistentStateManager manager = server.getPersistentStateManager();
        ContainedMap state = manager.getOrCreate(type, PlayerContainer.MOD_ID);
        state.markDirty();
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        NbtCompound containersNbt = new NbtCompound();
        containedPlayers.forEach((uuid, id) -> {
            if (id != 0) {
                containersNbt.putInt(uuid.toString(), id);
            }
        });
        nbt.put("containedPlayers",containersNbt);
        return nbt;
    }

    public static ContainedMap createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ContainedMap state = new ContainedMap();
        NbtCompound containersNbt = nbt.getCompound("containedPlayers");
        containersNbt.getKeys().forEach((strUuid) -> {
            state.containedPlayers.put(UUID.fromString(strUuid), containersNbt.getInt(strUuid));
        });
        return state;
    }

    private static Type<ContainedMap> type = new Type<ContainedMap>(
        ContainedMap::new, ContainedMap::createFromNbt, null
    );

    
}
