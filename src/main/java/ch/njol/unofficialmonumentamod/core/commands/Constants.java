package ch.njol.unofficialmonumentamod.core.commands;

import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class Constants {
    //Styles
    public static final Style MAIN_INFO_STYLE = Style.EMPTY.withColor(Formatting.AQUA);
    public static final Style SUB_INFO_STYLE = Style.EMPTY.withColor(Formatting.GRAY);
    public static final Style ERROR_STYLE = Style.EMPTY.withColor(Formatting.RED);

    public static final Style KEY_INFO_STYLE = Style.EMPTY.withColor(Formatting.DARK_GRAY);
    public static final Style VALUE_STYLE = Style.EMPTY.withColor(Formatting.DARK_AQUA);
    public static final Style MOD_INFO_STYLE = Style.EMPTY.withColor(Formatting.DARK_GREEN);
}
