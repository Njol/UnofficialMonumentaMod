package ch.njol.unofficialmonumentamod.options;

import ch.njol.unofficialmonumentamod.AbilityOptionPreset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Options {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Category {
		String value();
	}

	@Category("misc")
	public boolean overrideTridentRendering = true;

	@Category("misc")
	public boolean chestsortDisabledForInventory = false;
	@Category("misc")
	public boolean chestsortDisabledForEnderchest = false;
	@Category("misc")
	public boolean chestsortDisabledEverywhereElse = false;

	// TODO implement item cooldown display
	// requires sever-side adaptions to send the cooldown (on use and on connect)
//	@Category("misc")
//	public boolean renderItemCooldowns = true;

	@Category("abilities")
	public boolean abilitiesDisplay_enabled = true;
	@Category("abilities")
	public boolean abilitiesDisplay_showOnlyOnCooldown = false;
	@Category("abilities")
	public boolean abilitiesDisplay_showCooldownAsText = true;
	@Category("abilities")
	public boolean abilitiesDisplay_hideAbilityRelatedMessages = true;

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
	public int abilitiesDisplay_iconSize = 32;
	@Category("abilities")
	public int abilitiesDisplay_iconGap = 0;
	@Category("abilities")
	public int abilitiesDisplay_textOffset = 4;
	@Category("abilities")
	public boolean abilitiesDisplay_ascendingRenderOrder = false;
	// TODO option to clip borders if they intersect?

	@Category("debug")
	public boolean debugOptionsEnabled = false;
	@Category("debug")
	public boolean logPackets = false;

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
