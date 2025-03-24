package io.github.chromonym.playercontainer.blockentities;

import java.util.UUID;
import java.util.stream.IntStream;

import io.github.chromonym.playercontainer.blocks.CageBlock;
import io.github.chromonym.playercontainer.containers.CageSpectatorContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.registries.BlockEntities;
import io.github.chromonym.playercontainer.registries.Containers;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import io.github.chromonym.playercontainer.registries.Items;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap.Builder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CageBlockEntity extends BlockEntity implements ContainerInstanceHolder<CageSpectatorContainer>, SidedInventory {

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
    
    public boolean getFragile() {
        return fragile;
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
        nbt.remove("containerId");
        nbt.remove("fragile");
    }

    @Override
    public CageSpectatorContainer getContainer() {
        return container;
    }

    public static void tick(World world, BlockPos pos, BlockState state, CageBlockEntity blockEntity) {
        ContainerInstance<?> ci = blockEntity.getOrMakeContainerInstance(blockEntity, world);
        if (ci != null) {
            ci.setOwner(blockEntity);
            int playerCount = ci.getPlayerCount();
            if (playerCount > 0 && !state.get(CageBlock.CAPTURED)) {
                world.setBlockState(pos, state.with(CageBlock.CAPTURED, true));
            } else if (playerCount == 0 && state.get(CageBlock.CAPTURED)) {
                world.setBlockState(pos, state.with(CageBlock.CAPTURED, false));
            }
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    private ItemStack getLoosePlayer() {
        return new ItemStack(Items.loosePlayer.getRegistryEntry(), 1, ComponentChanges.builder().add(ItemComponents.CONTAINER_ID, containerId).add(ItemComponents.BREAK_ON_RELEASE, true).build());
    }

    @Override
    public ItemStack getStack(int slot) {
        ContainerInstance<?> ci = getOrMakeContainerInstance(this, world);
        if (ci != null && ci.getPlayerCount() > 0) {
            return getLoosePlayer();
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean isEmpty() {
        ContainerInstance<?> ci = getOrMakeContainerInstance(this, world);
        return ci == null || ci.getPlayerCount() > 0;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return removeStack(slot, 1);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ContainerInstance<?> ci = getOrMakeContainerInstance(this, world);
        if (ci != null && ci.getPlayerCount() > 0) {
            ItemStack toReturn = getLoosePlayer();
            this.containerId = null;
            if (!this.getWorld().isClient) {
                CageSpectatorContainer.onBreakBlock(ci, this.getWorld().getServer().getPlayerManager());
            }
            ContainerInstance.containers.remove(containerId);
            Items.loosePlayer.getOrMakeContainerInstance(toReturn, world); // hopefully this doesn't totally break everything :3
            getOrMakeContainerInstance(this, world);
            world.updateComparators(pos, getCachedState().getBlock());
            markDirty();
            return toReturn;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        // do nothing
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public void clear() {
        // do nothing
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public boolean canInsert(int arg0, ItemStack arg1, Direction arg2) {
        return false;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        ContainerInstance<?> ci = getOrMakeContainerInstance(this, world);
        if (ci != null && ci.getPlayerCount() > 0) {
            return IntStream.of(0).toArray();
        }
        return IntStream.empty().toArray();
    }
    
}
