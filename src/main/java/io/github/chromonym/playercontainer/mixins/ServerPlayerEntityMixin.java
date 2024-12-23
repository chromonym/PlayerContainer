package io.github.chromonym.playercontainer.mixins;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "playerTick()V", at = @At("HEAD"))
    public void capturedPlayerTick(CallbackInfo ci) {
        ServerPlayerEntity thisPE = (ServerPlayerEntity)(Object)this;
        if (ContainerInstance.players.containsKey(thisPE.getGameProfile())) {
            UUID containerID = ContainerInstance.players.get(thisPE.getGameProfile());
            if (ContainerInstance.containers.containsKey(containerID)) {
                ContainerInstance<?> cont = ContainerInstance.containers.get(containerID);
                if (cont != null) {cont.onPlayerTick(thisPE);}
            }
        }
    }

}
