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
import java.util.UUID;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class TextureSpoofer {
	private static final String CACHE_PATH = "monumenta/texture-spoof.json";
	public final HashMap<String, SpoofItem> spoofedItems = new HashMap<>();

	private static final Gson GSON = new GsonBuilder()
		                                 .setPrettyPrinting()
		                                 .excludeFieldsWithoutExposeAnnotation()
		                                 .create();

	public void onDisconnect() {
		save();
	}
	
	public static String getKeyOf(ItemStack stack) {
		return stack.getName().getString().toLowerCase();
	}
	
	public Item getSpoofItem(ItemStack stack) {
		if (!UnofficialMonumentaModClient.options.enableTextureSpoofing) {
			return stack.getItem();
		}
		
		String key = getKeyOf(stack);
		if (spoofedItems.containsKey(key)) {
			SpoofItem sI = spoofedItems.get(key);
			
			return Registries.ITEM.get(sI.getItemIdentifier());
		}
		
		return stack.getItem();
	}

	public ItemStack applyOnCopy(ItemStack stack) {
		if (!UnofficialMonumentaModClient.options.enableTextureSpoofing) {
			return stack;
		}
		String key = getKeyOf(stack);
		if (spoofedItems.containsKey(key)) {
			SpoofItem item = spoofedItems.get(key);
			if (stack.hasNbt() && hasBeenEdited(stack)) {
				return stack;
			}
			if (item == null || item.invalid) {
				return stack;
			}
			
			ItemStack newItemStack = stack.copy();
			
			Item overrideItem = Registries.ITEM.get(item.getItemIdentifier());
			if (!stack.getItem().equals(overrideItem)) {
				((ItemStackAccessor) (Object) newItemStack).setItem(overrideItem);
			}
			
			if (stack.hasNbt()) {
				assert newItemStack.getNbt() != null;
				if (item.displayName != null) {
					newItemStack.getNbt().put("plain", setPlain(newItemStack.getNbt(), item.displayName));
				}
				
				if (item.hope != null) {
					try {
						//check if it's a valid uuid
						UUID uuid = UUID.fromString(item.hope);
						newItemStack.getNbt().put("Monumenta", setHoped(newItemStack.getNbt(), item.hope));
					} catch (IllegalArgumentException e) {
						UnofficialMonumentaModClient.LOGGER.error("invalid Hope skin uuid, removing entry.", e);
						SpoofItem newSpoof = spoofedItems.get(key);
						newSpoof.invalid = true;
						spoofedItems.replace(key, newSpoof);
						return stack;
					}
					
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
	
	public ItemStack apply(ItemStack stack) {
		//avoid unforeseen use actions if the stack's item type is different from what it actually is (would look kinda dumb if I could place sword ._.)
		if (stack.getItem() == this.getSpoofItem(stack)) {
			applyOnOriginal(stack);
			return null;
		} else {
			return applyOnCopy(stack);
		}
	}
	
	public void applyOnOriginal(ItemStack stack) {
		if (!UnofficialMonumentaModClient.options.enableTextureSpoofing) {
			return;
		}
		String key = getKeyOf(stack);
		if (spoofedItems.containsKey(key)) {
			SpoofItem item = spoofedItems.get(key);
			if (stack.hasNbt() && hasBeenEdited(stack)) {
				return;
			}
			if (item == null || item.invalid) {
				return;
			}
			
			Item overrideItem = Registries.ITEM.get(item.getItemIdentifier());
			if (!stack.getItem().equals(overrideItem)) {
				((ItemStackAccessor) (Object) stack).setItem(overrideItem);
			}

			if (stack.hasNbt()) {
				assert stack.getNbt() != null;
				if (item.displayName != null) {
					stack.getNbt().put("plain", setPlain(stack.getNbt(), item.displayName));
				}
				
				if (item.hope != null) {
					try {
						//check if it's a valid uuid
						UUID uuid = UUID.fromString(item.hope);
						stack.getNbt().put("Monumenta", setHoped(stack.getNbt(), item.hope));
					} catch (IllegalArgumentException e) {
						UnofficialMonumentaModClient.LOGGER.error("invalid Hope skin uuid, removing entry.", e);
						SpoofItem newSpoof = spoofedItems.get(key);
						newSpoof.invalid = true;
						spoofedItems.replace(key, newSpoof);
						return;
					}
				
				}
				//to be able to detect already edited stacks
				NbtCompound monumentamodCompound = new NbtCompound();
				monumentamodCompound.put("edited", NbtInt.of(1));
				stack.getNbt().put("monumenta-mod", monumentamodCompound);
			}
		}
	}

	private final TypeToken<HashMap<String, SpoofItem>> typeToken = new TypeToken<>() {
	};
	
	private static NbtCompound setHoped(NbtCompound stackData, String uuid) {
		NbtCompound monumenta = getOrCreateCompound(stackData, "Monumenta");
		NbtCompound playerModified = getOrCreateCompound(monumenta, "PlayerModified");
		NbtCompound infusions = getOrCreateCompound(playerModified, "Infusions");
		NbtCompound hope = getOrCreateCompound(infusions, "Hope");
		
		hope.putString("Infuser", uuid);
		
		infusions.put("Hope", hope);
		playerModified.put("Infusions", infusions);
		monumenta.put("PlayerModified", playerModified);
		return monumenta;
	}

	public static NbtCompound setPlain(NbtCompound stackData, String displayName) {
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
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to obtain the display name of an item", e);
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
		String key = getKeyOf(stack);
		return UnofficialMonumentaModClient.spoofer.spoofedItems.containsKey(key) &&
				!UnofficialMonumentaModClient.spoofer.spoofedItems.get(key).invalid;
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
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to reload texture spoofing data", e);
		}
	}

	public void save() {
		File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_PATH).toFile();

		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to create files for texture spoofing", e);
			}
		}

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GSON.toJson(spoofedItems));
		} catch (Exception e) {
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to save texture spoofing data", e);
		}
	}

	public static class SpoofItem {
		@Expose
		public String item;
		@Expose
		public String displayName;
		@Expose
		public String hope;
		@Expose
		public boolean override = false;
		
		protected boolean invalid;

		public SpoofItem(Item item, String displayName) {
			this.item = Registries.ITEM.getId(item).toString();
			this.displayName = displayName;
		}

		public Identifier getItemIdentifier() {
			return new Identifier(item);
		}

		@Override
		public String toString() {
			return item + "-" + displayName + (hope != null ? "-(hoped by "+ hope +")" : "");
		}
	}
}
