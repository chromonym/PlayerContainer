package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setOnGround(Z)V"))
    public void checkForEnderChest(CallbackInfo ci) {
        // i fucking love mixins!!!!!!11!1!!!11
        PlayerEntity pe = (PlayerEntity)(Object)this;
        /*if (ContainerInstance.players.containsKey(pe.getGameProfile())) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(pe.getGameProfile()));
            if (conti.getContainer() instanceof SpectatorContainer) {
                // if this player is captured in a SpectatorContainer, don't let them move through blocks (except the container block)
                conti.getOwner().ifLeft(entity -> {
                    pe.noClip = false;
                }).ifRight(blockEntity -> {
                    if (blockEntity == null || !SpectatorContainer.isWithinBlock(pe.getEyePos(), blockEntity.getPos())) {
                        pe.noClip = false;
                    }
                });
            }
        }*/
        if (!pe.getWorld().isClient()) {
            EnderChestInventory inv = pe.getEnderChestInventory();
            for(int i = 0; i < inv.size(); ++i) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof AbstractContainerItem<?> containerItem) {
                    containerItem.getOrMakeContainerInstance(stack, pe.getWorld()).setOwner(pe);
                }
            }
        }
    }

}
