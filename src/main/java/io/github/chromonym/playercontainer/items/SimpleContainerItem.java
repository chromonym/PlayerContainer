package io.github.chromonym.playercontainer.items;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SimpleContainerItem<C extends AbstractContainer> extends AbstractContainerItem<C> {

    // I KNOW THIS IS BAD CODE (fix later:tm:) BUT SOME OF THIS IS COPIED IN CAGEBLOCKITEM

    public SimpleContainerItem(C container, Settings settings) {
        super(container, settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        ContainerInstance<?> cont = getOrMakeContainerInstance(user.getStackInHand(hand), user.getWorld());
        if (!user.isSneaking() && cont != null && entity instanceof PlayerEntity player) {
            boolean bl = cont.getContainer().capture(player, cont);
            return ActionResult.success(bl);
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
        ContainerInstance<?> cont = getOrMakeContainerInstance(user.getStackInHand(hand), user.getWorld());
        if (cont != null && (user.isSneaking() || cont.getPlayerCount() >= cont.getContainer().maxPlayers)) {
            doRelease(user.getStackInHand(hand), cont, world, user, hand, user.getBlockPos());
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return super.use(world, user, hand);
    }

}
