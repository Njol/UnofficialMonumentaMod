package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.hud.AbiltiesHud;
import ch.njol.unofficialmonumentamod.hud.BreathBar;
import ch.njol.unofficialmonumentamod.hud.HealthBar;
import ch.njol.unofficialmonumentamod.hud.HungerBar;
import ch.njol.unofficialmonumentamod.hud.MountHealthBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ModSpriteAtlasHolder extends SpriteAtlasHolder {

	// code to add sprites to an existing atlas via Fabric hacks:
	// ClientSpriteRegistryCallback.event(new Identifier("...")).register((atlasTexture, registry) -> registry.register(...));

	public static ModSpriteAtlasHolder HUD_ATLAS;
	public static ModSpriteAtlasHolder ABILITIES_ATLAS;

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
		ABILITIES_ATLAS = registerSprites(ABILITIES_ATLAS, "abilities", client, AbiltiesHud::registerSprites);
		HUD_ATLAS = registerSprites(HUD_ATLAS, "hud", client, register -> {
			HealthBar.registerSprites(register);
			HungerBar.registerSprites(register);
			BreathBar.registerSprites(register);
			MountHealthBar.registerSprites(register);
		});
	}

	public static ModSpriteAtlasHolder registerSprites(ModSpriteAtlasHolder existing, String identifier, MinecraftClient client, Consumer<Function<String, Identifier>> registerConsumer) {
		List<Identifier> sprites = new ArrayList<>();
		Function<String, Identifier> register = s -> {
			Identifier id = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, s);
			sprites.add(id);
			return id;
		};
		registerConsumer.accept(register);
		if (existing != null) {
			existing.sprites.clear();
			existing.sprites.addAll(sprites);
			return existing;
		} else {
			ModSpriteAtlasHolder atlas = new ModSpriteAtlasHolder(client.getTextureManager(), identifier, sprites);
			((ReloadableResourceManagerImpl) client.getResourceManager()).registerReloader(atlas);
			return atlas;
		}
	}

	public static NavigableMap<Integer, Identifier> findLevelledSprites(String atlas, String subDirectory, String fileNamePrefix, Function<String, Identifier> register) {
		NavigableMap<Integer, Identifier> map = new TreeMap<>();
		Pattern overlayPattern = Pattern.compile(Pattern.quote(fileNamePrefix) + "(\\d+).png$");
		Optional<Collection<Identifier>> foundIcons =
			MinecraftClient.getInstance().getResourceManager().streamResourcePacks()
				.map(rp -> rp.findResources(ResourceType.CLIENT_RESOURCES, UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/" + atlas + "/" + subDirectory, 1, path -> overlayPattern.matcher(path).find()))
				.filter(ids -> !ids.isEmpty())
				.reduce((a, b) -> b);
		if (foundIcons.isPresent()) {
			for (Identifier foundIcon : foundIcons.get()) {
				Matcher m = overlayPattern.matcher(foundIcon.getPath());
				if (!m.find()) {
					continue;
				}
				int level = Integer.parseInt(m.group(1));
				Identifier identifier = register.apply(foundIcon.getPath().substring("textures//".length() + atlas.length(), foundIcon.getPath().length() - ".png".length()));
				map.put(level, identifier);
			}
		}
		return map;
	}

}
