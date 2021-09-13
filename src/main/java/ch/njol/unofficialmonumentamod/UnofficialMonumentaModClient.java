package ch.njol.unofficialmonumentamod;

import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	public static Options options = new Options();

	@Override
	public void onInitializeClient() {

		FabricModelPredicateProviderRegistry.register(new Identifier("on_head"),
				(itemStack, clientWorld, livingEntity) -> livingEntity != null && itemStack == livingEntity.getEquippedStack(EquipmentSlot.HEAD) ? 1 : 0);

		try (FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve("./unofficial-monumenta-mod.json").toFile())) {
			options = new GsonBuilder().create().fromJson(reader, Options.class);
		} catch (FileNotFoundException e) {
			// Config file doesn't exist, so use default config (and write config file).
			saveConfig();
		} catch (IOException e) {
			// Any issue with the config file silently reverts to the default config
			e.printStackTrace();
		}

	}

	public static void saveConfig() {
		try (FileWriter writer = new FileWriter((FabricLoader.getInstance().getConfigDir().resolve("./unofficial-monumenta-mod.json").toFile()))) {
			writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(UnofficialMonumentaModClient.options));
		} catch (IOException e) {
			// Silently ignore save errors
			e.printStackTrace();
		}
	}

}
