package io.github.chromonym.blockentities;

import java.util.UUID;

import io.github.chromonym.playercontainer.containers.CageSpectatorContainer;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.registries.BlockEntities;
import io.github.chromonym.playercontainer.registries.Containers;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap.Builder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CageBlockEntity extends BlockEntity implements ContainerInstanceHolder<CageSpectatorContainer> {

    private CageSpectatorContainer container = Containers.CAGE_SPECTATOR_CONTAINER;
    private UUID containerId;
    private boolean fragile = false;

    public CageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CAGE_BLOCK_ENTITY, pos, state);
    }

    public void setContainerId(UUID containerId) {
        this.containerId = containerId;
        this.markDirty();
    }

    public UUID getContainerId() {
        return containerId;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        nbt.putUuid("containerId", containerId == null ? UUID.fromString("00000000-0000-0000-0000-000000000000"): containerId);
        nbt.putBoolean("fragile", fragile);
        super.writeNbt(nbt, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        containerId = nbt.getUuid("containerId");
        if (containerId.toString() == "00000000-0000-0000-0000-000000000000") {
            containerId = null;
        }
        fragile = nbt.getBoolean("fragile");
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        containerId = components.getOrDefault(ItemComponents.CONTAINER_ID, null);
        fragile = components.getOrDefault(ItemComponents.BREAK_ON_RELEASE, false);
    }

    @Override
    protected void addComponents(Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        if (containerId != null) {
            componentMapBuilder.add(ItemComponents.CONTAINER_ID, containerId);
        }
        componentMapBuilder.add(ItemComponents.BREAK_ON_RELEASE, fragile);
    }

    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        nbt.remove("id");
        nbt.remove("fragile");
    }

    @Override
    public CageSpectatorContainer getContainer() {
        return container;
    }

    public static void tick(World world, BlockPos pos, BlockState state, CageBlockEntity blockEntity) {
    }
    
}
