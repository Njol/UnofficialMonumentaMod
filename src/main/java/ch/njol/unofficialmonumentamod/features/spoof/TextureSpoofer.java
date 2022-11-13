package ch.njol.unofficialmonumentamod.features.spoof;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.item.ItemStackAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class TextureSpoofer {
	private static final String CACHE_PATH = "monumenta/texture-spoof.json";
	public final HashMap<String, SpoofItem> spoofedItems = new HashMap<>();
	//need to change nbt / item

	private static final Gson GSON = new GsonBuilder()
		                                 .setPrettyPrinting()
		                                 .excludeFieldsWithoutExposeAnnotation()
		                                 .create();

	public void onDisconnect() {
		save();
	}

	public ItemStack apply(ItemStack stack) {
		if (!UnofficialMonumentaModClient.options.enableTextureSpoofing) {
			return stack;
		}
		String key = stack.getName().getString().toLowerCase();
		//probably, just probably should make it, so it's just the render and not the actual item that's edited, because it being able to be placed, does do stupid things.
		if (spoofedItems.containsKey(key)) {
			SpoofItem item = spoofedItems.get(key);
			if (stack.hasNbt() && hasBeenEdited(stack)) {
				return stack;
			}
			if (item == null) {
				//if I do dumb shit again, this should stop it from crashing
				return stack;
			}
			ItemStack newItemStack = stack.copy();

			((ItemStackAccessor) (Object) newItemStack).setItem(Registry.ITEM.get(item.getItemIdentifier()));

			if (newItemStack.hasNbt()) {
				assert newItemStack.getNbt() != null;
				if (item.displayName != null) {
					newItemStack.getNbt().put("plain", setPlain(newItemStack.getNbt(), item.displayName));
				}
				//to be able to detect already edited stacks
				NbtCompound monumentamodCompound = new NbtCompound();
				monumentamodCompound.put("edited", NbtInt.of(1));
				newItemStack.getNbt().put("monumenta-mod", monumentamodCompound);
			}

			return newItemStack;
		}
		return stack;
	}

	private final TypeToken<HashMap<String, SpoofItem>> typeToken = new TypeToken<>() {
	};

	private static NbtCompound setPlain(NbtCompound stackData, String displayName) {
		NbtCompound plain = getOrCreateCompound(stackData, "plain");
		NbtCompound display = getOrCreateCompound(plain, "display");
		display.putString("Name", displayName);
		plain.put("display", display);
		return plain;
	}

	public static String getPlainDisplayName(NbtCompound stackData) {
		try {
			if (stackData.contains("plain")) {
				if (stackData.getCompound("plain").contains("display")) {
					return stackData.getCompound("plain").getCompound("display").getString("Name");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static NbtCompound getOrCreateCompound(NbtCompound compound, String key) {
		if (compound.getCompound(key) == null) {
			return new NbtCompound();
		} else {
			return compound.getCompound(key);
		}
	}

	public static boolean hasBeenEdited(ItemStack stack) {
		if (stack.hasNbt()) {
			assert stack.getNbt() != null;
			if (stack.getNbt().contains("monumenta-mod", 10)) {
				return stack.getNbt().getCompound("monumenta-mod").getInt("edited") == 1;
			}
		}
		return false;
	}

	public static boolean wouldveBeenEdited(ItemStack stack) {
		String key = stack.getName().getString().toLowerCase();
		return UnofficialMonumentaModClient.spoofer.spoofedItems.containsKey(key);
	}

	public void reload() {
		File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_PATH).toFile();
		if (!file.exists()) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			HashMap<String, SpoofItem> loadedItems = GSON.fromJson(reader, typeToken.getType());
			if (loadedItems != null) {
				spoofedItems.clear();
				spoofedItems.putAll(loadedItems);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void save() {
		File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_PATH).toFile();

		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GSON.toJson(spoofedItems));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class SpoofItem {
		@Expose
		public String item;
		@Expose
		public String displayName;
		@Expose
		public boolean override;

		public SpoofItem(Item item, String displayName) {
			this.item = Registry.ITEM.getId(item).toString();
			this.displayName = displayName;
		}

		public Identifier getItemIdentifier() {
			return new Identifier(item);
		}

		@Override
		public String toString() {
			return item + "-" + displayName;
		}
	}
}
