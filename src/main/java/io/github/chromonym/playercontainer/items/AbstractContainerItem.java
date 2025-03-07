package io.github.chromonym.playercontainer.items;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import io.github.chromonym.playercontainer.registries.Items;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class AbstractContainerItem<C extends AbstractContainer> extends Item implements ContainerInstanceHolder<C> {

    // I KNOW THIS IS BAD CODE (fix later:tm:) BUT SOME OF THIS IS COPIED IN CAGEBLOCKITEM
    
    private final C container;

    public AbstractContainerItem(C container, Settings settings) {
        super(settings.maxCount(1));
        this.container = container;
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
        if (cont != null) {cont.destroy(entity.getServer().getPlayerManager(), entity.getBlockPos());}
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        UUID cont = stack.get(ItemComponents.CONTAINER_ID);
        if (stack.getItem() instanceof ContainerInstanceHolder<?> aci) {
            ContainerInstance<?> ci = aci.getOrMakeContainerInstance(stack, null, true);
            if (ci != null && ci.getPlayerCount(true) != 0) {
                tooltip.add(Text.translatable("tooltip.playercontainer.contains").formatted(Formatting.GRAY));
                for (GameProfile player : ci.getPlayers(true)) {
                    tooltip.add(Text.literal("- " + player.getName()).formatted(Formatting.GRAY));
                }
			}
        }
        if (cont != null && type.isAdvanced()) {
            tooltip.add(Text.literal("ID: "+cont.toString()).formatted(Formatting.DARK_GRAY));
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.isOf(Items.debugContainer) || super.hasGlint(stack);
    }

    @Override
    public C getContainer() {
        return container;
    }

}
