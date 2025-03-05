package io.github.chromonym.playercontainer;

import java.util.Set;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.DynamicItemRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class LoosePlayerRenderer implements DynamicItemRenderer {

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        MinecraftClient mci = MinecraftClient.getInstance();
        if (stack.getItem() instanceof AbstractContainerItem aci) {
            ContainerInstance<?> ci = aci.getOrMakeContainerInstance(stack, mci.world, true);
            if (ci != null) {
                Set<GameProfile> players = ci.getPlayers(true);
                if (!players.isEmpty()) {
                    GameProfile prof = players.iterator().next();
                    // matrices.push();
                    // TODO: rotate, scale, translate the player model based on the ModelTransformationMode
                    NeverSpectatorPlayer player = new NeverSpectatorPlayer(mci.world, prof);
                    EntityRenderer renderer = mci.getEntityRenderDispatcher().getRenderer(player);
                    renderer.render(player, 0.0f, 0.0f, matrices, vertexConsumers, light);
                    // matrices.pop();
                    return;
                }
            }
        }
        // TODO: add fallback item rendering here (also add empty texture - the model already exists)
    }
    
}
