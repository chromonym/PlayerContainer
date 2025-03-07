package io.github.chromonym.playercontainer.blocks;

import com.mojang.serialization.MapCodec;

import io.github.chromonym.blockentities.CageBlockEntity;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.CageBlockItem;
import io.github.chromonym.playercontainer.registries.BlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CageBlock extends BlockWithEntity {

    public CageBlock(Settings settings) {
        super(settings.nonOpaque().pistonBehavior(PistonBehavior.BLOCK));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            if (itemStack.getItem() instanceof CageBlockItem cbi) {
                ContainerInstance<?> ci = cbi.getOrMakeContainerInstance(itemStack, world);
                ci.setOwner(world.getBlockEntity(pos));
            }
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CageBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(CageBlock::new);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> type) {
        return validateTicker(type, BlockEntities.CAGE_BLOCK_ENTITY, CageBlockEntity::tick);
    }
    
}
