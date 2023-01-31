package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

//This accessor won't stay, it's here to see why everything is breaking down
@Mixin(SpriteAtlasTexture.class)
public interface SpriteAtlasTextureAccessor {
    @Accessor("sprites")
    Map<Identifier, Sprite> getSprites();
}
