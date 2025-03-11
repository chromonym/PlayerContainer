package io.github.chromonym.playercontainer.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Shadow
    private MinecraftClient client;

    @Inject(method = "isFlyingLocked()Z", at = @At("HEAD"), cancellable = true)
    public void allowContainedPlayersToLand(CallbackInfoReturnable<Boolean> cir) {
        GameProfile profile = client.getGameProfile();
        if (ContainerInstance.players.containsKey(profile)) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(profile));
            if (conti.getContainer() instanceof SpectatorContainer) {
                // if this player is captured in a SpectatorContainer, let them land etc
                cir.setReturnValue(false);
            }
        }
    }
}
