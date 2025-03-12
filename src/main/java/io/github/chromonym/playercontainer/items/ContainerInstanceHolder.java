package io.github.chromonym.playercontainer.items;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import io.github.chromonym.blockentities.CageBlockEntity;
import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ContainerInstanceHolder<C extends AbstractContainer> {

    public abstract C getContainer();

    public default ContainerInstance<?> getOrMakeContainerInstance(ItemStack stack, World world) {
        return getOrMakeContainerInstance(stack, world, false);
    }

    public default ContainerInstance<?> getOrMakeContainerInstance(ItemStack stack, World world, boolean allowClient) {
        // check if this item has an id - if not, generate one and add a new relevant container (postprocesscomponents)
        // check if said id is stored - if not, create a new container with the relevant id.
        // only returns the containerInstance (doesn't create one if missing) if world is client
        if (allowClient || !world.isClient()) {
            if (stack.getItem() instanceof ContainerInstanceHolder) {
                UUID id = stack.get(ItemComponents.CONTAINER_ID);
                return getOrMakeContainerInstance(id, world, (uid) -> {stack.set(ItemComponents.CONTAINER_ID, uid);}, allowClient);
            } else {
                return null;
            }
        }
        return null;
    }

    public default ContainerInstance<?> getOrMakeContainerInstance(BlockEntity blockEntity, World world) {
        return getOrMakeContainerInstance(blockEntity, world, false);
    }

    public default ContainerInstance<?> getOrMakeContainerInstance(BlockEntity blockEntity, World world, boolean allowClient) {
        // check if this item has an id - if not, generate one and add a new relevant container (postprocesscomponents)
        // check if said id is stored - if not, create a new container with the relevant id.
        // only returns the containerInstance (doesn't create one if missing) if world is client
        if (allowClient || !world.isClient()) {
            if (blockEntity instanceof CageBlockEntity cage) {
                UUID id = cage.getContainerId();
                ContainerInstance<?> ci = getOrMakeContainerInstance(id, world, (uid) -> {cage.setContainerId(uid);}, allowClient);
                ci.setOwner(blockEntity);
                return ci;
            } else {
                return null;
            }
        }
        return null;
    }

    public default ContainerInstance<?> getOrMakeContainerInstance(UUID id, World world, Consumer<UUID> addToContainer, boolean allowClient) {
        if (id == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
            id = null;
        }
        if (id == null) {
            if (allowClient || world.isClient()) {
                return null;
            } else {
                //PlayerContainer.LOGGER.info("Container stack ID not yet created, adding it to the tracked list");
                ContainerInstance<C> cont = new ContainerInstance<C>(getContainer());
                addToContainer.accept(cont.getID());
                PlayerContainer.sendCIPtoAll(world.getServer().getPlayerManager());
                return cont;
            }
        } if (!ContainerInstance.containers.containsKey(id)) {
            if (allowClient || world.isClient()) {
                return null;
            } else {
                //PlayerContainer.LOGGER.info("Container stack ID not yet loaded, adding it to the tracked list");
                ContainerInstance<C> cont = new ContainerInstance<C>(getContainer(), id);
                PlayerContainer.sendCIPtoAll(world.getServer().getPlayerManager());
                return cont;
            }
        } else {
            return ContainerInstance.containers.get(id);
        }
    }

    public default void doRelease(ItemStack stack, @NotNull ContainerInstance<?> cont, World world, PlayerEntity user, Hand hand, BlockPos releasePos) {
        if (stack.getOrDefault(ItemComponents.BREAK_ON_RELEASE, false)) {
            cont.getContainer().destroy(world.getServer().getPlayerManager(), cont, releasePos);
            user.setStackInHand(hand, ItemStack.EMPTY);
        } else {
            cont.getContainer().releaseAll(world.getServer().getPlayerManager(), cont, releasePos);
        }
    }
}
