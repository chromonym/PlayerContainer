package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "markDirty()V", at = @At("HEAD"))
    public void trackContainerMovement(CallbackInfo ci) {
        BlockEntity thisBE = ((BlockEntity)(Object)this);
        if (thisBE instanceof Inventory inv) {
            for(int i = 0; i < inv.size(); ++i) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof SimpleContainerItem<?> containerItem) {
                    containerItem.getOrMakeContainerInstance(stack, thisBE.getWorld()).setOwner(thisBE);
                }
            }
        }
    }
}
