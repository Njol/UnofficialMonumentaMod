package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.features.strike.ChestCountOverlay;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "setHeader", at = @At("TAIL"))
    public void onPlayerListHeader(Text header, CallbackInfo ci) {
        Locations.resetCache();
        ChestCountOverlay.onPlayerListHeader(header);
    }
}
