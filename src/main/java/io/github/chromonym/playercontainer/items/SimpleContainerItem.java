package io.github.chromonym.playercontainer.items;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SimpleContainerItem<C extends AbstractContainer> extends AbstractContainerItem<C> {

    public SimpleContainerItem(C container, Settings settings) {
        super(container, settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        ContainerInstance<?> cont = getOrMakeContainerInstance(user.getStackInHand(hand), user.getWorld());
        if (cont != null && entity instanceof PlayerEntity player) {
            boolean bl = cont.getContainer().capture(player, cont);
            return bl ? ActionResult.SUCCESS : ActionResult.CONSUME;
        }
        return super.useOnEntity(stack, user, entity, hand);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            ContainerInstance<?> cont = getOrMakeContainerInstance(user.getStackInHand(hand), user.getWorld());
            if (cont != null) {
                cont.getContainer().releaseAll(world.getServer().getPlayerManager(), cont);
                return TypedActionResult.success(user.getStackInHand(hand));
            }
        }
        return super.use(world, user, hand);
    }
    
}
