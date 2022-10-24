package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {

    @Invoker("addSelectableChild")
    <T extends Element & Selectable> T invokeAddSelectableChild(T child);
}
