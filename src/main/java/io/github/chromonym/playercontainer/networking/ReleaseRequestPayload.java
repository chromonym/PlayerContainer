package io.github.chromonym.playercontainer.networking;

import io.github.chromonym.playercontainer.PlayerContainer;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record ReleaseRequestPayload(boolean toRelease) implements CustomPayload {

    public static final CustomPayload.Id<ReleaseRequestPayload> ID = new CustomPayload.Id<>(PlayerContainer.identifier("request_release"));
    public static final PacketCodec<RegistryByteBuf, ReleaseRequestPayload> CODEC = PacketCodec.tuple(PacketCodecs.BOOL, ReleaseRequestPayload::toRelease, ReleaseRequestPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
}
