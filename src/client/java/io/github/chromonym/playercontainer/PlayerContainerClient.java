package io.github.chromonym.playercontainer;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.items.AbstractContainerItem;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import io.github.chromonym.playercontainer.registries.Items;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;

public class PlayerContainerClient implements ClientModInitializer {

	public static final ClampedModelPredicateProvider captureCountProvider = (itemStack, clientWorld, livingEntity, seed) -> {
		if (itemStack.getItem() instanceof AbstractContainerItem aci) {
			ContainerInstance<?> ci = aci.getOrMakeContainerInstance(itemStack, clientWorld, true);
			if (ci != null) {
				if (ci.getMaxPlayerCount() == 0) {
					return 0.0F;
				}
				float out = ((float)ci.getPlayerCount())/((float)ci.getMaxPlayerCount());
				return out;
			}
		}
		return 0.0F;
	};

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientPlayNetworking.registerGlobalReceiver(ContainerInstancesPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
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
			});
		});
		ModelPredicateProviderRegistry.register(Items.basicContainer, Identifier.ofVanilla("captured"), captureCountProvider);
	}
}