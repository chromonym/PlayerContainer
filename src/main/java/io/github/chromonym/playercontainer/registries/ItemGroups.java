package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public class ItemGroups {
    public static final ItemGroup PLAYER_CONTAINERS = FabricItemGroup.builder()
        .icon(() -> new ItemStack(Items.basicContainer))
        .displayName(Text.translatable("itemGroup.playercontainer.playercontainer"))
        .entries((context, entries) -> {
            entries.add(Items.basicContainer);
            entries.add(Items.debugContainer);
        })
        .build();
    
    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, PlayerContainer.identifier("playercontainer"), PLAYER_CONTAINERS);
    }
}
