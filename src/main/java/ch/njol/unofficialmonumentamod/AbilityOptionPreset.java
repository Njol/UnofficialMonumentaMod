package ch.njol.unofficialmonumentamod;

public enum AbilityOptionPreset {

	CUSTOM("Custom", false, 0, 0, 0, 0, 0),
	ABOVE_HOTBAR("Above Hotbar", true, 0.5f, 0.5f, 1.0f, 0, -80),
	RIGHT_OF_HOTBAR("Right of Hotbar", true, 0, 0.5f, 1.0f, 90, -27),
	LEFT_SIDE_OF_SCREEN("Left side of Screen", false, 0.5f, 0, 0.5f, 0, 0),
	RIGHT_SIDE_OF_SCREEN("Right side of Screen", false, 0.5f, 1.0f, 0.5f, -32, 0),

	;

	public final String name;
	public final boolean horizontal;
	public final float align;
	public final float offsetXRelative;
	public final float offsetYRelative;
	public final int offsetXAbsolute;
	public final int offsetYAbsolute;

	AbilityOptionPreset(String name, boolean horizontal, float align, float offsetXRelative, float offsetYRelative, int offsetXAbsolute, int offsetYAbsolute) {
		this.name = name;
		this.horizontal = horizontal;
		this.align = align;
		this.offsetXRelative = offsetXRelative;
		this.offsetYRelative = offsetYRelative;
		this.offsetXAbsolute = offsetXAbsolute;
		this.offsetYAbsolute = offsetYAbsolute;
	}

	@Override
	public String toString() {
		return name;
	}

}
