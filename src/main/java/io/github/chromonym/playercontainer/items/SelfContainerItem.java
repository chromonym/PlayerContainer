package io.github.chromonym.playercontainer.items;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SelfContainerItem<C extends AbstractContainer> extends SimpleContainerItem<C> {

    public SelfContainerItem(C container, Settings settings) {
        super(container, settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        return use(user.getWorld(), user, hand).getResult();
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity user = context.getPlayer();
        World world = context.getWorld();
        Hand hand = context.getHand();
        ItemStack stack = user.getStackInHand(hand);
        ContainerInstance<?> cont = getOrMakeContainerInstance(stack, world);
        if (cont != null) {
            if (cont.getPlayerCount() == 0) {
                return use(world, user, hand).getResult();
            } else {
                doRelease(stack, cont, world, user, hand, context.getBlockPos().add(context.getSide().getVector()));
                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ContainerInstance<?> cont = getOrMakeContainerInstance(stack, user.getWorld());
        if (cont != null) {
            if (cont.getPlayerCount() > 0) {
                doRelease(user.getStackInHand(hand), cont, world, user, hand, user.getBlockPos());
            } else {
                ItemEntity entity = user.dropItem(stack, true);
                user.setStackInHand(hand, ItemStack.EMPTY);
                cont.setOwner(entity);
                cont.capture(user, false);
            }
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return super.use(world, user, hand);
    }
    
}
