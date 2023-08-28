package ch.njol.unofficialmonumentamod.mixins;

import java.util.Comparator;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
	@Accessor("header")
	Text getHeader();

	@Accessor("footer")
	Text getFooter();

	@Accessor("ENTRY_ORDERING")
	Comparator<PlayerListEntry> getOrdering();
}
