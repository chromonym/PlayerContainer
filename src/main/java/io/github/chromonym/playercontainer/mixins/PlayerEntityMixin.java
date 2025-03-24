package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity.MoveEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    
    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setOnGround(Z)V"))
    public void disableCapturedPlayerNoclip(PlayerEntity pe, boolean b) {
        // i fucking love mixins!!!!!!11!1!!!11
        if (ContainerInstance.players.containsKey(pe.getGameProfile()) && !b) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(pe.getGameProfile()));
            if (conti != null && conti.getContainer() instanceof SpectatorContainer) {
                // if this player is captured in a SpectatorContainer, don't let them move through blocks (except the container block)
                conti.getOwner().ifLeft(entity -> {
                    pe.noClip = false;
                }).ifRight(blockEntity -> {
                    pe.noClip = false;
                });
            }
            return;
        }
        pe.setOnGround(b);
    }

    @Inject(method = "playStepSound(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("HEAD"), cancellable = true)
    public void makeCapturedSpectatorsQuieter(BlockPos pos, BlockState state, CallbackInfo cir) {
        GameProfile profile = ((PlayerEntity)(Object)this).getGameProfile();
        if (((PlayerEntity)(Object)this).isSpectator() && ContainerInstance.players.containsKey(profile)) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(profile));
            if (conti != null && conti.getContainer() instanceof SpectatorContainer) {
                // if this player is captured in a SpectatorContainer, don't make noises
                cir.cancel();
            }
        }
    }

    @Inject(method = "getMoveEffect()Lnet/minecraft/entity/Entity$MoveEffect;", at = @At("HEAD"), cancellable = true)
    public void makeCapturedSpectatorsEvenQuieter(CallbackInfoReturnable<MoveEffect> cir) {
        GameProfile profile = ((PlayerEntity)(Object)this).getGameProfile();
        if (((PlayerEntity)(Object)this).isSpectator() && ContainerInstance.players.containsKey(profile)) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(profile));
            if (conti != null && conti.getContainer() instanceof SpectatorContainer) {
                // if this player is captured in a SpectatorContainer, don't make noises
                cir.setReturnValue(MoveEffect.NONE);
            }
        }
    }

}
