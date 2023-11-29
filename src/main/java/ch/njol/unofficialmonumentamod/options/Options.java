package ch.njol.unofficialmonumentamod.options;

import ch.njol.minecraft.config.annotations.Category;
import ch.njol.minecraft.config.annotations.Color;
import ch.njol.minecraft.config.annotations.DescriptionLine;
import ch.njol.minecraft.config.annotations.Dropdown;
import ch.njol.minecraft.config.annotations.FloatSlider;
import ch.njol.minecraft.config.annotations.IntSlider;
import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.unofficialmonumentamod.AbilityHandler;
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
	public boolean hideVillagerPlayerHeads = false;

	@Category("misc")
	public boolean firmamentPingFix = true;

	@Dropdown("chestsort")
	@Category("misc")
	public boolean chestsortDisabledForInventory = false;
	@Dropdown("chestsort")
	@Category("misc")
	public boolean chestsortDisabledForEnderchest = false;
	@Dropdown("chestsort")
	@Category("misc")
	public boolean chestsortDisabledEverywhereElse = false;

	@Category("misc")
	public boolean enableDelveRecognition = true;

	@Dropdown("location")
	@Category("misc")
	public boolean notifyLocation = false;

	@Dropdown("notifier")
	@Category("misc")
	@FloatSlider(min = 1.5F, max = 20F, step = 0.1F, unit = " seconds")
	public float notifierShowTime = 10F;
	@Dropdown("notifier")
	@Category("misc")
	public boolean notifierEarlyDismiss = false;

	@Dropdown("notifier")
	@Category("misc")
	@FloatSlider(min = 0.5F, max = 3F, step = 0.5F, unit = "x bigger")
	public float notifierScaleFactor = 1.5F;
	@Dropdown("notifier")
	@Category("misc")
	public ElementPosition notifierPosition = new ElementPosition(1.0F, -10, 1.0F, -10, 1.0F, 1.0F);


	// TODO implement item cooldown display
	// requires sever-side adaptions to send the cooldown (on use and on connect)

	// the biggest issue: most tesseracts are apparently done in mcfunctions -> and one of them for some reason doesn't handle its charges the same way as the others.
	// @Category("misc")
	// public boolean renderItemCooldowns = true;

	@Category("misc")
	public boolean silenceTeamErrors = true;

	@Dropdown("calculator")
	@Category("misc")
	public boolean showCalculator = true;
	@Dropdown("calculator")
	@Category("misc")
	public boolean enableKeybindOutsidePlots = false;
	@Dropdown("calculator")
	@Category("misc")
	public boolean calculatorPersistOnClosed = false;

	@Category("misc")
	public boolean enableTextureSpoofing = true;

	@Category("misc")
	public transient DescriptionLine overlay_misc;
	@Category("misc")
	@FloatSlider(min = 0F, max = 1F, step = 0.05F, unit = "%")
	public float overlay_opacity = 0.3F;

	@Dropdown("lock")
	@Category("misc")
	@FloatSlider(min = 0.2F, max = 10F, step = 0.2F, unit = "s")
	public float lock_textCooldown = 1F;

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
	public AbilityHandler.DurationRenderMode abilitiesDisplay_durationRenderMode = AbilityHandler.DurationRenderMode.BAR;
	@Category("abilities")
	public DurationBarSideMode abilitiesDisplay_durationBar_side = DurationBarSideMode.FOLLOW;
	@Category("abilities")
	@IntSlider(min = -32, max = 32, unit = " pixels")
	public int abilitiesDisplay_durationBar_offsetY = -20;
	@Category("abilities")
	@IntSlider(min = -32, max = 32, unit = " pixels")
	public int abilitiesDisplay_durationBar_offsetX = 2;

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


	/**
	 * Discord RPC Configuration
	 */
	@Category("discord")
	public boolean discordEnabled = true;
	/**
	 * discordDetails replace values:
	 * {player} returns the player's name
	 * {shard} returns the shard name
	 * {holding} returns the item held in the main hand
	 * {class} returns the class the user is playing as
	 * {location} returns the location or if not found the shard name
	 * everything else is a string literal
	 */
	@Category("discord")
	public String discordDetails = "{player} is in {shard}";
	@Category("discord")
	public boolean hideShardMode = false;

	@Category("effectOverlay")
	public boolean effect_enabled = true;
	@Category("effectOverlay")
	public ElementPosition effect_position = new ElementPosition(0.5f, 0, 0.0f, 0, 0.5f, 0);

	@Category("effectOverlay")
	public boolean effect_compress = true;
	@Category("effectOverlay")
	public boolean effect_textAlightRight = false;
	@Category("effectOverlay")
	@IntSlider(min = 50, max = 500)
	public int effect_width = 200;

	@Category("chestCountOverlay")
	public boolean chestCount_enabled = true;
	@Category("chestCountOverlay")
	public ElementPosition chestCount_position = new ElementPosition(1f, 0, 0.0f, 0, 1f, 0);

	@Category("debug")
	public boolean debugOptionsEnabled = false;
	@Category("debug")
	public boolean logPackets = false;
	@Category("debug")
	public boolean shardDebug = false;
	@Category("debug")
	public boolean enableChestCountMaxError = true;

	@Category("debug")
	public boolean logEffectPackets = false;

	@Dropdown("lock")
	@Category("debug")
	public boolean lock_renderDebuggingAdvancedLock = false;

	public void onUpdate() {
		if (abilitiesDisplay_preset != AbilityOptionPreset.CUSTOM) {
			abilitiesDisplay_horizontal = abilitiesDisplay_preset.horizontal;
			abilitiesDisplay_position = abilitiesDisplay_preset.position.clone();
			abilitiesDisplay_preset = AbilityOptionPreset.CUSTOM;
		}

		try {
			if (UnofficialMonumentaModClient.canInitializeDiscord()) {
				if (UnofficialMonumentaModClient.options.discordEnabled) {
					if (UnofficialMonumentaModClient.discordRPC.isInitialized()) {
						UnofficialMonumentaModClient.discordRPC.updateDiscordRPCDetails();
					} else {
						UnofficialMonumentaModClient.discordRPC.Init();
					}
				} else {
					if (UnofficialMonumentaModClient.discordRPC.isInitialized()) {
						UnofficialMonumentaModClient.discordRPC.shutdown();
					}
				}
			}
		} catch (Exception e) {
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to update Discord Presence data: ", e);
		}

		UnofficialMonumentaModClient.saveConfig();
	}

	public boolean categoryVisible(String category) {
		return debugOptionsEnabled || !category.equals("debug");
	}

	public enum DurationBarSideMode {
		FOLLOW(),
		HORIZONTAL(),
		VERTICAL()
	}

}
