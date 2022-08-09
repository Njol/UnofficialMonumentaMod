package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.managers.CooldownManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    private static final Transformation THIRD_PERSON_RIGHT_HAND_TRANSFORM = new Transformation(new Vector3f(0, -90, 55), new Vector3f(0, 0.2f, 0), new Vector3f(1.7f, 1.7f, 0.85f));
    private static final Transformation THIRD_PERSON_LEFT_HAND_TRANSFORM = new Transformation(new Vector3f(0, 90, -55), new Vector3f(0, 0.2f, 0), new Vector3f(1.7f, 1.7f, 0.85f));
    private static final Transformation FIRST_PERSON_RIGHT_HAND_TRANSFORM = new Transformation(new Vector3f(0, -90, 70), new Vector3f(0.1f, 0.2f, 0.1f), new Vector3f(1.36f, 1.36f, 0.68f));
    private static final Transformation FIRST_PERSON_LEFT_HAND_TRANSFORM = new Transformation(new Vector3f(0, 90, -70), new Vector3f(0.1f, 0.2f, 0.1f), new Vector3f(1.36f, 1.36f, 0.68f));

    @Shadow
    @Final
    private ItemModels models;

    @Shadow public float zOffset;

    /**
     * Pretend that tridents are apples so that the trident-specific code is not executed
     */
    @Redirect(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    public Item renderItem_tridentFix1(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (UnofficialMonumentaModClient.options.overrideTridentRendering && item == Items.TRIDENT)
            return Items.APPLE;
        return item;
    }

    /**
     * Always use the trident item model instead of the entity model
     */
    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            ordinal = 0)
    public BakedModel renderItem_tridentFix2(BakedModel model, ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model2) {
        if (UnofficialMonumentaModClient.options.overrideTridentRendering
                && stack.getItem() == Items.TRIDENT)
            return models.getModelManager().getModel(new ModelIdentifier("minecraft:trident#inventory"));
        return model;
    }

    @Unique
    private static ItemStack contextStack;
    @Unique
    private static MatrixStack contextMatrices;

    /**
     * See setK for use
     */
    @Inject(method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getCount()I", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void getContextForRenderGuiItemOverlay(TextRenderer renderer, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci, MatrixStack matrixStack) {
        contextStack = stack;
        contextMatrices = matrixStack;

    }

    /**
     *  Will trigger the vanilla cooldown render with item cooldown :)
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")//I have no idea why it's screaming saying it doesn't exist, works for me though.
    @ModifyVariable(method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "STORE", ordinal = 0), index = 8)
    private float setK(float value) {
        if (CooldownManager.getCooldownProgress(contextStack.getItem(), MinecraftClient.getInstance().getTickDelta()) > 0.0F) {
            return CooldownManager.getCooldownProgress(contextStack.getItem(), MinecraftClient.getInstance().getTickDelta());
        } else return value;
    }

    /**
     *  Renders the charges for items under cooldown
     */
    @Inject(method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 1))
    private void renderItemCharges(TextRenderer renderer, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {//doesn't update if in cooldown
        if (CooldownManager.getCooldownProgress(stack.getItem(), MinecraftClient.getInstance().getTickDelta()) > 0.0F) {
            String s = String.valueOf(CooldownManager.getMaxItemCharges(stack.getItem()) - CooldownManager.getItemCharges(stack.getItem()));
            contextMatrices.translate(0.0D, 0.0D, (double)(this.zOffset + 200.0F));
            renderer.draw(contextMatrices, s, (float) (x + 19 - 2 - renderer.getWidth(s)), (float) (y), 16777215);
        }
    }

    /**
     * Always use the hardcoded transforms here where the trident model was otherwise used (except HEAD which really should not be used...)
     */
    @Redirect(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/json/ModelTransformation;getTransformation(Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)Lnet/minecraft/client/render/model/json/Transformation;"))
    Transformation getTransformation_tridentFix3(ModelTransformation modelTransformation, ModelTransformation.Mode renderMode, ItemStack stack, ModelTransformation.Mode renderMode2, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model) {
        if (UnofficialMonumentaModClient.options.overrideTridentRendering
                && stack.getItem() == Items.TRIDENT) {
            switch (renderMode) {
                case THIRD_PERSON_RIGHT_HAND:
                    return THIRD_PERSON_RIGHT_HAND_TRANSFORM;
                case THIRD_PERSON_LEFT_HAND:
                    return THIRD_PERSON_LEFT_HAND_TRANSFORM;
                case FIRST_PERSON_RIGHT_HAND:
                    return FIRST_PERSON_RIGHT_HAND_TRANSFORM;
                case FIRST_PERSON_LEFT_HAND:
                    return FIRST_PERSON_LEFT_HAND_TRANSFORM;
            }
        }
        return modelTransformation.getTransformation(renderMode);
    }
}
