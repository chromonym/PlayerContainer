package io.github.chromonym.playercontainer;

import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.networking.ContainerInstancesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class PlayerContainerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientPlayNetworking.registerGlobalReceiver(ContainerInstancesPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				ContainerInstance.containers = payload.containers();
				ContainerInstance.players = payload.players();
				ContainerInstance.containers.forEach((uuid, container) -> {
					PlayerContainer.LOGGER.info("Recieved "+uuid.toString()+" ("+container.getContainerKey()+")");
				});
				ContainerInstance.players.forEach((player, container) -> {
					PlayerContainer.LOGGER.info("Recieved "+
						player.toString()+" in "+container.toString());
				});
			});
		});
	}
}