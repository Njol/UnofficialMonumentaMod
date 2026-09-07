package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.core.shard.ShardLoader;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
	@Inject(method = "setHeader", at = @At("TAIL"))
	public void umm$onPlayerListHeader(Text header, CallbackInfo ci) {
		ShardLoader.loadShardFromTabHeader(ShardData.getShard(header));
	}
}
