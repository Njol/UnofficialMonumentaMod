package ch.njol.unofficialmonumentamod.options;

import ch.njol.unofficialmonumentamod.AbilityOptionPreset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class Options {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Category {
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Color {
	}

	// apparently there's no slider for floats....
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Slider {
		float min();

		float max();

		float step();

		String unit();
	}

	public interface DescriptionLine {
	}

	@Category("misc")
	public boolean overrideTridentRendering = true;
	@Category("misc")
	public boolean lowerVillagerHelmets = true;

	@Category("misc")
	public boolean firmamentPingFix = true;

	@Category("misc")
	public boolean chestsortDisabledForInventory = false;
	@Category("misc")
	public boolean chestsortDisabledForEnderchest = false;
	@Category("misc")
	public boolean chestsortDisabledEverywhereElse = false;

	@Category("misc")
	public boolean crossbowFix = true;

	@Category("misc")
	public boolean locationUpdate = true;
	@Category("misc")
	public boolean notifyLocation = true;
	@Category("misc")
	@Slider(min = 1.5F, max = 60F, step = 0.1F, unit = "second")
	public float notifierShowTime = 5F;

	@Category("misc")
	public boolean showCalculatorInPlots = true;

	// TOD implement item cooldown display
	// requires sever-side adaptions to send the cooldown (on use and on connect)
	// the biggest issue: most tesseracts are apparently done in mcfunctions
	@Category("misc")
	public boolean renderItemCooldowns = true;

	/*
	 * Location related settings
	 */

	@Category("abilities")
	public transient DescriptionLine abilitiesDisplay_info;
	@Category("abilities")
	public boolean abilitiesDisplay_enabled = true;
	@Category("abilities")
	public boolean abilitiesDisplay_showCooldownAsText = true;
	@Category("abilities")
	public boolean abilitiesDisplay_hideAbilityRelatedMessages = true;
	@Category("abilities")
	public boolean abilitiesDisplay_inFrontOfChat = false;
	@Category("abilities")
	public boolean abilitiesDisplay_tooltips = true;

	@Category("abilities")
	public transient DescriptionLine abilitiesDisplay_positionInfo;
	@Category("abilities")
	public transient AbilityOptionPreset abilitiesDisplay_preset = AbilityOptionPreset.CUSTOM;
	@Category("abilities")
	public boolean abilitiesDisplay_horizontal = AbilityOptionPreset.ABOVE_HOTBAR.horizontal;
	@Category("abilities")
	public float abilitiesDisplay_align = AbilityOptionPreset.ABOVE_HOTBAR.align;
	@Category("abilities")
	public float abilitiesDisplay_offsetXRelative = AbilityOptionPreset.ABOVE_HOTBAR.offsetXRelative;
	@Category("abilities")
	public float abilitiesDisplay_offsetYRelative = AbilityOptionPreset.ABOVE_HOTBAR.offsetYRelative;
	@Category("abilities")
	public int abilitiesDisplay_offsetXAbsolute = AbilityOptionPreset.ABOVE_HOTBAR.offsetXAbsolute;
	@Category("abilities")
	public int abilitiesDisplay_offsetYAbsolute = AbilityOptionPreset.ABOVE_HOTBAR.offsetYAbsolute;

	@Category("abilities")
	public transient DescriptionLine abilitiesDisplay_miscInfo;
	@Category("abilities")
	public boolean abilitiesDisplay_offCooldownResize = true;
	@Category("abilities")
	@Slider(min = 0, max = 1, step = 0.01f, unit = "%")
	public float abilitiesDisplay_offCooldownFlashIntensity = 1;
	@Category("abilities")
	@Slider(min = 0, max = 1, step = 0.01f, unit = "%")
	public float abilitiesDisplay_offCooldownSoundVolume = 0f;
	@Category("abilities")
	@Slider(min = 0, max = 2, step = 0.05f, unit = "")
	public float abilitiesDisplay_offCooldownSoundPitchMin = 1f;
	@Category("abilities")
	@Slider(min = 0, max = 2, step = 0.05f, unit = "")
	public float abilitiesDisplay_offCooldownSoundPitchMax = 1f;
	@Category("abilities")
	public int abilitiesDisplay_iconSize = 32;
	@Category("abilities")
	public int abilitiesDisplay_iconGap = 0;
	@Category("abilities")
	@Color
	public int abilitiesDisplay_textColorRaw = 0xffeeeeee;
	@Category("abilities")
	public int abilitiesDisplay_textOffset = 4;
	@Category("abilities")
	public boolean abilitiesDisplay_ascendingRenderOrder = false;
	@Category("abilities")
	public boolean abilitiesDisplay_showOnlyOnCooldown = false;
	@Category("abilities")
	public boolean abilitiesDisplay_alwaysShowAbilitiesWithCharges = false;
	@Category("abilities")
	public boolean abilitiesDisplay_condenseOnlyOnCooldown = false;
	@Category("abilities")
	public boolean abilitiesDisplay_showPassiveAbilities = false;

	/**
	 * List of [class]/[ability]. Abilities not present in this list are sorted alphabetically.
	 */
	public List<String> abilitiesDisplay_order = new ArrayList<>();

	@Category("debug")
	public boolean debugOptionsEnabled = false;
	@Category("debug")
	public boolean logPackets = false;

	/*
	 * Discord RPC Configuration
	 */
	@Category("discord")
	public boolean discordEnabled = true;
	@Category("discord")
	public String discordDetails = "{player} is on {shard}";
	/*
	 * discordDetails replace values:
	 *
	 * {player} returns the player's name
	 * {shard} returns the shard name
	 * {server} returns the server name
	 * {holding} returns the item held in the main hand
	 * {class} returns the class the user is playing as
	 * {location} returns the location or if not found the shard name
	 * everything else is a string literal
	 */

	public void onUpdate() {
		if (abilitiesDisplay_preset != AbilityOptionPreset.CUSTOM) {
			abilitiesDisplay_horizontal = abilitiesDisplay_preset.horizontal;
			abilitiesDisplay_align = abilitiesDisplay_preset.align;
			abilitiesDisplay_offsetXRelative = abilitiesDisplay_preset.offsetXRelative;
			abilitiesDisplay_offsetYRelative = abilitiesDisplay_preset.offsetYRelative;
			abilitiesDisplay_offsetXAbsolute = abilitiesDisplay_preset.offsetXAbsolute;
			abilitiesDisplay_offsetYAbsolute = abilitiesDisplay_preset.offsetYAbsolute;
			abilitiesDisplay_preset = AbilityOptionPreset.CUSTOM;
		}
	}

}
