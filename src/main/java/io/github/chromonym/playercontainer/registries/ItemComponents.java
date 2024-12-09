package io.github.chromonym.playercontainer.registries;

import java.util.UUID;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public class ItemComponents {

    public static final ComponentType<UUID> CONTAINER_ID = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(PlayerContainer.MOD_ID, "id"),
        ComponentType.<UUID>builder().codec(Uuids.CODEC).build()
    );
    
    public static void initialize() {}
    
}
