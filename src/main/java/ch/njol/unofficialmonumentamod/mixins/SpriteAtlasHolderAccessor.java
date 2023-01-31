package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//This accessor class won't stay, it's only here to see what broke down
@Mixin(SpriteAtlasHolder.class)
public interface SpriteAtlasHolderAccessor {
    @Accessor("atlas")
    SpriteAtlasTexture getAtlas();
}
