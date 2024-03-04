package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardLoader;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    public void umm$onPlayerRespawnPacket(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        ShardLoader.loadWorldFromDimensionKey(packet.getDimension());
        if (UnofficialMonumentaModClient.options.shardDebug) {
            UnofficialMonumentaModClient.LOGGER.info("Loading shard from Player Respawn packet");
        }
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    public void umm$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        ShardLoader.loadWorldFromDimensionKey(packet.dimensionId());
        if (UnofficialMonumentaModClient.options.shardDebug) {
            UnofficialMonumentaModClient.LOGGER.info("Loading shard from Game Join packet");
        }
    }
}
