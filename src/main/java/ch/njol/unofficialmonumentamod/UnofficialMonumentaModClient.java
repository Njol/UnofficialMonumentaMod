package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.features.discordrpc.DiscordRPC;
import ch.njol.unofficialmonumentamod.features.effect.EffectMoveScreen;
import ch.njol.unofficialmonumentamod.features.effect.EffectOverlay;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.options.Options;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	// TODO:
	// sage's insight has no ClassAbility, but has stacks
	// spellshock however has a ClassAbility, but doesn't really need to be displayed...

	public static final String MOD_IDENTIFIER = "unofficial-monumenta-mod";

	public static final String OPTIONS_FILE_NAME = "unofficial-monumenta-mod.json";

	public static Options options = new Options();

	public final static Logger LOGGER = LogManager.getLogger(MOD_IDENTIFIER);

	public static Locations locations = new Locations();

	public static DiscordRPC discordRPC = new DiscordRPC();

	public static EffectOverlay eOverlay = new EffectOverlay();

	public static final AbilityHandler abilityHandler = new AbilityHandler();

	// This is a hacky way to pass data around...
	public static boolean isReorderingAbilities = false;

	@Override
	public void onInitializeClient() {

		FabricModelPredicateProviderRegistry.register(new Identifier("on_head"),
			(itemStack, clientWorld, livingEntity, seed) -> livingEntity != null && itemStack == livingEntity.getEquippedStack(EquipmentSlot.HEAD) ? 1 : 0);

		try {
			options = readJsonFile(Options.class, OPTIONS_FILE_NAME);
		} catch (FileNotFoundException e) {
			// Config file doesn't exist, so use default config (and write config file).
			writeJsonFile(options, OPTIONS_FILE_NAME);
		} catch (IOException | JsonParseException e) {
			// Any issue with the config file silently reverts to the default config
			e.printStackTrace();
		}

		if (options.discordEnabled) discordRPC.Init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			abilityHandler.tick();
			eOverlay.tick();
		});

		ClientPlayNetworking.registerGlobalReceiver(ChannelHandler.CHANNEL_ID, new ChannelHandler());
		ClientCommandManager.DISPATCHER.register(
				ClientCommandManager.literal("UMM").then(ClientCommandManager.literal("moveScreen").executes((context) -> {
					MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new EffectMoveScreen()));
					return 1;
				}))
		);
	}

	public static void onDisconnect() {
		abilityHandler.onDisconnect();
		locations.onDisconnect();
	}

	public static boolean isOnMonumenta() {
		boolean onMM = false;
		MinecraftClient mc = MinecraftClient.getInstance();
		String shard = Locations.getShard();

		if (!Objects.equals(shard, "unknown")) onMM = true;

		if (!onMM && mc.getCurrentServerEntry() != null) {
			onMM = !mc.isInSingleplayer() && mc.getCurrentServerEntry().address.toLowerCase().endsWith(".playmonumenta.com");
		}

		return onMM;
	}

	private static <T> T readJsonFile(Class<T> c, String filePath) throws IOException, JsonParseException {
		try (FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile())) {
			return new GsonBuilder().create().fromJson(reader, c);
		}
	}

	private static void writeJsonFile(Object o, String filePath) {
		try (FileWriter writer = new FileWriter((FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile()))) {
			writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(o));
		} catch (IOException e) {
			// Silently ignore save errors
			e.printStackTrace();
		}
	}

	public static void saveConfig() {
		MinecraftClient.getInstance().execute(() -> {
			writeJsonFile(options, OPTIONS_FILE_NAME);
		});
	}

	public static boolean isAbilityVisible(AbilityHandler.AbilityInfo abilityInfo, boolean forSpaceCalculation) {
		// Passive abilities are visible iff passives are enabled in the options
		if (abilityInfo.initialCooldown == 0 && abilityInfo.maxCharges == 0) {
			return options.abilitiesDisplay_showPassiveAbilities;
		}

		// Active abilities take up space even if hidden unless condenseOnlyOnCooldown is enabled
		if (forSpaceCalculation && !UnofficialMonumentaModClient.options.abilitiesDisplay_condenseOnlyOnCooldown) {
			return true;
		}

		// Active abilities are visible with showOnlyOnCooldown iff they are on cooldown or don't have a cooldown (and should have stacks instead)
		return !options.abilitiesDisplay_showOnlyOnCooldown
			       || isReorderingAbilities
			       || abilityInfo.remainingCooldown > 0
			       || abilityInfo.maxCharges > 0 && (abilityInfo.initialCooldown <= 0 || options.abilitiesDisplay_alwaysShowAbilitiesWithCharges);
	}

}
