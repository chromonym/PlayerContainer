package io.github.chromonym.playercontainer.items;

import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class AbstractContainerItem<C extends AbstractContainer> extends Item {
    
    private final C container;

    public AbstractContainerItem(C container, Settings settings) {
        super(settings.maxCount(1));
        this.container = container;
    }

    public final ContainerInstance<?> getOrMakeContainerInstance(ItemStack stack, World world) {
        // check if this item has an id - if not, generate one and add a new relevant container (postprocesscomponents)
        // check if said id is stored - if not, create a new container with the relevant id.
        if (stack.getItem() instanceof SimpleContainerItem && !world.isClient()) {
            UUID id = stack.get(ItemComponents.CONTAINER_ID);
            if (id == null) {
                PlayerContainer.LOGGER.info("Container stack ID not yet created, adding it to the tracked list");
                ContainerInstance<C> cont = new ContainerInstance<C>(container);
                stack.set(ItemComponents.CONTAINER_ID, cont.getID());
                return cont;
            } if (!ContainerInstance.containers.containsKey(id)) {
                PlayerContainer.LOGGER.info("Container stack ID not yet loaded, adding it to the tracked list");
                ContainerInstance<C> cont = new ContainerInstance<C>(container, id);
                return cont;
            } else {
                return ContainerInstance.containers.get(id);
            }
        } else {
            return null;
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        ContainerInstance<?> cont = getOrMakeContainerInstance(stack, world);
        if (cont != null) {
            cont.setOwner(entity);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        ContainerInstance<?> cont = this.getOrMakeContainerInstance(entity.getStack(), entity.getWorld());
        if (cont != null) {cont.destroy(entity.getServer().getPlayerManager());}
    }

}
