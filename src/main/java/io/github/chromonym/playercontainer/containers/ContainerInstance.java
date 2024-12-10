package io.github.chromonym.playercontainer.containers;

import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.chromonym.playercontainer.registries.Containers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Uuids;

public class ContainerInstance<C extends AbstractContainer> {

    public static BiMap<UUID, ContainerInstance<?>> containers = HashBiMap.create();
    public static BiMap<UUID, UUID> players = HashBiMap.create(); // PLAYERS TO CONTAINERS!!

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
    private final UUID ID;
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
        return ID;
    }

    public C getContainer() {
        return container;
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

    public void setOwner(Entity entity) {
        if (entity != ownerEntity) {
            ownerBlockEntity = null;
            ownerEntity = entity;
            container.onOwnerChange(Either.left(entity), this);
        }
    }

    public void setOwner(BlockEntity blockEntity) {
        if (blockEntity != ownerBlockEntity) {
            ownerBlockEntity = blockEntity;
            ownerEntity = null;
            container.onOwnerChange(Either.right(blockEntity), this);
        }
    }

    // write methods in here that call the relevant AbstractContainer method

    public void onDestroy() {
        containers.remove(ID);
        container.onDestroy(this);
    }
}
