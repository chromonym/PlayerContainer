package io.github.chromonym.playercontainer.registries;

import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;

public class ItemComponents {

    public static final ComponentType<UUID> CONTAINER_ID = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        PlayerContainer.identifier("id"),
        ComponentType.<UUID>builder().codec(Uuids.CODEC).build()
    );
    public static final ComponentType<String> PLAYER_NAME = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        PlayerContainer.identifier("name"),
        ComponentType.<String>builder().codec(Codecs.NON_EMPTY_STRING).build()
    );
    
    public static void initialize() {}
    
}
