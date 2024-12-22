package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "onEquipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    public void trackContainerMovement(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo ci) {
        if (!ItemStack.areItemsAndComponentsEqual(oldStack, newStack) && !((LivingEntity)(Object)this).getWorld().isClient()) {
            if (newStack.getItem() instanceof AbstractContainerItem<?> sci) {
                sci.getOrMakeContainerInstance(newStack, ((LivingEntity)(Object)this).getWorld()).setOwner((LivingEntity)(Object)this);
            }
        }
    }

    @Inject(method = "triggerItemPickedUpByEntityCriteria(Lnet/minecraft/entity/ItemEntity;)V", at = @At("HEAD"))
    public void trackContainerMovementB(ItemEntity item, CallbackInfo ci) {
        if (!((LivingEntity)(Object)this).getWorld().isClient() && item.getStack().getItem() instanceof AbstractContainerItem<?> sci) {
            sci.getOrMakeContainerInstance(item.getStack(), ((LivingEntity)(Object)this).getWorld()).setOwner((LivingEntity)(Object)this);
        }
    }

    @Inject(method="sendPickup(Lnet/minecraft/entity/Entity;I)V", at = @At("HEAD"))
    public void trackContainerMovement(Entity item, int count, CallbackInfo ci) {
        if (!item.isRemoved() && !((LivingEntity)(Object)this).getWorld().isClient() && item instanceof ItemEntity ie) {
            if (ie.getStack().getItem() instanceof AbstractContainerItem<?> sci) {
                sci.getOrMakeContainerInstance(ie.getStack(), ((LivingEntity)(Object)this).getWorld()).setOwner((LivingEntity)(Object)this);;
            }
        }
    }
}
