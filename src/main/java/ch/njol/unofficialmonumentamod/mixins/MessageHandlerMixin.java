package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {
    @Inject(method = "onChatMessage", at = @At("TAIL"))
    private void umm$onChatMessage(SignedMessage message, MessageType.Parameters params, CallbackInfo ci) {
        if (!Locations.getShortShard().equals("isles") || !UnofficialMonumentaModClient.options.enableDelveRecognition) {
            return;	//stop it on triggering either on another shard (outside dev environment) than a possible one or if the feature is disabled
        }
        for (Map.Entry<String, ShardData.Shard> entry: ShardData.getShards().entrySet()) {
            if (!entry.getValue().canBeDelveBounty) {
                continue;//skip if shard cannot be delve bounty
            }

            MutableText translatedText = MutableText.of(new TranslatableTextContent("unofficial-monumenta-mod.delvebounty." + entry.getKey().toLowerCase()));

            if (Objects.equals(message.getContent().getString(), translatedText.getString())) {
                MutableText text = MutableText.of(new TranslatableTextContent("unofficial-monumenta-mod.delvebounty.newBountyMessage"))
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                        .append(MutableText.of(new LiteralTextContent(entry.getValue().officialName)).setStyle(Style.EMPTY.withColor(Formatting.RED)));
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
                break;
            }
        }
    }
}
