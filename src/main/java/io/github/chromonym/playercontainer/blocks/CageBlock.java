package io.github.chromonym.playercontainer.blocks;

import com.mojang.serialization.MapCodec;

import io.github.chromonym.blockentities.CageBlockEntity;
import io.github.chromonym.playercontainer.containers.CageSpectatorContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.CageBlockItem;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.registries.BlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CageBlock extends BlockWithEntity {

    public static final BooleanProperty CAPTURED = BooleanProperty.of("captured");

    public static final VoxelShape cageShape = VoxelShapes.union(VoxelShapes.cuboid(0.9375, 0, 0, 1, 1, 1),
                                                                 VoxelShapes.cuboid(0, 0.9375, 0, 1, 1, 1),
                                                                 VoxelShapes.cuboid(0, 0, 0.9375, 1, 1, 1),
                                                                 VoxelShapes.cuboid(0, 0, 0, 0.0625, 1, 1),
                                                                 VoxelShapes.cuboid(0, 0, 0, 1, 0.0625, 1),
                                                                 VoxelShapes.cuboid(0, 0, 0, 1, 1, 0.0625));

    public CageBlock(Settings settings) {
        super(settings.nonOpaque().pistonBehavior(PistonBehavior.BLOCK));
        setDefaultState(getDefaultState().with(CAPTURED, false));
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(CAPTURED);
        super.appendProperties(builder);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            if (itemStack.getItem() instanceof CageBlockItem cbi) {
                ContainerInstance<?> ci = cbi.getOrMakeContainerInstance(itemStack, world);
                ci.setOwner(world.getBlockEntity(pos));
                ci.getContainer().onPlaceBlock(ci, (ServerWorld)world, pos, world.getServer().getPlayerManager());
            }
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof CageBlockEntity cbe && !world.isClient) {
            ContainerInstance<?> ci = cbe.getOrMakeContainerInstance(cbe, world);
            CageSpectatorContainer.onBreakBlock(ci, world.getServer().getPlayerManager());
            if (player.isCreative()) {
                ci.destroy(world.getServer().getPlayerManager(), pos);
            }
        }
        return super.onBreak(world, pos, state, player);
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
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(CAPTURED)) {
            return cageShape;
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> type) {
        return validateTicker(type, BlockEntities.CAGE_BLOCK_ENTITY, CageBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof ContainerInstanceHolder cih) {
            ContainerInstance<?> ci = cih.getOrMakeContainerInstance(entity, world);
            if (ci != null && ci.capture(player, false)) {
                world.updateComparators(pos, this);
                return ActionResult.SUCCESS;
            }
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof ContainerInstanceHolder cih) {
            ContainerInstance<?> ci = cih.getOrMakeContainerInstance(entity, world);
            if (ci != null && ci.getPlayerCount() > 0) {
                return 15;
            }
        }
        return 0;
    }
    
}
