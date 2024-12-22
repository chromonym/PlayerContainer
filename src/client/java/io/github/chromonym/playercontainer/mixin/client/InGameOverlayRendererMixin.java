package io.github.chromonym.playercontainer.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.containers.SpectatorContainer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @Invoker("getInWallBlockState")
    private static BlockState invokeGetInWallBlockState(PlayerEntity player) {
        throw new AssertionError();
    }

    @Invoker("renderInWallOverlay")
    private static void invokeRenderInWallOverlay(Sprite sprite, MatrixStack matrices) {
        throw new AssertionError();
    }

    @Inject(method = "renderOverlays(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("HEAD"))
    private static void forceBlockOverlayIfCaptured(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        PlayerEntity playerEntity = client.player;
        if (playerEntity.noClip && ContainerInstance.players.containsKey(playerEntity.getGameProfile())) {
            ContainerInstance<?> conti = ContainerInstance.containers.get(ContainerInstance.players.get(playerEntity.getGameProfile()));
            if (conti != null && conti.getContainer() instanceof SpectatorContainer) {
                BlockState blockState = invokeGetInWallBlockState(playerEntity);
                if (blockState != null) {
                    invokeRenderInWallOverlay(client.getBlockRenderManager().getModels().getModelParticleSprite(blockState), matrices);
                }
            }
        }
    }
}
