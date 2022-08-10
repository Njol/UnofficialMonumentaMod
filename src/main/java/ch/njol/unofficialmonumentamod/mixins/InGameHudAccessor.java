package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
	@Accessor
	Text getOverlayMessage();

	@Accessor
	ItemStack getCurrentStack();

	@Accessor
	void setHeldItemTooltipFade(int heldItemTooltipFade);

	@Accessor
	void setCurrentStack(ItemStack currentStack);
}
