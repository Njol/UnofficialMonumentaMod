package ch.njol.unofficialmonumentamod;

import ch.njol.minecraft.uiframework.ElementPosition;

public enum AbilityOptionPreset {

	CUSTOM("Custom", false, 0, 0, 0, 0, 0),
	ABOVE_HOTBAR("Above Hotbar", true, 0.5f, 0.5f, 1.0f, 0, -73),
	RIGHT_OF_HOTBAR("Right of Hotbar", true, 0, 0.5f, 1.0f, 107, 5),
	LEFT_SIDE_OF_SCREEN("Left side of Screen", false, 0.5f, 0, 0.5f, 0, 0),
	RIGHT_SIDE_OF_SCREEN("Right side of Screen", false, 0.5f, 1.0f, 0.5f, 0, 0),

	;

	public final String name;
	public final boolean horizontal;
	public final ElementPosition position = new ElementPosition();

	AbilityOptionPreset(String name, boolean horizontal, float align, float offsetXRelative, float offsetYRelative, int offsetXAbsolute, int offsetYAbsolute) {
		this.name = name;
		this.horizontal = horizontal;
		position.alignX = horizontal ? align : offsetXRelative > 0.5 ? 1 : 0;
		position.alignY = horizontal ? (offsetYRelative > 0.5 ? 1 : 0) : align;
		position.offsetXRelative = offsetXRelative;
		position.offsetYRelative = offsetYRelative;
		position.offsetXAbsolute = offsetXAbsolute;
		position.offsetYAbsolute = offsetYAbsolute;
	}

	@Override
	public String toString() {
		return name;
	}

}
