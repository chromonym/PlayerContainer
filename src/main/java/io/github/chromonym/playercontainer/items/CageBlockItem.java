package io.github.chromonym.playercontainer.items;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.CageSpectatorContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.Blocks;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CageBlockItem extends BlockItem implements ContainerInstanceHolder<CageSpectatorContainer> {

    public final CageSpectatorContainer container;

    public CageBlockItem(CageSpectatorContainer container, Settings settings) {
        super(Blocks.CAGE_BLOCK, settings.maxCount(1));
        this.container = container;
    }

    @Override
    public CageSpectatorContainer getContainer() {
        return container;
    }

    // THIS IS BAD CODE BUT I CAN'T BE BOTHERED FIGURING OUT HOW TO MAKE IT GOOD YET
    // COPIED FROM AbstractContainerItem:
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

    // COPIED FROM SIMPLECONTAINERITEM:
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
        if (user.isSneaking() && cont != null) {
            doRelease(stack, cont, world, user, hand, context.getBlockPos().add(context.getSide().getVector()));
            return ActionResult.SUCCESS;
        }
        return super.useOnBlock(context);
    }

    @Override
    public ActionResult place(ItemPlacementContext context) {
        if (context.getPlayer().isSneaking()) {
            return ActionResult.SUCCESS;
        }
        return super.place(context);
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
    
}
