package ch.njol.unofficialmonumentamod.options;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class ConfigMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		try {
			Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
		} catch (ClassNotFoundException e) {
			return parent -> null;
		}
		return ConfigScreen::new;
	}

	// this fake screen is a workaround for a bug when using both modmenu and cloth-config (cloth config expects a new screen each time, but modmenu caches the screen)
	private static class ConfigScreen extends Screen {

		private final Screen parent;

		protected ConfigScreen(Screen parent) {
			super(new TranslatableText("unofficial-monumenta-mod.config.title"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			super.init();
			Options defaultOptions = new Options();
			ConfigBuilder config = ConfigBuilder.create()
					.setParentScreen(parent)
					.setTitle(new TranslatableText("unofficial-monumenta-mod.config.title"));

			for (Field field : Options.class.getDeclaredFields()) {
				String name = field.getName();
				try {
					Object value = field.get(UnofficialMonumentaModClient.options);
					Object defaultValue = field.get(defaultOptions);
					Consumer<Object> saveConsumer = val -> {
						try {
							field.set(UnofficialMonumentaModClient.options, val);
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					};
					String translateKey = "unofficial-monumenta-mod.config.option." + name;
					Options.Category categoryAnnotation = field.getAnnotation(Options.Category.class);
					if (categoryAnnotation != null && categoryAnnotation.value().startsWith("debug") && !UnofficialMonumentaModClient.options.debugOptionsEnabled)
						continue;
					ConfigCategory category = config.getOrCreateCategory(new TranslatableText("unofficial-monumenta-mod.config.category." + (categoryAnnotation == null ? "misc" : categoryAnnotation.value())));
					// this code cannot be simplified because ClothConfig sucks
					if (field.getType() == Boolean.TYPE) {
						category.addEntry(config.entryBuilder()
								.startBooleanToggle(new TranslatableText(translateKey), (Boolean) value)
								.setDefaultValue((Boolean) defaultValue)
								.setTooltip(new TranslatableText(translateKey + ".tooltip"))
								.setSaveConsumer(saveConsumer::accept)
								.build());
					} else if (field.getType() == Integer.TYPE) {
						category.addEntry(config.entryBuilder()
								.startIntField(new TranslatableText(translateKey), (Integer) value)
								.setDefaultValue((Integer) defaultValue)
								.setTooltip(new TranslatableText(translateKey + ".tooltip"))
								.setSaveConsumer(saveConsumer::accept)
								.build());
					} else if (field.getType() == Float.TYPE) {
						category.addEntry(config.entryBuilder()
								.startFloatField(new TranslatableText(translateKey), (Float) value)
								.setDefaultValue((Float) defaultValue)
								.setTooltip(new TranslatableText(translateKey + ".tooltip"))
								.setSaveConsumer(saveConsumer::accept)
								.build());
					} else if (field.getType() == String.class) {
						category.addEntry(config.entryBuilder()
								.startTextField(new TranslatableText(translateKey), (String) value)
								.setDefaultValue((String) defaultValue)
								.setTooltip(new TranslatableText(translateKey + ".tooltip"))
								.setSaveConsumer(saveConsumer::accept)
								.build());
					} else if (Enum.class.isAssignableFrom(field.getType())) {
						category.addEntry(config.entryBuilder()
								.startEnumSelector(new TranslatableText(translateKey), (Class<Enum<?>>) field.getType(), (Enum<?>) value)
								.setDefaultValue((Enum<?>) defaultValue)
								.setTooltip(new TranslatableText(translateKey + ".tooltip"))
								.setSaveConsumer(saveConsumer::accept)
								.build());
					} else {
						throw new RuntimeException("Unexpected field in Options: " + field);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			config.setSavingRunnable(() -> {
				UnofficialMonumentaModClient.options.onUpdate();
				UnofficialMonumentaModClient.saveConfig();
			});
			if (client != null)
				client.openScreen(config.build());
		}

		@Override
		public void onClose() {
			if (client != null)
				client.openScreen(parent);
		}
	}

}
