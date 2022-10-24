package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("y")
    int getY();

    @Accessor("x")
    int getX();

    @Accessor("backgroundWidth")
    int getBackGroundWidth();
}
