package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onCapeRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci, ItemStack itemStack) {
        if (UnofficialMonumentaModClient.options.enableTextureSpoofing && TextureSpoofer.wouldveBeenEdited(itemStack)) {
            if (UnofficialMonumentaModClient.spoofer.apply(itemStack).getItem() == Items.ELYTRA) {
                ci.cancel();
            }
        }
    }
}
