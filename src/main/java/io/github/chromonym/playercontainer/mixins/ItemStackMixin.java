package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "setHolder(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    public void trackContainerMovement(Entity holder, CallbackInfo ci) {
        ItemStack thisIS = (ItemStack)(Object)this;
        if (thisIS.getItem() instanceof ContainerInstanceHolder<?> sci && holder != null) {
            ContainerInstance<?> container = sci.getOrMakeContainerInstance(thisIS, holder.getWorld());
            if (container != null) {
                container.setOwner(holder);
            }
        }
   }
}
