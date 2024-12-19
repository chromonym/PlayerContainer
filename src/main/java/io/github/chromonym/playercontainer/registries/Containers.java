package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.LoggingContainer;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class Containers {
    
    public static final RegistryKey<Registry<AbstractContainer>> REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.of(PlayerContainer.MOD_ID, "containers"));
    public static final Registry<AbstractContainer> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    public static final LoggingContainer LOGGING_CONTAINER = register(new LoggingContainer(), "logging");
    public static final SpectatorContainer SPECTATOR_CONTAINER = register(new SpectatorContainer(), "spectator");

    public static <I extends AbstractContainer> I register(I container, String id) {
        Identifier itemID = Identifier.of(PlayerContainer.MOD_ID, id);
        I registeredItem = Registry.register(REGISTRY, itemID, container);
        return registeredItem;
    }

    public static void initialize() {}

}
