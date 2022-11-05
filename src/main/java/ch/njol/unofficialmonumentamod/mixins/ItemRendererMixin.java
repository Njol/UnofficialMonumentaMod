package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BuiltinBakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    private static final Transformation THIRD_PERSON_RIGHT_HAND_TRANSFORM = new Transformation(new Vec3f(0, -90, 55), new Vec3f(0, 0.2f, 0), new Vec3f(1.7f, 1.7f, 0.85f));
    private static final Transformation THIRD_PERSON_LEFT_HAND_TRANSFORM = new Transformation(new Vec3f(0, 90, -55), new Vec3f(0, 0.2f, 0), new Vec3f(1.7f, 1.7f, 0.85f));
    private static final Transformation FIRST_PERSON_RIGHT_HAND_TRANSFORM = new Transformation(new Vec3f(0, -90, 70), new Vec3f(0.1f, 0.2f, 0.1f), new Vec3f(1.36f, 1.36f, 0.68f));
    private static final Transformation FIRST_PERSON_LEFT_HAND_TRANSFORM = new Transformation(new Vec3f(0, 90, -70), new Vec3f(0.1f, 0.2f, 0.1f), new Vec3f(1.36f, 1.36f, 0.68f));

    @Shadow
    @Final
    private ItemModels models;

    @Unique
    private BakedModel originalModel;

    /**
     * Prevent trident-specific code from being executed
     */
    @Redirect(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"),
            expect = 4)
    public boolean renderItem_tridentFix1(ItemStack itemStack, Item item) {
        if (UnofficialMonumentaModClient.options.overrideTridentRendering && item == Items.TRIDENT)
            return false;
        return itemStack.isOf(item);
    }

    /**
     * Always use the trident item model instead of the entity model
     */
    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            ordinal = 0, argsOnly = true)
    public BakedModel renderItem_tridentFix2(BakedModel model, ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model2) {
        originalModel = model;
        if (UnofficialMonumentaModClient.options.overrideTridentRendering
                && stack.getItem() == Items.TRIDENT)
            return models.getModelManager().getModel(new ModelIdentifier("minecraft:trident#inventory"));
        return model;
    }

    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("LOAD"),
            ordinal = 0, argsOnly = true)
    public BakedModel renderItem_tridentFix_citResewn(BakedModel model, ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model2) {
        if (UnofficialMonumentaModClient.options.overrideTridentRendering
                && stack.getItem() == Items.TRIDENT
                && (!(originalModel instanceof BuiltinBakedModel)))
            return originalModel;
        return model;
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

    @ModifyVariable(method = "renderGuiItemIcon", at = @At("HEAD"), argsOnly = true)
    private ItemStack editStackrGII(ItemStack value) {
        return UnofficialMonumentaModClient.spoofer.apply(value);
    }

    @ModifyVariable(method = "innerRenderInGui(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;IIII)V", at = @At("HEAD"), argsOnly = true)
    private ItemStack editStackiRIG(ItemStack value) {
        return UnofficialMonumentaModClient.spoofer.apply(value);
    }

    @ModifyVariable(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("HEAD"), argsOnly = true)
    private ItemStack editStackrIE(ItemStack value) {
        return UnofficialMonumentaModClient.spoofer.apply(value);
    }
}
