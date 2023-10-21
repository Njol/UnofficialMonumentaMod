package ch.njol.unofficialmonumentamod.features.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.core.PersistentData;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DelveBounty {
    private static final String NPCNamePattern = "\\[\\w*] ";
    private static String delveBounty = null;
    private static Long delveBountyTime = null;

    public static void initializeListeners() {
        PersistentData.PersistentDataLoadedCallback.EVENT.register((persistentData) -> {
            if (Utils.getNextWeeklyResetOf(persistentData.delveBounty.time) > System.currentTimeMillis()) {
                delveBounty = persistentData.delveBounty.value;
                delveBountyTime = persistentData.delveBounty.time;
            }
        });

        PersistentData.PersistentDataSavingCallback.EVENT.register((persistentData) -> persistentData.delveBounty = new PersistentData.DatedHolder<>(delveBountyTime, delveBounty));
    }

    public static void onMessage(Text message, boolean overlay) {
        if (overlay || !ShardData.getCurrentShard().shortShard.equals("isles") || !UnofficialMonumentaModClient.options.enableDelveRecognition) {
            return;//Ignore if received outside a possible shard or if the feature is disabled.
        }

        String npcText = message.getString();
        String npcMessage = npcText.replaceFirst(NPCNamePattern, "");
        if (npcMessage.equals(npcText)) {
            return;//if the giver is not an NPC, ignore.
        }

        for (Map.Entry<String, ShardData.Shard> entry: ShardData.getShards().entrySet()) {
            if (!entry.getValue().canBeDelveBounty) {
                continue;//If the shard does not have an associated bounty, skip.
            }

            MutableText translatedText = Text.translatable("unofficial-monumenta-mod.delvebounty." + entry.getKey().toLowerCase());
            if (Objects.equals(npcMessage, translatedText.getString())) {
                //set persistent data.
                delveBountyTime = System.currentTimeMillis();
                delveBounty = entry.getKey().toLowerCase();

                MutableText text = Text.translatable("unofficial-monumenta-mod.delvebounty.newBountyMessage")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                        .append(MutableText.of(new LiteralTextContent(entry.getValue().officialName)).setStyle(Style.EMPTY.withColor(Formatting.RED)));
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
                break;
            }
        }
    }
}
