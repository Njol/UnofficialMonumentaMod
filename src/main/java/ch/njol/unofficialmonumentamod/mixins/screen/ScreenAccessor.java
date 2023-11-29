package ch.njol.unofficialmonumentamod.mixins.screen;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("remove")
    void doRemove(Element child);

    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T doAddDrawableChild(T drawableElement);
}
