package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.ItemStack;

@Mixin(StorageMinecartEntity.class)
public class StorageMinecartEntityMixin {
    @Inject(method = "markDirty()V", at = @At("HEAD"))
    public void trackContainerMovement(CallbackInfo ci) {
        StorageMinecartEntity thisSM = ((StorageMinecartEntity)(Object)this);
        if (!thisSM.getWorld().isClient()) {
            for(int i = 0; i < thisSM.size(); ++i) {
                ItemStack stack = thisSM.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ContainerInstanceHolder<?> containerItem) {
                    containerItem.getOrMakeContainerInstance(stack, thisSM.getWorld()).setOwner(thisSM);
                }
            }
        }
    }
}
