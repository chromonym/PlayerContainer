package io.github.chromonym.playercontainer.items;

import org.jetbrains.annotations.NotNull;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
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
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity user = context.getPlayer();
        World world = context.getWorld();
        Hand hand = context.getHand();
        ItemStack stack = user.getStackInHand(hand);
        ContainerInstance<?> cont = getOrMakeContainerInstance(stack, user.getWorld());
        if (cont != null) {
            doRelease(stack, cont, world, user, hand, context.getBlockPos().add(context.getSide().getVector()));
            return ActionResult.SUCCESS;
        }
        return super.useOnBlock(context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            ContainerInstance<?> cont = getOrMakeContainerInstance(user.getStackInHand(hand), user.getWorld());
            if (cont != null) {
                doRelease(user.getStackInHand(hand), cont, world, user, hand, user.getBlockPos());
                return TypedActionResult.success(user.getStackInHand(hand));
            }
        }
        return super.use(world, user, hand);
    }
    
    public void doRelease(ItemStack stack, @NotNull ContainerInstance<?> cont, World world, PlayerEntity user, Hand hand, BlockPos releasePos) {
        if (stack.getOrDefault(ItemComponents.BREAK_ON_RELEASE, false)) {
            cont.getContainer().destroy(world.getServer().getPlayerManager(), cont, releasePos);
            user.setStackInHand(hand, ItemStack.EMPTY);
        } else {
            cont.getContainer().releaseAll(world.getServer().getPlayerManager(), cont, releasePos);
        }
    }

}
