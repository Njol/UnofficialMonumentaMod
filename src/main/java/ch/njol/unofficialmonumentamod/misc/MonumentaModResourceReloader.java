package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;

public class MonumentaModResourceReloader implements SynchronousResourceReloader {
    @Override
    public String getName() {
        return UnofficialMonumentaModClient.MOD_IDENTIFIER + " Resource Reloader";
    }

    @Override
    public void reload(ResourceManager manager) {
        UnofficialMonumentaModClient.locations.load();
    }
}
