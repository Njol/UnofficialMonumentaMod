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

	// TODO implement item cooldown display
	// requires sever-side adaptions to send the cooldown (on use and on connect)
	// biggest issue: most tesseracts are apparently done in mcfunctions
//	@Category("misc")
//	public boolean renderItemCooldowns = true;

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
