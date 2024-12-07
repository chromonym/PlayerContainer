package io.github.chromonym.playercontainer.items;

import java.util.List;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.PlayerContainerComponents;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;

public class ContainerItem<C extends AbstractContainer> extends Item {

    private final C container;

    public ContainerItem(C container, Settings settings) {
        super(settings.component(PlayerContainerComponents.CONTAINER_ID, 0));
        this.container = container;
    }

    public ContainerInstance<?> getContainerInstance(ItemStack stack) {
        // check if this item has an id - if not, generate one and add a new relevant container (postprocesscomponents)
        // check if said id is stored - if not, create a new container with the relevant id.

        int id = stack.getOrDefault(PlayerContainerComponents.CONTAINER_ID, 0);
        if (id == 0) {
            //this.postProcessComponents(stack);
            return new ContainerInstance<AbstractContainer>(new AbstractContainer(), 0);
        } else if (!PlayerContainer.containers.containsKey(id)) {
            PlayerContainer.LOGGER.info("Container stack ID not yet loaded, adding it to the tracked list");
            ContainerInstance<C> cont = new ContainerInstance<C>(container, id);
            return cont;
        } else {
            return PlayerContainer.containers.get(id);
        }
    }

    public void fixComponents(ItemStack stack) {
        if (stack.getOrDefault(PlayerContainerComponents.CONTAINER_ID, 0) == 0) {
            ContainerInstance<C> cont = new ContainerInstance<C>(container);
            stack.set(PlayerContainerComponents.CONTAINER_ID, cont.getID());
        }
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (getContainerInstance(stack).getID() == 0) {
            stack.remove(PlayerContainerComponents.CONTAINER_ID);
            this.fixComponents(stack);
        }
        return super.onStackClicked(stack, slot, clickType, player);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.of("ID: "+Integer.toString(getContainerInstance(stack).getID())));
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        this.getContainerInstance(entity.getStack()).onDestroy();
    }
    
}
