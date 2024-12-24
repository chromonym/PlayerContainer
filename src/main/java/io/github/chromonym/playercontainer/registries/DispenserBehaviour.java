package io.github.chromonym.playercontainer.registries;

import io.github.chromonym.playercontainer.containers.ContainerDispenserBehaviour;
import net.minecraft.block.DispenserBlock;

public class DispenserBehaviour {

    public static final ContainerDispenserBehaviour CONTAINER_BEHAVIOUR = new ContainerDispenserBehaviour();
    
    public static void initialize() {
        DispenserBlock.registerBehavior(Items.basicContainer, CONTAINER_BEHAVIOUR);
        DispenserBlock.registerBehavior(Items.debugContainer, CONTAINER_BEHAVIOUR);
        DispenserBlock.registerBehavior(Items.hugeContainer, CONTAINER_BEHAVIOUR);
        DispenserBlock.registerBehavior(Items.largeContainer, CONTAINER_BEHAVIOUR);
        DispenserBlock.registerBehavior(Items.singularityContainer, CONTAINER_BEHAVIOUR);
    }

}
