package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "onEquipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    public void trackContainerMovement(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo ci) {
        if (!ItemStack.areItemsAndComponentsEqual(oldStack, newStack) && !((LivingEntity)(Object)this).getWorld().isClient()) {
            if (newStack.getItem() instanceof SimpleContainerItem sci) {
                sci.getOrMakeContainerInstance(newStack, ((LivingEntity)(Object)this).getWorld()).setOwner((LivingEntity)(Object)this);
            }
        }
    }
}
