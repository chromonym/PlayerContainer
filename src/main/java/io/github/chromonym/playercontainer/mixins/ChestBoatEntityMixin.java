package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.item.ItemStack;

@Mixin(ChestBoatEntity.class)
public class ChestBoatEntityMixin {
    @Inject(method = "markDirty()V", at = @At("HEAD"))
    public void trackContainerMovement(CallbackInfo ci) {
        ChestBoatEntity thisCB = ((ChestBoatEntity)(Object)this);
        if (!thisCB.getWorld().isClient()) {
            for(int i = 0; i < thisCB.size(); ++i) {
                ItemStack stack = thisCB.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof SimpleContainerItem<?> containerItem) {
                    containerItem.getOrMakeContainerInstance(stack, thisCB.getWorld()).setOwner(thisCB);
                }
            }
        }
    }
}
