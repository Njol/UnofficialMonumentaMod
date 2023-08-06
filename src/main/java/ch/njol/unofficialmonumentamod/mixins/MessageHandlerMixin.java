package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {
    @Unique
    private final String NPCNamePattern = "\\[\\w*] ";

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void umm$onChatMessage(Text message, boolean overlay, CallbackInfo ci) {
        if (overlay || !ShardData.getCurrentShard().shortShard.equals("isles") || !UnofficialMonumentaModClient.options.enableDelveRecognition) {
            return;	//stop it on triggering either on another shard (outside dev environment) than a possible one or if the feature is disabled
        }

        String npcText = message.getString();
        String npcMessage = npcText.replaceFirst(NPCNamePattern, "");
        if (npcMessage.equals(npcText)) {
            return;//if it's not given by an NPC, it should not be used.
        }

        for (Map.Entry<String, ShardData.Shard> entry: ShardData.getShards().entrySet()) {
            if (!entry.getValue().canBeDelveBounty) {
                continue;//skip if shard cannot be delve bounty
            }

            MutableText translatedText = Text.translatable("unofficial-monumenta-mod.delvebounty." + entry.getKey().toLowerCase());
            if (Objects.equals(npcMessage, translatedText.getString())) {
                MutableText text = Text.translatable("unofficial-monumenta-mod.delvebounty.newBountyMessage")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                        .append(MutableText.of(new LiteralTextContent(entry.getValue().officialName)).setStyle(Style.EMPTY.withColor(Formatting.RED)));
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
                break;
            }
        }
    }
}
