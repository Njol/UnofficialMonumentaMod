package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.features.misc.DelveBounty;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void umm$onChatMessage(Text message, boolean overlay, CallbackInfo ci) {
        DelveBounty.onMessage(message, overlay);
    }
}
