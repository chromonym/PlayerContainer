package io.github.chromonym.playercontainer.mixins;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Shadow int pickupDelay;
    @Shadow UUID owner;

    @Inject(method="<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    public void constructorOverride(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        updateContainerInstance(world, stack, x, y, z);
    }

    @Inject(method="<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;DDD)V", at = @At("TAIL"))
    public void constructorOverrideLong(World world, double x, double y, double z, ItemStack stack, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        updateContainerInstance(world, stack, x, y, z);
    }

    @Inject(method="tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;discard()V"))
    public void sanityCheck(CallbackInfo ci) {
        ItemEntity thisIE = ((ItemEntity)(Object)this);
        if (thisIE.getStack().getItem() instanceof AbstractContainerItem<?> sci && !thisIE.getWorld().isClient()) {
            sci.getOrMakeContainerInstance(thisIE.getStack(), thisIE.getWorld()).destroy(thisIE.getServer().getPlayerManager(), thisIE.getBlockPos());
        }
    }

    /*@Inject(method="onPlayerCollision(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At(value = "INVOKE", target="Lnet/minecraft/entity/player/PlayerEntity;sendPickup(Lnet/minecraft/entity/Entity;I)V"))
    public void trackContainerMovement(PlayerEntity player, CallbackInfo ci) {
        ItemEntity thisIE = (ItemEntity)(Object)this;
        if (this.pickupDelay == 0 && (this.owner == null || this.owner.equals(player.getUuid())) && player.hasR)
        if (thisIE.getStack().getItem() instanceof AbstractContainerItem<?> sci) {
            PlayerContainer.LOGGER.info("Attempting to set owner");
            sci.getOrMakeContainerInstance(thisIE.getStack(), player.getWorld()).setOwner(player);
            PlayerContainer.LOGGER.info("Finished attempting to set owner");
        }
    }*/

    @Unique
    public void updateContainerInstance(World world, ItemStack stack, double x, double y, double z) {
        UUID contID = stack.get(ItemComponents.CONTAINER_ID);
        if (contID != null) {
            ContainerInstance<?> container = ContainerInstance.containers.get(contID);
            if (container != null) {
                container.setOwner((ItemEntity)(Object)this);
            }
        }
    }
}
