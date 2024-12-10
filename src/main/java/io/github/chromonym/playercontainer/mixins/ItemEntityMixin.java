package io.github.chromonym.playercontainer.mixins;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method="<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    public void constructorOverride(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        updateContainerInstance(world, stack, x, y, z);
    }

    @Inject(method="<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;DDD)V", at = @At("TAIL"))
    public void constructorOverrideLong(World world, double x, double y, double z, ItemStack stack, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        updateContainerInstance(world, stack, x, y, z);
    }

    @Unique
    public void updateContainerInstance(World world, ItemStack stack, double x, double y, double z) {
        UUID contID = stack.get(ItemComponents.CONTAINER_ID);
        if (contID != null) {
            ContainerInstance<?> container = ContainerInstance.containers.get(contID);
            if (container != null) {
                container.getContainer().onReleaseAll(container);
                PlayerContainer.LOGGER.info("ItemStack created at "+Double.toString(x)+", "+Double.toString(y)+", "+Double.toString(z));
            }
        }
    }
}
