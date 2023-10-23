package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique
    //used to limit the amount of synchronization packets it can catch.
    private static long lastUpdate;

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    public void umm$onSynchronizePositionPacket(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        //This packet is the only one sent by the server that could be considered a teleport packet, as the confirmation is sent from C2S.
        if (packet.getTeleportId() != 1 && ShardData.loadedAtLeastOnce && lastUpdate + 1000 < System.currentTimeMillis()) {
            //skip 1st "teleport" as it is synchronization after world join.
            lastUpdate = System.currentTimeMillis();
            ShardData.onPlayerSynchronizePosition();
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    public void umm$onPlayerRespawnPacket(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        ShardData.pebWorldSpoofingShardDetected(packet.getDimension());
        if (UnofficialMonumentaModClient.options.shardDebug) {
            UnofficialMonumentaModClient.LOGGER.info("Loading shard from Player Respawn packet");
        }
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    public void umm$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        ShardData.pebWorldSpoofingShardDetected(packet.dimensionId());
        if (UnofficialMonumentaModClient.options.shardDebug) {
            UnofficialMonumentaModClient.LOGGER.info("Loading shard from Game Join packet");
        }
    }
}
