package ch.njol.unofficialmonumentamod;

import ch.njol.minecraft.config.Config;
import ch.njol.minecraft.uiframework.hud.Hud;
import ch.njol.unofficialmonumentamod.features.calculator.Calculator;
import ch.njol.unofficialmonumentamod.features.discordrpc.DiscordRPC;
import ch.njol.unofficialmonumentamod.features.effect.EffectOverlay;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.features.misc.managers.Notifier;
import ch.njol.unofficialmonumentamod.features.misc.notifications.LocationNotifier;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import ch.njol.unofficialmonumentamod.features.strike.ChestCountOverlay;
import ch.njol.unofficialmonumentamod.hud.AbiltiesHud;
import ch.njol.unofficialmonumentamod.options.ConfigMenu;
import ch.njol.unofficialmonumentamod.options.Options;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	public static final String MOD_IDENTIFIER = "unofficial-monumenta-mod";

	public static final String OPTIONS_FILE_NAME = "unofficial-monumenta-mod.json";

	public static final Logger LOGGER = LogManager.getLogger(MOD_IDENTIFIER);

	public static Options options = new Options();

	public static Locations locations = new Locations();
	public static TextureSpoofer spoofer = new TextureSpoofer();

	public static DiscordRPC discordRPC = new DiscordRPC();

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
			e.printStackTrace();
		}

		if (options.discordEnabled) {
			discordRPC.Init();
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			abilityHandler.tick();
			effectOverlay.tick();
			Calculator.tick();
		});

		ClientTickEvents.END_WORLD_TICK.register(world -> {
			Notifier.tick();
		});

		ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
			ChestCountOverlay.INSTANCE.onWorldLoad();
		}));

		ClientPlayNetworking.registerGlobalReceiver(ChannelHandler.CHANNEL_ID, new ChannelHandler());

		KeyBindingHelper.registerKeyBinding(toggleCalculatorKeyBinding);

		Hud.INSTANCE.addElement(AbiltiesHud.INSTANCE);
		Hud.INSTANCE.addElement(ChestCountOverlay.INSTANCE);
		Hud.INSTANCE.addElement(effectOverlay);
		ConfigMenu.registerTypes();
	}

	public static void onDisconnect() {
		abilityHandler.onDisconnect();
		Notifier.onDisconnect();
		LocationNotifier.onDisconnect();
		spoofer.onDisconnect();
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
			} catch (IOException ex) {
				// ignore
			}
		});
	}

}
