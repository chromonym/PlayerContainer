package io.github.chromonym.playercontainer.items;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class AbstractContainerItem<C extends AbstractContainer> extends Item {
    
    private final C container;

    public AbstractContainerItem(C container, Settings settings) {
        super(settings.maxCount(1));
        this.container = container;
    }

    public final ContainerInstance<?> getOrMakeContainerInstance(ItemStack stack, World world) {
        return getOrMakeContainerInstance(stack, world, false);
    }

    public final ContainerInstance<?> getOrMakeContainerInstance(ItemStack stack, World world, boolean allowClient) {
        // check if this item has an id - if not, generate one and add a new relevant container (postprocesscomponents)
        // check if said id is stored - if not, create a new container with the relevant id.
        // only returns the containerInstance (doesn't create one if missing) if world is client
        if (allowClient || !world.isClient()) {
            if (stack.getItem() instanceof AbstractContainerItem) {
                UUID id = stack.get(ItemComponents.CONTAINER_ID);
                if (id == null) {
                    if (allowClient || world.isClient()) {
                        return null;
                    } else {
                        PlayerContainer.LOGGER.info("Container stack ID not yet created, adding it to the tracked list");
                        ContainerInstance<C> cont = new ContainerInstance<C>(container);
                        stack.set(ItemComponents.CONTAINER_ID, cont.getID());
                        return cont;
                    }
                } if (!ContainerInstance.containers.containsKey(id)) {
                    if (allowClient || world.isClient()) {
                        return null;
                    } else {
                        PlayerContainer.LOGGER.info("Container stack ID not yet loaded, adding it to the tracked list");
                        ContainerInstance<C> cont = new ContainerInstance<C>(container, id);
                        return cont;
                    }
                } else {
                    return ContainerInstance.containers.get(id);
                }
            } else {
                return null;
            }
        }
        return null;
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

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        UUID cont = stack.get(ItemComponents.CONTAINER_ID);
        if (stack.getItem() instanceof AbstractContainerItem<?> aci) {
            ContainerInstance<?> ci = aci.getOrMakeContainerInstance(stack, null, true);
            if (ci != null) {
                tooltip.add(Text.translatable("tooltip.playercontainer.contains"));
                for (GameProfile player : ci.getPlayers()) {
                    tooltip.add(Text.of("- " + player.getName()));
                }
			}
        }
        if (cont != null && type.isAdvanced()) {
            tooltip.add(Text.of("ID: "+cont.toString()));
        }
    }

}
