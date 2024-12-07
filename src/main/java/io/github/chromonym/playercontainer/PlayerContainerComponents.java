package io.github.chromonym.playercontainer;

import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class PlayerContainerComponents {

    public static final ComponentType<Integer> CONTAINER_ID = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(PlayerContainer.MOD_ID, "id"),
        ComponentType.<Integer>builder().codec(Codecs.NONNEGATIVE_INT).build()
    );
    
    public static void initialize() {}
    
}
