package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.CageSpectatorContainer;
import io.github.chromonym.playercontainer.containers.LoggingContainer;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class Containers {
    
    public static final RegistryKey<Registry<AbstractContainer>> REGISTRY_KEY = RegistryKey.ofRegistry(PlayerContainer.identifier("containers"));
    public static final Registry<AbstractContainer> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    public static final LoggingContainer LOGGING_CONTAINER = register(new LoggingContainer(), "logging");
    public static final SpectatorContainer SPECTATOR_CONTAINER = register(new SpectatorContainer(10.5,20.5), "spectator");
    public static final SpectatorContainer LARGE_SPECTATOR_CONTAINER = register(new SpectatorContainer(3, 10.5,20.5), "spectator_large");
    public static final SpectatorContainer HUGE_SPECTATOR_CONTAINER = register(new SpectatorContainer(5, 10.5,20.5), "spectator_huge");
    public static final SpectatorContainer INFINITE_SPECTATOR_CONTAINER = register(new SpectatorContainer(Integer.MAX_VALUE, 10.5,20.5), "spectator_infinite");

    public static final CageSpectatorContainer CAGE_SPECTATOR_CONTAINER = register(new CageSpectatorContainer(10.5, 20.5), "cage");

    public static <I extends AbstractContainer> I register(I container, String id) {
        Identifier itemID = PlayerContainer.identifier(id);
        I registeredItem = Registry.register(REGISTRY, itemID, container);
        return registeredItem;
    }

    public static void initialize() {}

}
