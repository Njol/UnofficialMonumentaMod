package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.hud.HealthBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class ModSpriteAtlasHolder extends SpriteAtlasHolder {

	// code to add sprites to an existing atlas via Fabric hacks:
	// ClientSpriteRegistryCallback.event(new Identifier("...")).register((atlasTexture, registry) -> registry.register(...));

	public static ModSpriteAtlasHolder HUD_ATLAS;

	private final List<Identifier> sprites;

	public ModSpriteAtlasHolder(TextureManager textureManager, String name, List<Identifier> sprites) {
		super(textureManager, new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/" + name + "/atlas.png"), name);
		this.sprites = sprites;
	}

	@Override
	protected Stream<Identifier> getSprites() {
		return sprites.stream();
	}

	@Override
	public Sprite getSprite(Identifier objectId) {
		return super.getSprite(objectId);
	}

	public static void registerSprites(MinecraftClient client) {
		List<Identifier> sprites = new ArrayList<>();
		Function<String, Identifier> register = s -> {
			Identifier id = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, s);
			sprites.add(id);
			return id;
		};
		HealthBar.registerSprites(register);
		HUD_ATLAS = new ModSpriteAtlasHolder(client.getTextureManager(), "hud", sprites);
		((ReloadableResourceManagerImpl) client.getResourceManager()).registerReloader(HUD_ATLAS);
	}

}
