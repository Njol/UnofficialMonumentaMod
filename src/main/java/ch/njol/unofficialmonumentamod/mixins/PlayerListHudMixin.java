package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
	@Unique
	private String getShard(Text text) {
		String shard = Locations.getShortShardFrom(text);
		if (shard == null || Objects.equals(shard, "unknown")) {
			return null;
		}

		return shard;
	}

	@Inject(method = "setHeader", at = @At("TAIL"))
	public void umm$onPlayerListHeader(Text header, CallbackInfo ci) {
		ShardData.onShardChange(getShard(header));
	}
}
