package ch.njol.unofficialmonumentamod.options;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options.Position;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.impl.ConfigEntryBuilderImpl;
import me.shedaniel.math.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
				Options.Category categoryAnnotation = field.getAnnotation(Options.Category.class);
				if (categoryAnnotation == null
					    || categoryAnnotation.value().startsWith("debug") && !UnofficialMonumentaModClient.options.debugOptionsEnabled) {
					continue;
				}
				ConfigCategory category = config.getOrCreateCategory(new TranslatableText("unofficial-monumenta-mod.config.category." + categoryAnnotation.value()));
				category.addEntry(buildConfigEntry(UnofficialMonumentaModClient.options, defaultOptions, field, "unofficial-monumenta-mod.config.option"));
			}

			config.setSavingRunnable(() -> {
				UnofficialMonumentaModClient.options.onUpdate();
				UnofficialMonumentaModClient.saveConfig();
			});
			if (client != null) {
				client.setScreen(config.build());
			}
		}

		@Override
		public void close() {
			if (client != null) {
				client.setScreen(parent);
			}
		}
	}

	private static AbstractConfigListEntry<?> buildConfigEntry(Object object, Object defaultObject, Field field, String parentTranslatePath) {
		try {
			String name = field.getName();
			Object value = field.get(object);
			Object defaultValue = field.get(defaultObject);
			Consumer<Object> saveConsumer = val -> {
				try {
					field.set(object, val);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			};
			String translateKey = parentTranslatePath + "." + name;
			TranslatableText text = new TranslatableText(translateKey);
			TranslatableText tooltip = new TranslatableText(translateKey + ".tooltip");
			// this code cannot be simplified because ClothConfig sucks
			if (field.getType() == Options.DescriptionLine.class) {
				return ConfigEntryBuilderImpl.create()
					.startTextDescription(text)
					.build();
			} else if (field.getType() == Boolean.TYPE) {
				return ConfigEntryBuilderImpl.create()
					.startBooleanToggle(text, (Boolean) value)
					.setDefaultValue((Boolean) defaultValue)
					.setTooltip(tooltip)
					.setSaveConsumer(saveConsumer::accept)
					.build();
			} else if (field.getType() == Integer.TYPE) {
				Options.IntSlider slider = field.getAnnotation(Options.IntSlider.class);
				if (field.getAnnotation(Options.Color.class) != null) {
					return ConfigEntryBuilderImpl.create()
						.startColorField(text, Color.ofOpaque((int) value))
						.setDefaultValue((int) defaultValue)
						.setTooltip(tooltip)
						.setSaveConsumer(saveConsumer::accept)
						.build();
				} else if (slider != null) {
					return ConfigEntryBuilderImpl.create()
						.startIntSlider(text, (Integer) value, slider.min(), slider.max())
						.setDefaultValue((Integer) defaultValue)
						.setTooltip(tooltip)
						.setSaveConsumer(saveConsumer::accept)
						.build();
				} else {
					return ConfigEntryBuilderImpl.create()
						.startIntField(text, (Integer) value)
						.setDefaultValue((Integer) defaultValue)
						.setTooltip(tooltip)
						.setSaveConsumer(saveConsumer::accept)
						.build();
				}
			} else if (field.getType() == Float.TYPE) {
				Options.FloatSlider slider = field.getAnnotation(Options.FloatSlider.class);
				if (slider != null) {
					float step = slider.step();
					return ConfigEntryBuilderImpl.create()
						.startLongSlider(text, Math.round((Float) value / step), Math.round(slider.min() / step), Math.round(slider.max() / step))
						.setDefaultValue(Math.round((Float) defaultValue / slider.step()))
						.setTextGetter(l -> new LiteralText(l + "%"))
						.setTooltip(tooltip)
						.setSaveConsumer(l -> saveConsumer.accept(l * step))
						.build();
				} else {
					return ConfigEntryBuilderImpl.create()
						.startFloatField(text, (Float) value)
						.setDefaultValue((Float) defaultValue)
						.setTooltip(tooltip)
						.setSaveConsumer(saveConsumer::accept)
						.build();
				}
			} else if (field.getType() == String.class) {
				return ConfigEntryBuilderImpl.create()
					.startTextField(text, (String) value)
					.setDefaultValue((String) defaultValue)
					.setTooltip(tooltip)
					.setSaveConsumer(saveConsumer::accept)
					.build();
			} else if (Enum.class.isAssignableFrom(field.getType())) {
				return ConfigEntryBuilderImpl.create()
					.startEnumSelector(text, (Class<Enum<?>>) field.getType(), (Enum<?>) value)
					.setDefaultValue((Enum<?>) defaultValue)
					.setTooltip(tooltip)
					.setSaveConsumer(saveConsumer::accept)
					.build();
			} else if (field.getType() == Position.class) {
				List<AbstractConfigListEntry> entries = new ArrayList<>();
				for (Field posField : Position.class.getDeclaredFields()) {
					entries.add(buildConfigEntry(value, defaultValue, posField, "unofficial-monumenta-mod.config.position"));
				}
				return ConfigEntryBuilderImpl.create()
					.startSubCategory(text, entries)
					.build();
			} else {
				throw new RuntimeException("Unexpected field type in " + object.getClass() + ": " + field);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
