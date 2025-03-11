package io.github.chromonym.playercontainer;

import java.util.Set;

import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
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
        if (stack.getItem() instanceof ContainerInstanceHolder aci) {
            ContainerInstance<?> ci = aci.getOrMakeContainerInstance(stack, mci.world, true);
            if (ci != null) {
                Set<GameProfile> players = ci.getPlayers(true);
                GameProfile prof;
                if (!players.isEmpty()) {
                    prof = players.iterator().next();
                } else {
                    prof = mci.getGameProfile(); // use self as fallback
                }
                matrices.push();
                switch (mode) {
                    case FIRST_PERSON_LEFT_HAND:
                        matrices.translate(-0.25f, -0.25f, 0.0f);
                        matrices.scale(0.625f, 0.625f, 0.625f);
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(90.0),0f,1f,0f)));
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(-25.0),0f,0f,1f)));
                        break;
                    case FIRST_PERSON_RIGHT_HAND:
                        matrices.translate(1.25f, -0.25f, 0.0f);
                        matrices.scale(0.625f, 0.625f, 0.625f);
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(-90.0),0f,1f,0f)));
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(25.0),0f,0f,1f)));
                        break;
                    case FIXED:
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(180.0),0f,1f,0f)));
                        matrices.translate(-0.5f, 0f, -0.5f);
                        matrices.scale(0.5f, 0.5f, 0.5f);
                        break;
                    case GROUND:
                        matrices.translate(0.5f, 0.25f, 0.5f);
                        matrices.scale(0.3125f, 0.3125f, 0.3125f);
                        break;
                    case GUI:
                        matrices.translate(0.5f, 0f, 0f);
                        matrices.scale(0.5f, 0.5f, 0.5f);
                        //matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(20.0),1f,0f,0f)));
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(-20.0),0f,1f,0f)));
                        break;
                    case HEAD:
                        matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(180.0),0f,1f,0f)));
                        matrices.translate(-0.5f, 0.75f, -0.5f);
                        matrices.scale(0.5f, 0.5f, 0.5f);
                        break;
                    case THIRD_PERSON_LEFT_HAND:
                        matrices.translate(0.5f, 0.3125f, 0.5625f);
                        matrices.scale(0.3125f, 0.3125f, 0.3125f);
                        break;
                    case THIRD_PERSON_RIGHT_HAND:
                        matrices.translate(0.5f, 0.3125f, 0.5625f);
                        matrices.scale(0.3125f, 0.3125f, 0.3125f);
                        break;
                    default:
                        break;
                    
                }
                NeverSpectatorPlayer player = new NeverSpectatorPlayer(mci.world, prof);
                EntityRenderer renderer = mci.getEntityRenderDispatcher().getRenderer(player);
                renderer.render(player, 0.0f, 0.0f, matrices, vertexConsumers, light);
                matrices.pop();
                return;
            }
        }
    }
    
}
