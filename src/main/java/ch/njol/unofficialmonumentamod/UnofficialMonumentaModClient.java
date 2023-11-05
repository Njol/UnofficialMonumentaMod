package ch.njol.unofficialmonumentamod;

import ch.njol.minecraft.config.Config;
import ch.njol.minecraft.uiframework.hud.Hud;
import ch.njol.unofficialmonumentamod.core.PersistentData;
import ch.njol.unofficialmonumentamod.core.commands.MainCommand;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.core.shard.ShardDebugCommand;
import ch.njol.unofficialmonumentamod.features.calculator.Calculator;
import ch.njol.unofficialmonumentamod.features.discordrpc.DiscordPresence;
import ch.njol.unofficialmonumentamod.features.effects.EffectOverlay;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.features.misc.DelveBounty;
import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import ch.njol.unofficialmonumentamod.features.misc.managers.MessageNotifier;
import ch.njol.unofficialmonumentamod.features.misc.notifications.LocationNotifier;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import ch.njol.unofficialmonumentamod.hud.AbilitiesHud;
import ch.njol.unofficialmonumentamod.options.ConfigMenu;
import ch.njol.unofficialmonumentamod.options.Options;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Platform;

@Environment(EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	public static final String MOD_IDENTIFIER = "unofficial-monumenta-mod";

	public static final String OPTIONS_FILE_NAME = "unofficial-monumenta-mod.json";

	public static final Logger LOGGER = LogManager.getLogger(MOD_IDENTIFIER);

	public static Options options = new Options();

	public static Locations locations = new Locations();
	public static TextureSpoofer spoofer = new TextureSpoofer();

	public static DiscordPresence discordRPC = DiscordPresence.INSTANCE;

	public static EffectOverlay effectOverlay = new EffectOverlay();

	public static final AbilityHandler abilityHandler = new AbilityHandler();

	public static KeyBinding toggleCalculatorKeyBinding = new KeyBinding("unofficial-monumenta-mod.keybinds.toggleCalculator", GLFW.GLFW_KEY_K, "unofficial-monumenta-mod.keybinds.category");

	@Override
	public void onInitializeClient() {

		ModelPredicateProviderRegistry.register(new Identifier("on_head"),
			(itemStack, clientWorld, livingEntity, seed) -> livingEntity != null && itemStack == livingEntity.getEquippedStack(EquipmentSlot.HEAD) ? 1 : 0);

		try {
			options = Config.readJsonFile(Options.class, OPTIONS_FILE_NAME);
		} catch (FileNotFoundException e) {
			// Config file doesn't exist, so use default config (and write config file).
			try {
				Config.writeJsonFile(options, OPTIONS_FILE_NAME);
			} catch (IOException ex) {
				// ignore
			}
		} catch (IOException | JsonParseException e) {
			// Any issue with the config file silently reverts to the default config
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to load configuration file", e);
		}

		PersistentData.getInstance().initialize();

		if (options.discordEnabled) {
			if (canInitializeDiscord()) {
				try {
					discordRPC.Init();
				} catch (Exception e) {
					UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to initialize DiscordRPC", e);
				}
			} else {
				UnofficialMonumentaModClient.LOGGER.error("Disabled DiscordRPC as architecture is not compatible.");

				//since it is most likely going to crash, just make sure it doesn't nearly cause it to happen again.
				options.discordEnabled = false;
				options.onUpdate();
				MinecraftClient.getInstance().submit(UnofficialMonumentaModClient::saveConfig);
			}
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			abilityHandler.tick();
			effectOverlay.tick();
			LocationNotifier.tick();
			MessageNotifier.getInstance().tick();
			SlotLocking.getInstance().onEndTick();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			onDisconnect();
		});

		ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
			ShardData.onWorldLoad();
			//TODO if false, re-schedule loading
			PersistentData.getInstance().onLogin();
		}));

		ClientPlayNetworking.registerGlobalReceiver(ChannelHandler.CHANNEL_ID, new ChannelHandler());

		KeyBindingHelper.registerKeyBinding(toggleCalculatorKeyBinding);
		KeyBindingHelper.registerKeyBinding(SlotLocking.LOCK_KEY);

		Hud.INSTANCE.addElement(AbilitiesHud.INSTANCE);
		Hud.INSTANCE.addElement(ChestCountOverlay.INSTANCE);
		Hud.INSTANCE.addElement(effectOverlay);
		Hud.INSTANCE.addElement(MessageNotifier.getInstance());

		DelveBounty.initializeListeners();
		ChestCountOverlay.INSTANCE.initializeListeners();
		Locations.registerListeners();
		Calculator.registerListeners();
		ShardData.ShardChangedEventCallback.EVENT.register((currentShard, previousShard) -> {
			if (options.shardDebug) {
				LOGGER.info("Received shard update: " + previousShard + " -> " + currentShard);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
					dispatcher.register(new ShardDebugCommand().register());
					dispatcher.register(new MainCommand().register());
		}));

		try {
			Class.forName("com.terraformersmc.modmenu.api.ModMenuApi");
			Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
			ConfigMenu.registerTypes();
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Could not load modmenu or cloth-config, disabling ConfigMenu.");
		}
	}

	public static void onDisconnect() {
		abilityHandler.onDisconnect();
		LocationNotifier.onDisconnect();
		spoofer.onDisconnect();
		//TODO if false, re-schedule unload.
		PersistentData.getInstance().onDisconnect();
		SlotLocking.getInstance().save();
	}

	public static boolean isOnMonumenta() {
		boolean onMM = false;
		MinecraftClient mc = MinecraftClient.getInstance();
		String shard = Locations.getShard();

		if (!Objects.equals(shard, "unknown")) {
			onMM = true;
		}

		if (!onMM && mc.getCurrentServerEntry() != null) {
			onMM = !mc.isInSingleplayer() && mc.getCurrentServerEntry().address.toLowerCase().endsWith(".playmonumenta.com");
		}
		return onMM;
	}

	public static void saveConfig() {
		MinecraftClient.getInstance().execute(() -> {
			try {
				Config.writeJsonFile(options, OPTIONS_FILE_NAME);
			} catch (IOException ignore) {}
		});
	}

	public static boolean canInitializeDiscord() {
		Platform.Architecture currentArch = Platform.getArchitecture();
		return currentArch != Platform.Architecture.ARM64 && currentArch != Platform.Architecture.ARM32;
	}
}
