package io.github.chromonym.playercontainer;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.items.ContainerItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class PlayerContainerItems {

    public static final ContainerItem<AbstractContainer> simpleContainer = register(new ContainerItem<AbstractContainer>(new AbstractContainer(), new Item.Settings()), "simple");

    public static <I extends Item> I register(I item, String id) {
        Identifier itemID = Identifier.of(PlayerContainer.MOD_ID, id);
        I registeredItem = Registry.register(Registries.ITEM, itemID, item);
        return registeredItem;
    }

    public static void initialize() {}

}
