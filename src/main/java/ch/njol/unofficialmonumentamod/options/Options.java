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

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface FloatSlider {
		float min();

		float max();

		float step();

		String unit();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface IntSlider {
		int min();

		int max();
	}

	public interface DescriptionLine {
	}

	public static class Position {
		public float offsetXRelative;
		public float offsetYRelative;
		public int offsetXAbsolute;
		public int offsetYAbsolute;
		public float alignX;
		public float alignY;

		public Position() {
		}

		public Position(float offsetXRelative, int offsetXAbsolute, float offsetYRelative, int offsetYAbsolute, float alignX, float alignY) {
			this.offsetXRelative = offsetXRelative;
			this.offsetXAbsolute = offsetXAbsolute;
			this.offsetYRelative = offsetYRelative;
			this.offsetYAbsolute = offsetYAbsolute;
			this.alignX = alignX;
			this.alignY = alignY;
		}

		public Position clone() {
			Position clone = new Position();
			clone.offsetXRelative = offsetXRelative;
			clone.offsetYRelative = offsetYRelative;
			clone.offsetXAbsolute = offsetXAbsolute;
			clone.offsetYAbsolute = offsetYAbsolute;
			clone.alignX = alignX;
			clone.alignY = alignY;
			return clone;
		}
	}

	public enum HudMode {
		VANILLA, REPLACE, REMOVE;
	}

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
	public Position abilitiesDisplay_position = AbilityOptionPreset.ABOVE_HOTBAR.position.clone();

	@Category("abilities")
	public transient DescriptionLine abilitiesDisplay_miscInfo;
	@Category("abilities")
	public boolean abilitiesDisplay_offCooldownResize = true;
	@Category("abilities")
	@FloatSlider(min = 0, max = 1, step = 0.01f, unit = "%")
	public float abilitiesDisplay_offCooldownFlashIntensity = 1;
	@Category("abilities")
	@FloatSlider(min = 0, max = 1, step = 0.01f, unit = "%")
	public float abilitiesDisplay_offCooldownSoundVolume = 0f;
	@Category("abilities")
	@FloatSlider(min = 0, max = 2, step = 0.05f, unit = "")
	public float abilitiesDisplay_offCooldownSoundPitchMin = 1f;
	@Category("abilities")
	@FloatSlider(min = 0, max = 2, step = 0.05f, unit = "")
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

	@Category("hud")
	public DescriptionLine hud_info;
	@Category("hud")
	public boolean hud_enabled = false;
	@Category("hud")
	public boolean hud_statusBarsEnabled = true;
	@Category("hud")
	public float hud_regenerationSpeed = 10;
	@Category("hud")
	public boolean hud_healthText = true;
	@Category("hud")
	public int hud_healthTextOffset = -5;
	@Category("hud")
	public boolean hud_healthMirror = false;
	@Category("hud")
	public boolean hud_hungerMirror = false;
	@Category("hud")
	public boolean hud_breathMirror = true;
	@Category("hud")
	public boolean hud_hideBreathWithWaterBreathing = false;
	@Category("hud")
	public boolean hud_mountHealthEnabled = true;
	@Category("hud")
	public boolean hud_mountHealthText = true;
	@Category("hud")
	public int hud_mountHealthTextOffset = -5;
	@Category("hud")
	public boolean hud_mountHealthMirror = false;
	@Category("hud")
	public boolean hud_moveOverlayMessage = true;
	@Category("hud")
	public boolean hud_moveHeldItemTooltip = true;
	@Category("hud")
	public DescriptionLine hud_positionsInfo;
	@Category("hud")
	public Position hud_healthBarPosition = new Position(0.5f, 0, 1.0f, -39, 0.5f, 1.0f);
	@Category("hud")
	public Position hud_hungerBarPosition = new Position(0.5f, 60, 1.0f, -22, 0.5f, 1.0f);
	@Category("hud")
	public Position hud_breathBarPosition = new Position(0.5f, -60, 1.0f, -22, 0.5f, 1.0f);
	@Category("hud")
	public Position hud_mountHealthBarPosition = new Position(0.5f, 60, 1.0f, -55, 0.5f, 1.0f);
	@Category("hud")
	public Position hud_overlayMessagePosition = new Position(0.5f, 0, 1.0f, -105, 0.5f, 1.0f);
	@Category("hud")
	public Position hud_heldItemTooltipPosition = new Position(0.5f, 0, 1.0f, -36, 0.5f, 1.0f);

	/**
	 * List of [class]/[ability]. Abilities not present in this list are sorted alphabetically.
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
	}

}
