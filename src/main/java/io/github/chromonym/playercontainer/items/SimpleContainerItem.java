package io.github.chromonym.playercontainer.items;

import java.util.List;
import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SimpleContainerItem<C extends AbstractContainer> extends Item {

    private final C container;

    public SimpleContainerItem(C container, Settings settings) {
        super(settings);
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ContainerInstance<?> cont = getOrMakeContainerInstance(user.getStackInHand(hand), world);
        if (cont != null) {
            cont.getContainer().onCapture(user, cont);
        }
        return super.use(world, user, hand);
    }

    /*@Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (getContainerInstance(stack).getID() == null) {
            stack.remove(ItemComponents.CONTAINER_ID);
            this.fixComponents(stack);
        }
        return super.onStackClicked(stack, slot, clickType, player);
    }*/

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        UUID cont = stack.get(ItemComponents.CONTAINER_ID);
        if (cont != null) {
            tooltip.add(Text.of("ID: "+cont.toString()));
        }
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        ContainerInstance<?> cont = this.getOrMakeContainerInstance(entity.getStack(), entity.getWorld());
        if (cont != null) {cont.onDestroy();}
    }
    
}
