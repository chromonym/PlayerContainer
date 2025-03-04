package io.github.chromonym.playercontainer.containers;

import java.util.Iterator;
import java.util.List;

import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class ContainerDispenserBehaviour extends FallibleItemDispenserBehavior {
    public ContainerDispenserBehaviour() {}

    @Override
    protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
        ServerWorld world = pointer.world();
        BlockPos blockPos = pointer.pos().offset((Direction)pointer.state().get(DispenserBlock.FACING));
        if (stack.getItem() instanceof AbstractContainerItem aci) {
            ContainerInstance<?> ci = aci.getOrMakeContainerInstance(stack, world);
            this.setSuccess(tryCapture(world, blockPos, ci));
            if (!this.isSuccess()) {
                this.setSuccess(tryRelease(world, blockPos, ci));
            }
            if (this.isSuccess()) {
                return stack;
            }
        }
        return super.dispenseSilently(pointer, stack);
    }

    private static boolean tryCapture(ServerWorld world, BlockPos pos, ContainerInstance<?> ci) {
        List<PlayerEntity> list = world.getEntitiesByClass(PlayerEntity.class, new Box(pos), EntityPredicates.EXCEPT_SPECTATOR);
        Iterator<PlayerEntity> var3 = list.iterator();
        while (var3.hasNext()) {
            PlayerEntity player = (PlayerEntity)var3.next();
            if (ci.capture(player, false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryRelease(ServerWorld world, BlockPos pos, ContainerInstance<?> ci) {
        boolean bl = ci.getPlayerCount() > 0;
        ci.releaseAll(world.getServer().getPlayerManager(), false, pos);
        return bl;
    }
    
}
