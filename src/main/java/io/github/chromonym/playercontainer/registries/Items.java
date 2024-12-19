package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.LoggingContainer;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class Items {

    public static final SimpleContainerItem<LoggingContainer> debugContainer = register(new SimpleContainerItem<LoggingContainer>(Containers.LOGGING_CONTAINER, new Item.Settings().rarity(Rarity.EPIC)), "debug_container");
    public static final SimpleContainerItem<SpectatorContainer> basicContainer = register(new SimpleContainerItem<SpectatorContainer>(Containers.SPECTATOR_CONTAINER, new Item.Settings()), "basic_container");

    public static <I extends Item> I register(I item, String id) {
        Identifier itemID = Identifier.of(PlayerContainer.MOD_ID, id);
        I registeredItem = Registry.register(Registries.ITEM, itemID, item);
        return registeredItem;
    }

    public static void initialize() {}

}
