package io.github.chromonym.playercontainer.mixins;

import java.util.Map.Entry;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Invoker
    public abstract World getGetWorld(); // this is so silly

    @Inject(method = "setUuid(Ljava/util/UUID;)V", at = @At("HEAD"))
    public void trackOwnerUpdating(UUID uuid, CallbackInfo ci) {
        Entity thisE = (Entity)(Object)this;
        for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
            Optional<Entity> owner = entry.getValue().getOwner().left();
            if (owner.isPresent() && owner.get().getUuid() == uuid) {
                entry.getValue().setOwner(thisE);
            }
        }
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    public void trackContainerMovement(CallbackInfo ci) {
        Entity thisE = (Entity)(Object)this;
        if ((Entity)(Object)this instanceof Inventory inv) {
            if (!thisE.getWorld().isClient()) {
                for(int i = 0; i < inv.size(); ++i) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ContainerInstanceHolder<?> containerItem) {
                        containerItem.getOrMakeContainerInstance(stack, thisE.getWorld()).setOwner(thisE);
                    }
                }
            }
        }
    }

    @Inject(method = "setRemoved(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    public void trackContainerRemoval(RemovalReason reason, CallbackInfo ci) {
        if (!this.getGetWorld().isClient()) {
            Set<ContainerInstance<?>> toDestroy = new HashSet<ContainerInstance<?>>();
            Set<ContainerInstance<?>> toRemoveAll = new HashSet<ContainerInstance<?>>();
            Entity thisE = (Entity)(Object)this;
            for (Entry<UUID, ContainerInstance<?>> entry : ContainerInstance.containers.entrySet()) {
                Optional<Entity> owner = entry.getValue().getOwner().left();
                if (owner.isPresent() && owner.get().getUuid() == thisE.getUuid()) {
                    if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
                        toRemoveAll.add(entry.getValue()); // entry.getValue().releaseAll(thisE.getWorld(), true);
                    } else if ((reason == RemovalReason.DISCARDED && !((Entity)(Object)this instanceof ItemEntity)) ||
                    (reason == RemovalReason.KILLED && ((Entity)(Object)this instanceof ItemEntity))){
                        // entity despawned (or itemEntity is killed)
                        toDestroy.add(entry.getValue()); // entry.getValue().destroy(thisE.getWorld());
                    }
                    // other than that there should be another entity that becomes the new owner? i hope?
                }
            }
            for (ContainerInstance<?> cont : toDestroy) {
                cont.destroy(thisE.getServer().getPlayerManager(), thisE.getBlockPos());
            }
            for (ContainerInstance<?> cont : toRemoveAll) {
                cont.releaseAll(thisE.getServer().getPlayerManager(), true, thisE.getBlockPos());
            }
        }
    }
}
