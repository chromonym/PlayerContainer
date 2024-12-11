package io.github.chromonym.playercontainer.networking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public record ContainerInstancesPayload(BiMap<UUID, ContainerInstance<?>> containers, Map<UUID, UUID> players) implements CustomPayload {

    public static final CustomPayload.Id<ContainerInstancesPayload> ID = new CustomPayload.Id<ContainerInstancesPayload>(Identifier.of(PlayerContainer.MOD_ID, "containers"));
    public static final PacketCodec<PacketByteBuf, ContainerInstancesPayload> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.map(HashBiMap::create, Uuids.PACKET_CODEC, ContainerInstance.PACKET_CODEC), ContainerInstancesPayload::containers,
        PacketCodecs.map(HashMap::new, Uuids.PACKET_CODEC, Uuids.PACKET_CODEC), ContainerInstancesPayload::players,
        ContainerInstancesPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
}
