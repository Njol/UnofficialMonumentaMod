package ch.njol.unofficialmonumentamod.mc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.ShardData;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;

public class MonumentaModResourceReloader implements SynchronousResourceReloader {
	@Override
	public String getName() {
		return UnofficialMonumentaModClient.MOD_IDENTIFIER + " Resource Reloader";
	}

	@Override
	public void reload(ResourceManager manager) {
		UnofficialMonumentaModClient.LOGGER.info("Loading " + getName());
		UnofficialMonumentaModClient.locations.reload();
		UnofficialMonumentaModClient.spoofer.reload();
		ShardData.reload();
		UnofficialMonumentaModClient.LOGGER.info("Finished loading " + getName());
	}

}
