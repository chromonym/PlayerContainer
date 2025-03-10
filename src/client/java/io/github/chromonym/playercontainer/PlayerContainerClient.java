package io.github.chromonym.playercontainer;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.ContainerInstanceHolder;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.registries.Blocks;
import io.github.chromonym.playercontainer.registries.Items;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class PlayerContainerClient implements ClientModInitializer {

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
						ContainerInstance.containers.put(uuid, container);
						PlayerContainer.LOGGER.info("Recieved "+uuid.toString()+" ("+container.getContainerKey()+")");
					});
					payload.players().forEach((player, container) -> {
						ContainerInstance.players.put(player, container);
						PlayerContainer.LOGGER.info("Recieved "+
							player.getName()+" in "+container.toString());
					});
				}
			});
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