package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    public void umm$onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        //The actual teleport packet being a C2S means I have to use this one (which is the one that tells the server that the teleport was successful.
        if (ShardData.loadedAtLeastOnce) {
            ShardData.onPlayerTeleport();
        }
    }
}