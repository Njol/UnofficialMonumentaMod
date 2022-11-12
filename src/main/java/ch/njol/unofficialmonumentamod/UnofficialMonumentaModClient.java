package ch.njol.unofficialmonumentamod;

import ch.njol.minecraft.config.Config;
import ch.njol.minecraft.uiframework.hud.Hud;
import ch.njol.unofficialmonumentamod.hud.AbiltiesHud;
import ch.njol.unofficialmonumentamod.options.ConfigMenu;
import ch.njol.unofficialmonumentamod.options.Options;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	public static final String MOD_IDENTIFIER = "unofficial-monumenta-mod";

	public static final String OPTIONS_FILE_NAME = "unofficial-monumenta-mod.json";

	public static Options options = new Options();

	public static final AbilityHandler abilityHandler = new AbilityHandler();

	@Override
	public void onInitializeClient() {

		FabricModelPredicateProviderRegistry.register(new Identifier("on_head"),
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

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			abilityHandler.tick();
		});

		ClientPlayNetworking.registerGlobalReceiver(ChannelHandler.CHANNEL_ID, new ChannelHandler());

		Hud.INSTANCE.addElement(AbiltiesHud.INSTANCE);
		ConfigMenu.registerTypes();
	}

	public static void onDisconnect() {
		abilityHandler.onDisconnect();
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
