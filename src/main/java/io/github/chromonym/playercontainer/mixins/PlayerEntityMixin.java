package io.github.chromonym.playercontainer.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setOnGround(Z)V"))
    public void disableCapturedPlayerNoclip(CallbackInfo ci) {
        // i fucking love mixins!!!!!!11!1!!!11
        PlayerEntity pe = (PlayerEntity)(Object)this;
        if (ContainerInstance.players.containsKey(pe.getGameProfile())) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(pe.getGameProfile()));
            if (conti.getContainer() instanceof SpectatorContainer) {
                // if this player is captured in a SpectatorContainer, don't let them move through blocks
                pe.noClip = false;
            }
        }
    }

}
