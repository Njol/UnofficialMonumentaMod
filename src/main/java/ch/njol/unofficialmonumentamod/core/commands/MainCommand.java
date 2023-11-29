package ch.njol.unofficialmonumentamod.core.commands;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.core.PersistentData;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class MainCommand extends Constants {
    public LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("umm");

        builder.then(ClientCommandManager.literal("disableChestCountError").executes((ctx) -> runExecuteDisableChestCountError()));

        builder.then(ClientCommandManager.literal("streamermode")
                .then(ClientCommandManager.literal("enable").executes((ctx) -> switchStreamerMode(true)))
                .then(ClientCommandManager.literal("disable").executes((ctx) -> switchStreamerMode(false)))
        );

        builder.then(ClientCommandManager.literal("debug").then(ClientCommandManager.literal("addCount").then(ClientCommandManager.argument("count", IntegerArgumentType.integer(0)).executes((MainCommand::runAddCount)))));

        builder.then(ClientCommandManager.literal("info").executes(ctx -> runSelfInfo()));
        builder.then(ClientCommandManager.literal("info")
                .then(ClientCommandManager.literal("modlist")
                        .then(ClientCommandManager.literal("clip").executes(ctx -> runCopyInfo()))
                        .executes(ctx -> runModList())));

        builder.then(ClientCommandManager.literal("persistence")
                .then(ClientCommandManager.literal("list").executes(ctx -> runListPersistentData())));

        return builder;
    }

    public String getName() {
        return MainCommand.class.getSimpleName();
    }

    private static int runListPersistentData() {
        MutableText text = Text.literal("Persistent data:");
        text.setStyle(Constants.MAIN_INFO_STYLE);

        PersistentData persistence = PersistentData.getInstance();

        if (persistence.chestCount != null && !persistence.chestCount.isEmpty()) {
            MutableText chestCountText = Text.literal("\nChest Count:\n%s".formatted(persistence.chestCount.toString()));
            chestCountText.setStyle(Constants.VALUE_STYLE);
            text.append(chestCountText);
        }


        if (persistence.delveBounty != null && !persistence.delveBounty.isEmpty()) {
            MutableText delveBountyText = Text.literal("\nDelve bounty:\n%s".formatted(persistence.delveBounty.toString()));
            delveBountyText.setStyle(Constants.VALUE_STYLE);
            text.append(delveBountyText);
        }

        long weekly = Utils.getNextWeeklyReset();
        long currentTime = System.currentTimeMillis();

        long timeTillWeekly = (weekly - currentTime) / 1000;
        //get days
        int daysTW = (int) (timeTillWeekly / 86400);
        timeTillWeekly %= 86400;
        //get hours
        int hoursTW = (int) (timeTillWeekly / 3600);
        timeTillWeekly %= 3600;
        //get minutes
        int minutesTW = (int) (timeTillWeekly / 60);
        timeTillWeekly %= 60;

        String timeTillDailyDate = "%02dH:%02dM:%02dS".formatted(hoursTW, minutesTW, timeTillWeekly);
        String timeTillWeeklyDate = "%2d days %02dH:%02dM:%02dS".formatted(daysTW, hoursTW, minutesTW, timeTillWeekly);

        MutableText serverInfo = Text.literal("\nServer Info: ").setStyle(Constants.MAIN_INFO_STYLE);
        serverInfo.append(Text.literal("\nTime till next daily reset: ").setStyle(Constants.KEY_INFO_STYLE).append(Text.literal(timeTillDailyDate).setStyle(Constants.VALUE_STYLE)));
        serverInfo.append(Text.literal("\nTime till next weekly reset: ").setStyle(Constants.KEY_INFO_STYLE).append(Text.literal(timeTillWeeklyDate).setStyle(Constants.VALUE_STYLE)));

        text.append(serverInfo);

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        return 0;
    }

    private static int switchStreamerMode(boolean enable) {
        try {
            UnofficialMonumentaModClient.options.hideShardMode = enable;
            UnofficialMonumentaModClient.options.onUpdate();
        } catch (Exception e) {
            UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to switch streamer mode " + (enable ? "on" : "off"), e);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("[UMM] Caught error whilst trying to " + (enable ? "enable" : "disable") + " streamer mode").setStyle(ERROR_STYLE));
            return 1;
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("[UMM] " + (enable ? "enabled" : "disabled") + " streamer mode").setStyle(MAIN_INFO_STYLE));
        return 0;
    }

    private static String getSelfInfoString() {
        StringBuilder data = new StringBuilder();
        data.append("[Mod Info]");

        if (FabricLoader.getInstance().isModLoaded(UnofficialMonumentaModClient.MOD_IDENTIFIER)) {
            ModContainer container = FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).get();

            ModMetadata thisMetadata = container.getMetadata();
            Version version = thisMetadata.getVersion();
            String name = thisMetadata.getName();
            data.append("\nName: ").append(name);
            data.append("\nVersion: ").append(version.getFriendlyString());
            data.append("\nFile name: ").append(!FabricLoader.getInstance().isDevelopmentEnvironment() ? container.getOrigin().getPaths().get(0).getFileName().toString() : "Unknown");
        }

        data.append("\nMinecraft: ") .append(MinecraftClient.getInstance().getGameVersion()).append("-").append(SharedConstants.getGameVersion().getName());
        data.append("\nIn Development environment: ").append(FabricLoader.getInstance().isDevelopmentEnvironment() ? "Yes" : "No");
        return data.append("\n").toString();
    }

    private static String getModListString() {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        StringBuilder data = new StringBuilder();

        data.append("[Mod List]");
        for (ModContainer mod: mods) {
            ModMetadata metadata = mod.getMetadata();
            if (metadata.getId().startsWith("fabric-") || metadata.getId().equals("minecraft") || metadata.getId().equals("java")) {
                continue;//Skip fabric apis, Minecraft and Java.
            }
            data.append("\n").append(metadata.getName()).append(" (").append(metadata.getId()).append(") ").append(metadata.getVersion().getFriendlyString());
            if (mod.getContainingMod().isPresent()) {
                data.append(" via ").append(mod.getContainingMod().get().getMetadata().getId());
            }
        }

        return data.append("\n").toString();
    }

    private static int runCopyInfo() {
        MinecraftClient.getInstance().keyboard.setClipboard(getSelfInfoString().concat(getModListString()));
        MutableText text = Text.literal("Copied info to clipboard").setStyle(SUB_INFO_STYLE);

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        return 0;
    }

    private static int runSelfInfo() {
        if (FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).isEmpty()) {
            return 1;
        }
        ModContainer container = FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).get();

        ModMetadata thisMetadata = container.getMetadata();
        Version version = thisMetadata.getVersion();
        String name = thisMetadata.getName();

        MutableText text = Text.literal("[Mod Info]").setStyle(MAIN_INFO_STYLE);

        text.append(Text.literal("\nName: ").setStyle(KEY_INFO_STYLE));
        text.append(Text.literal(name).setStyle(VALUE_STYLE));

        text.append(Text.literal("\nFile name: ").setStyle(KEY_INFO_STYLE));
        text.append(Text.literal(!FabricLoader.getInstance().isDevelopmentEnvironment() ? container.getOrigin().getPaths().get(0).getFileName().toString() : "Unknown").setStyle(VALUE_STYLE));

        text.append(Text.literal("\nVersion: ").setStyle(KEY_INFO_STYLE));
        text.append(Text.literal(version.getFriendlyString()).setStyle(VALUE_STYLE));

        text.append(Text.literal("\nMinecraft: ").setStyle(KEY_INFO_STYLE));
        text.append(Text.literal(MinecraftClient.getInstance().getGameVersion() + "-" + SharedConstants.getGameVersion().getName()).setStyle(VALUE_STYLE));

        text.append(Text.literal("\nIn Development environment: ").setStyle(KEY_INFO_STYLE));
        text.append(Text.literal(FabricLoader.getInstance().isDevelopmentEnvironment() ? "Yes" : "No").setStyle(VALUE_STYLE));

        //other "pages"
        text.append(Text.literal("\n[Press Here to show modlist]").setStyle(SUB_INFO_STYLE.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info modlist"))));
        text.append(Text.literal("\n[Press Here to show current shard]").setStyle(SUB_INFO_STYLE.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ummShard debug loaded"))));

        //copy all info to clipboard
        text.append(Text.literal("\n[Press Here to copy to clipboard]").setStyle(SUB_INFO_STYLE.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info clip"))));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    private static int runModList() {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        MutableText text = Text.literal("[Mod List]").setStyle(MAIN_INFO_STYLE);

        for (ModContainer mod: mods) {
            ModMetadata metadata = mod.getMetadata();
            if (metadata.getId().startsWith("fabric-") || metadata.getId().equals("minecraft") || metadata.getId().equals("java")) {
                continue;//Skip fabric apis, Minecraft and Java.
            }

            MutableText modText = Text.literal("\n" + metadata.getName()).setStyle(MOD_INFO_STYLE);
            modText.append(Text.literal(" (" + metadata.getId() + ") ").setStyle(KEY_INFO_STYLE));
            modText.append(Text.literal(metadata.getVersion().getFriendlyString()).setStyle(MOD_INFO_STYLE));

            text.append(modText);
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    public static int runExecuteDisableChestCountError() {
        if (!UnofficialMonumentaModClient.options.enableChestCountMaxError) {
            return 1;
        }
        UnofficialMonumentaModClient.options.enableChestCountMaxError = false;
        //wouldn't want to mitigate the effect of the command.
        UnofficialMonumentaModClient.saveConfig();
        MutableText text = Text.literal("[UMM] Successfully disabled warning message").setStyle(MAIN_INFO_STYLE);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    public static int runAddCount(CommandContext<FabricClientCommandSource> commandContext) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            MutableText text = Text.literal("[UMM] nuh uh, not happening.").setStyle(ERROR_STYLE);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
            return 1;
        }
        int count = IntegerArgumentType.getInteger(commandContext, "count");
        ChestCountOverlay.INSTANCE.addCount(count);

        MutableText text = Text.literal("[UMM] added " + count + " to chestCountOverlay").setStyle(MAIN_INFO_STYLE);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }
}