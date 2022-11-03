package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mc.MonumentaModResourceReloader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Final
	@Shadow private ReloadableResourceManagerImpl resourceManager;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;


	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	void disconnect(Screen screen, CallbackInfo ci) {
		UnofficialMonumentaModClient.onDisconnect();
	}


	@Inject(method="<init>", at = @At(value= "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;addResourcePackProfilesToManager(Lnet/minecraft/resource/ResourcePackManager;)V"))
	void init(RunArgs args, CallbackInfo ci) {
		resourceManager.registerReloader(new MonumentaModResourceReloader());
	}
    @Shadow @Nullable public HitResult crosshairTarget;

    // Send a position update before firing a crossbow for the Recoil fix to work
    // NB: not needed on MC 1.17
    @Inject(method = "doItemUse()V", at = @At("HEAD"))
    void doItemUse_crossbowFix(CallbackInfo ci) {
        if (player == null) {
            return;
        }

        if (!UnofficialMonumentaModClient.options.crossbowFix) return;
        if (player.getMainHandStack() != null && player.getMainHandStack().getItem() == Items.CROSSBOW
                || player.getOffHandStack() != null && player.getOffHandStack().getItem() == Items.CROSSBOW) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), player.getPitch(), player.isOnGround()));
        }
    }
}
