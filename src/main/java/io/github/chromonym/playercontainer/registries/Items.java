package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.LoggingContainer;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import io.github.chromonym.playercontainer.items.CageBlockItem;
import io.github.chromonym.playercontainer.items.NamedItem;
import io.github.chromonym.playercontainer.items.NamedSingleContainerItem;
import io.github.chromonym.playercontainer.items.SelfContainerItem;
import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class Items {
    // Debug Container
    public static final SimpleContainerItem<LoggingContainer> debugContainer = register(new SimpleContainerItem<LoggingContainer>(Containers.LOGGING_CONTAINER, new Item.Settings().rarity(Rarity.EPIC)), "debug_container");
    // Standard Containers
    public static final SimpleContainerItem<SpectatorContainer> basicContainer = register(new SimpleContainerItem<SpectatorContainer>(Containers.SPECTATOR_CONTAINER, new Item.Settings().rarity(Rarity.RARE)), "basic_container");
    public static final SimpleContainerItem<SpectatorContainer> largeContainer = register(new SimpleContainerItem<SpectatorContainer>(Containers.LARGE_SPECTATOR_CONTAINER, new Item.Settings().rarity(Rarity.RARE)), "large_container");
    public static final SimpleContainerItem<SpectatorContainer> hugeContainer = register(new SimpleContainerItem<SpectatorContainer>(Containers.HUGE_SPECTATOR_CONTAINER, new Item.Settings().rarity(Rarity.RARE)), "huge_container");
    public static final SimpleContainerItem<SpectatorContainer> singularityContainer = register(new SimpleContainerItem<SpectatorContainer>(Containers.INFINITE_SPECTATOR_CONTAINER, new Item.Settings().rarity(Rarity.RARE)), "singularity_container");
    // Special Containers
    public static final NamedSingleContainerItem<SpectatorContainer> loosePlayer = register(new NamedSingleContainerItem<SpectatorContainer>(Containers.SPECTATOR_CONTAINER, new Item.Settings().rarity(Rarity.RARE)), "loose_player");
    public static final CageBlockItem cageBlock = register(new CageBlockItem(Containers.CAGE_SPECTATOR_CONTAINER, new Item.Settings().rarity(Rarity.RARE)), "cage");
    public static final SelfContainerItem<SpectatorContainer> selfContainer = register(new SelfContainerItem<SpectatorContainer>(Containers.SPECTATOR_CONTAINER, new Item.Settings()), "self_container");
    // Other Items
    public static final NamedItem playerEssence = register(new NamedItem(new Item.Settings()), "player_essence");
    public static final NamedItem playerEssenceBottle = register(new NamedItem(new Item.Settings().recipeRemainder(net.minecraft.item.Items.GLASS_BOTTLE)), "player_essence_bottle");

    public static <I extends Item> I register(I item, String id) {
        Identifier itemID = PlayerContainer.identifier(id);
        I registeredItem = Registry.register(Registries.ITEM, itemID, item);
        return registeredItem;
    }

    public static void initialize() {}

}
