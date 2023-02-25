package ch.njol.unofficialmonumentamod.mixins.screen;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("y")
	int getY();

	@Accessor("x")
	int getX();

	@Accessor("backgroundWidth")
	int getBackGroundWidth();
	
	@Invoker("getSlotAt")
	Slot doGetSlotAt(double x, double y);
}
