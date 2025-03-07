package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.blockentities.CageBlockEntity;
import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class BlockEntities {

    public static final BlockEntityType<CageBlockEntity> CAGE_BLOCK_ENTITY = register(
      "demo_block",
      // For versions 1.21.2 and above,
      // replace `BlockEntityType.Builder` with `FabricBlockEntityTypeBuilder`.
      BlockEntityType.Builder.create(CageBlockEntity::new, Blocks.CAGE_BLOCK).build()
  );

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, PlayerContainer.identifier(path), blockEntityType);
    }

    public static void initialize() {}
}
