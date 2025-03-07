package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.blocks.CageBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class Blocks {

    public static final CageBlock CAGE_BLOCK = register(new CageBlock(Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).strength(5.0F).sounds(BlockSoundGroup.METAL).nonOpaque()), "cage");

    public static <B extends Block> B register(B block, String id) {
        Identifier blockID = PlayerContainer.identifier(id);
        B registeredBlock = Registry.register(Registries.BLOCK, blockID, block);
        return registeredBlock;
    }

    public static void initialize() {}
    
}
