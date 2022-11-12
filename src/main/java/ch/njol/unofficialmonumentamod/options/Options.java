package ch.njol.unofficialmonumentamod.options;

import ch.njol.minecraft.config.annotations.Category;
import ch.njol.minecraft.config.annotations.Color;
import ch.njol.minecraft.config.annotations.DescriptionLine;
import ch.njol.minecraft.config.annotations.FloatSlider;
import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.unofficialmonumentamod.AbilityOptionPreset;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import java.util.ArrayList;
import java.util.List;

public class Options implements ch.njol.minecraft.config.Options {

	@Category("misc")
	public boolean overrideTridentRendering = true;
	@Category("misc")
	public boolean lowerVillagerHelmets = false;

	@Category("misc")
	public boolean firmamentPingFix = true;

	@Category("misc")
	public boolean chestsortDisabledForInventory = false;
	@Category("misc")
	public boolean chestsortDisabledForEnderchest = false;
	@Category("misc")
	public boolean chestsortDisabledEverywhereElse = false;

	@Category("misc")
	public boolean notifyLocation = true;
	@Category("misc")
	@Slider(min = 1.5F, max = 60F, step = 0.1F, unit = "second")
	public float notifierShowTime = 5F;

	// TODO implement item cooldown display
	// requires sever-side adaptions to send the cooldown (on use and on connect)

	// the biggest issue: most tesseracts are apparently done in mcfunctions -> and one of them for some reason doesn't handle its charges the same way as the others.
	// @Category("misc")
	// public boolean renderItemCooldowns = true;
	/*
	 * Location related settings
	 */

	@Category("misc")
	public boolean silenceTeamErrors = true;

	@Category("misc")
	public boolean showCalculator = true;

	@Category("misc")
	public boolean enableTextureSpoofing = true;

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
	public ElementPosition abilitiesDisplay_position = AbilityOptionPreset.ABOVE_HOTBAR.position.clone();

	@Category("abilities")
	public transient DescriptionLine abilitiesDisplay_miscInfo;
	@Category("abilities")
	public boolean abilitiesDisplay_offCooldownResize = true;
	@Category("abilities")
	@FloatSlider(min = 0, max = 1, step = 0.01f, unit = "%", unitStep = 100)
	public float abilitiesDisplay_offCooldownFlashIntensity = 1;
	@Category("abilities")
	@FloatSlider(min = 0, max = 1, step = 0.01f, unit = "%", unitStep = 100)
	public float abilitiesDisplay_offCooldownSoundVolume = 0f;
	@Category("abilities")
	@FloatSlider(min = 0, max = 2, step = 0.05f)
	public float abilitiesDisplay_offCooldownSoundPitchMin = 1f;
	@Category("abilities")
	@FloatSlider(min = 0, max = 2, step = 0.05f)
	public float abilitiesDisplay_offCooldownSoundPitchMax = 1f;
	@Category("abilities")
	public boolean abilitiesDisplay_offCooldownSoundUseAlt = false;
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
	 * List of [class]/[ability]
	 */
	public List<String> abilitiesDisplay_order = new ArrayList<>();

	@Category("debug")
	public boolean debugOptionsEnabled = false;
	@Category("debug")
	public boolean logPackets = false;
	@Category("debug")
	public boolean showShardOverridingChanges = false;

	/**
	 * Discord RPC Configuration
	 */
	@Category("discord")
	public boolean discordEnabled = true;
	@Category("discord")
	public String discordDetails = "{player} is on {shard}";

	/**
	 * discordDetails replace values:
	 * {player} returns the player's name
	 * {shard} returns the shard name
	 * {holding} returns the item held in the main hand
	 * {class} returns the class the user is playing as
	 * {location} returns the location or if not found the shard name
	 * everything else is a string literal
	 */

	@Category("effectOverlay")
	public transient DescriptionLine effectOverlay_info;
	@Category("effectOverlay")
	public float effect_offsetXRelative = 0.5f;
	@Category("effectOverlay")
	public float effect_offsetYRelative = 0.0f;
	@Category("effectOverlay")
	public int effect_offsetXAbsolute = 0;
	@Category("effectOverlay")
	public int effect_offsetYAbsolute = 0;

	@Category("effectOverlay")
	public transient DescriptionLine effect_format;
	@Category("effectOverlay")
	public boolean effect_compress = true;
	@Category("effectOverlay")
	public boolean effect_active = true;

	@Category("chestCountOverlay")
	public transient DescriptionLine chestCountOverlay_pos;
	@Category("chestCountOverlay")
	public float chestCount_offsetXRelative = 1f;
	@Category("chestCountOverlay")
	public float chestCount_offsetYRelative = 0.0f;
	@Category("chestCountOverlay")
	public int chestCount_offsetXAbsolute = -64;
	@Category("chestCountOverlay")
	public int chestCount_offsetYAbsolute = 0;

	@Category("chestCountOverlay")
	public boolean chestCount_active = true;

	public void onUpdate() {
		if (abilitiesDisplay_preset != AbilityOptionPreset.CUSTOM) {
			abilitiesDisplay_horizontal = abilitiesDisplay_preset.horizontal;
			abilitiesDisplay_position = abilitiesDisplay_preset.position.clone();
			abilitiesDisplay_preset = AbilityOptionPreset.CUSTOM;
		}
		UnofficialMonumentaModClient.saveConfig();
	}

	public boolean categoryVisible(String category) {
		return debugOptionsEnabled || !category.equals("debug");
	}

}
