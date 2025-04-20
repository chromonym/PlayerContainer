package io.github.chromonym.playercontainer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.networking.ReleaseRequestPayload;
import io.github.chromonym.playercontainer.registries.Blocks;
import io.github.chromonym.playercontainer.registries.Items;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class PlayerContainerClient implements ClientModInitializer {

	public static KeyBinding releaseKey;

	public static final ClampedModelPredicateProvider captureCountProvider = (itemStack, clientWorld, livingEntity, seed) -> {
		// returns a float representing the percentage of its maximum size filled
		if (itemStack.getItem() instanceof ContainerInstanceHolder aci) {
			ContainerInstance<?> ci = aci.getOrMakeContainerInstance(itemStack, clientWorld, true);
			if (ci != null) {
				if (ci.getMaxPlayerCount() == 0) {
					return 0.0F;
				}
				float out = ((float)ci.getPlayerCount(true))/((float)ci.getMaxPlayerCount());
				return out;
			}
		}
		return 0.0F;
	};

	public static final ClampedModelPredicateProvider infiniteCaptureCountProvider = (itemStack, clientWorld, livingEntity, seed) -> {
		// returns 0 if it contains no players, 1 if it contains any
		if (itemStack.getItem() instanceof ContainerInstanceHolder aci) {
			ContainerInstance<?> ci = aci.getOrMakeContainerInstance(itemStack, clientWorld, true);
			if (ci != null) {
				if (ci.getPlayerCount(true) > 0) {
					return 1.0F;
				}
			}
		}
		return 0.0F;
	};

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientPlayNetworking.registerGlobalReceiver(ContainerInstancesPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				if (!context.client().isIntegratedServerRunning()) {
					ContainerInstance.containers.clear();
					ContainerInstance.players.clear();
					payload.containers().forEach((uuid, container) -> {
						container.setOwnerClient(context.player().getWorld());
						ContainerInstance.containers.put(uuid, container);
						//PlayerContainer.LOGGER.info("Recieved "+uuid.toString()+" ("+container.getContainerKey()+")");
					});
					payload.players().forEach((player, container) -> {
						ContainerInstance.players.put(player, container);
						//PlayerContainer.LOGGER.info("Recieved "+ player.getName()+" in "+container.toString());
					});
				}
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ContainerInstance.containers.clear();
			ContainerInstance.players.clear();
		});
		releaseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.playercontainer.release", // The translation key of the keybinding's name
			InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
			GLFW.GLFW_KEY_UNKNOWN, // The keycode of the key
			"category.playercontainer" // The translation key of the keybinding's category.
		));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (releaseKey.wasPressed()) {
			ClientPlayNetworking.send(new ReleaseRequestPayload(true));
			}
		});
		ModelPredicateProviderRegistry.register(Items.basicContainer, Identifier.ofVanilla("captured"), captureCountProvider);
		ModelPredicateProviderRegistry.register(Items.largeContainer, Identifier.ofVanilla("captured"), captureCountProvider);
		ModelPredicateProviderRegistry.register(Items.hugeContainer, Identifier.ofVanilla("captured"), captureCountProvider);
		ModelPredicateProviderRegistry.register(Items.singularityContainer, Identifier.ofVanilla("captured"), infiniteCaptureCountProvider);
		ModelPredicateProviderRegistry.register(Items.loosePlayer, Identifier.ofVanilla("captured"), captureCountProvider);
		ModelPredicateProviderRegistry.register(Items.cageBlock, Identifier.ofVanilla("captured"), captureCountProvider);
		ModelPredicateProviderRegistry.register(Items.selfContainer, Identifier.ofVanilla("captured"), captureCountProvider);
		BuiltinItemRendererRegistry.INSTANCE.register(Items.loosePlayer, new LoosePlayerRenderer());
		BlockRenderLayerMap.INSTANCE.putBlock(Blocks.CAGE_BLOCK, RenderLayer.getCutout());
	}
}