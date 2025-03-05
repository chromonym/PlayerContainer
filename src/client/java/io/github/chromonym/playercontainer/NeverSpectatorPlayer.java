package io.github.chromonym.playercontainer;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class NeverSpectatorPlayer extends AbstractClientPlayerEntity {

    public NeverSpectatorPlayer(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }
    
}
