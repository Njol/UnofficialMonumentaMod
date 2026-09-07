package ch.njol.unofficialmonumentamod.mixins.screen;

import ch.njol.unofficialmonumentamod.core.gui.InventoryWidget;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {
    //allows the pass-through of mouseScrolled events to inventory widgets.
    @Inject(at = @At("HEAD"), method = "mouseScrolled", cancellable = true)
    void umm$onMouseScrolled(double mouseX, double mouseY, double horizontalScroll, double verticalScroll, CallbackInfoReturnable<Boolean> cir) {
        if (((CreativeInventoryScreen) (Object) this).hoveredElement(mouseX, mouseY).filter(
                (el) -> (el instanceof InventoryWidget) && el.mouseScrolled(mouseX, mouseY, horizontalScroll, verticalScroll)
        ).isPresent()) {
            cir.setReturnValue(true);
        };
    }
}
