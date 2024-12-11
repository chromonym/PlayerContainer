package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {
    @Inject(method = "setHeldItemStack(Lnet/minecraft/item/ItemStack;Z)V", at = @At("HEAD"))
    public void trackContainerMovement(ItemStack stack, boolean update, CallbackInfo ci) {
        ItemFrameEntity thisIF = (ItemFrameEntity)(Object)this;
        if (!thisIF.getWorld().isClient() && !stack.isEmpty() && stack.getItem() instanceof SimpleContainerItem<?> containerItem) {
            containerItem.getOrMakeContainerInstance(stack, thisIF.getWorld()).setOwner(thisIF);
        }
    }
}
