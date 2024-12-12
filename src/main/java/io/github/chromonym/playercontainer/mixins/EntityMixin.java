package io.github.chromonym.playercontainer.mixins;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.PlayerContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.SimpleContainerItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    public void trackContainerMovement(CallbackInfo ci) {
        Entity thisE = (Entity)(Object)this;
        if ((Entity)(Object)this instanceof Inventory inv) {
            if (!thisE.getWorld().isClient()) {
                for(int i = 0; i < inv.size(); ++i) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof SimpleContainerItem<?> containerItem) {
                        containerItem.getOrMakeContainerInstance(stack, thisE.getWorld()).setOwner(thisE);
                    }
                }
            }
        }
    }

    @Inject(method = "setRemoved(Lnet/minecraft/entity/Entity/RemovalReason;)V", at = @At("HEAD"))
    public void trackContainerRemoval(RemovalReason reason, CallbackInfo ci) {
        Entity thisE = (Entity)(Object)this;
        for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
            Optional<Entity> owner = entry.getValue().getOwner().left();
            if (owner.isPresent() && owner.get().getUuid() == thisE.getUuid()) {
                if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
                    PlayerContainer.LOGGER.info("Entity unloaded: "+thisE.getNameForScoreboard());
                    entry.getValue().releaseAll(thisE.getWorld(), true);
                } else if (reason == RemovalReason.DISCARDED ||
                (reason == RemovalReason.KILLED && ((Entity)(Object)this instanceof ItemEntity))){
                    // entity despawned (or itemEntity is killed)
                    PlayerContainer.LOGGER.info("Entity despawned/killed: "+thisE.getNameForScoreboard());
                    entry.getValue().destroy(thisE.getWorld());
                    //entry.getValue().releaseAll(thisE.getWorld(), false);
                }
                // other than that there should be another entity that becomes the new owner? i hope?
            }
        }
        /*if (thisE instanceof PlayerEntity player && reason ==) {
            if (ContainerInstance.players.keySet().contains(player.getGameProfile())) {
                // player is in a container
                ContainerInstance.containers.get(ContainerInstance.players.get(player.getGameProfile())).release(player, true); // temporarily release that player
            }
        }*/
    }
}
