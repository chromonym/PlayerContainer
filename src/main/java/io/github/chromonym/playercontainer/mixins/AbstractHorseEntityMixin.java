package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

@Mixin(AbstractHorseEntity.class)
public class AbstractHorseEntityMixin {
    @Inject(method = "onInventoryChanged(Lnet/minecraft/inventory/Inventory;)V", at = @At("HEAD"))
    public void trackContainerMovement(Inventory inv, CallbackInfo ci) {
        AbstractHorseEntity thisAH = ((AbstractHorseEntity)(Object)this);
        for(int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SimpleContainerItem<?> containerItem && !thisAH.getWorld().isClient()) {
                containerItem.getOrMakeContainerInstance(stack, thisAH.getWorld()).setOwner(thisAH);
            }
        }
    }
}
