package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.misc.managers.ItemNameSpoofer;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mixin(InGameHud.class)
public class InGameHudMixin extends DrawableHelper {

    @Unique
    private static final Identifier COOLDOWN_OVERLAY = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/cooldown_overlay.png");
    @Unique
    private static final Identifier COOLDOWN_FLASH = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/off_cooldown.png");
    @Unique
    private static final Identifier UNKNOWN_ABILITY_ICON = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/unknown_ability.png");
    @Unique
    private static final Identifier UNKNOWN_CLASS_BORDER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/unknown_border.png");

    @Unique
    private static final Pattern IDENTIFIER_SANITATION_PATTERN = Pattern.compile("[^a-zA-Z0-9/._-]");

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private int scaledWidth;

    @Shadow
    private int scaledHeight;

    @Shadow private ItemStack currentStack;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
    void renderSkills_beforeStatusEffects(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!renderInFrontOfChat()) {
            renderAbilities(matrices, tickDelta, false);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;getObjectiveForSlot(I)Lnet/minecraft/scoreboard/ScoreboardObjective;", shift = At.Shift.BEFORE),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/util/math/MatrixStack;I)V")))
    void renderSkills_afterChat(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (renderInFrontOfChat()) {
            renderAbilities(matrices, tickDelta, true);
        }
    }

    /**
     *  Used for item name spoofing, required for the text to be centered
     */
    @ModifyVariable(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasCustomName()Z"), ordinal = 0)
    private MutableText getSpoofedName(MutableText value) {
        return getSpoofed(value);
    }

    /**
     *  Used for item name spoofing, required to have the right text style.
     */
    @ModifyVariable(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/StringVisitable;)I"), ordinal = 0)
    private MutableText spoofedName(MutableText value) {//overrides the style changes.
        return getSpoofed(value);
    }

    @Unique
    private MutableText getSpoofed(MutableText text) {
        MutableText spoofedName = ItemNameSpoofer.getSpoofedName(this.currentStack);
        if (!Objects.equals(spoofedName.getString(), this.currentStack.getName().getString())) {
            return (spoofedName);
        }
        return text;
    }

    @Unique
    private boolean renderInFrontOfChat() {
        return UnofficialMonumentaModClient.options.abilitiesDisplay_inFrontOfChat
                && !(client.currentScreen instanceof ChatScreen);
    }

    @Unique
    private void renderAbilities(MatrixStack matrices, float tickDelta, boolean inFrontOfChat) {
        if (client.options.hudHidden || client.player == null || client.player.isSpectator()) {
            return;
        }
        Options options = UnofficialMonumentaModClient.options;
        if (!options.abilitiesDisplay_enabled) {
            return;
        }

        AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
        synchronized (abilityHandler) {
            List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
            if (abilityInfos.isEmpty()) {
                return;
            }

            // NB: this code is partially duplicated in ChatScreenMixin!

            abilityInfos = abilityInfos.stream().filter(a -> UnofficialMonumentaModClient.isAbilityVisible(a, true)).collect(Collectors.toList());

            int iconSize = options.abilitiesDisplay_iconSize;
            int iconGap = options.abilitiesDisplay_iconGap;

            int textColor = 0xFF000000 | options.abilitiesDisplay_textColorRaw;

            float silenceCooldownFraction = abilityHandler.initialSilenceDuration <= 0 || abilityHandler.silenceDuration <= 0 ? 0 : 1f * abilityHandler.silenceDuration / abilityHandler.initialSilenceDuration;

            // multiple passes to render multiple layers.
            // layer 0: textures
            // layer 1: numbers
            for (int layer = 0; layer < 2; layer++) {

                Point point = Utils.abilitiesDisplay.getAbilitiesOrigin(abilityInfos, this.scaledWidth, this.scaledHeight, true);
                int x = point.x;
                int y = point.y;

                for (int i = 0; i < abilityInfos.size(); i++) {
                    AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(Utils.abilitiesDisplay.isAscendingOrder() ? i : abilityInfos.size() - 1 - i);

                    if (UnofficialMonumentaModClient.isAbilityVisible(abilityInfo, false)) {
                        // some settings are affected by called methods, so set them anew for each ability to render
                        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.enableRescaleNormal();
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.enableAlphaTest();

                        if (layer == 0) {

                            float animTicks = abilityInfo.offCooldownAnimationTicks + tickDelta;
                            float animLength = 2; // half of length actually
                            float scaledIconSize = iconSize * (options.abilitiesDisplay_offCooldownResize ? 1 + 0.08f * Utils.smoothStep(1 - Math.abs(animTicks - animLength) / animLength) : 1);
                            float scaledX = x - (scaledIconSize - iconSize) / 2;
                            float scaledY = y - (scaledIconSize - iconSize) / 2;

                            bindTextureOrDefault(getAbilityFileIdentifier(abilityInfo.className, abilityInfo.name, abilityInfo.mode), UNKNOWN_ABILITY_ICON);
                            Utils.abilitiesDisplay.drawTextureSmooth(matrices, scaledX, scaledY, scaledIconSize, scaledIconSize);

                            // silenceCooldownFraction is >= 0 so this is also >= 0
                            float cooldownFraction = abilityInfo.initialCooldown <= 0 ? 0 : Math.min(Math.max((abilityInfo.remainingCooldown - tickDelta) / abilityInfo.initialCooldown, silenceCooldownFraction), 1);
                            if (cooldownFraction > 0) {
                                // cooldown overlay is a series of 16 images, starting will full cooldown at the top, and successive shorter cooldowns below.
                                final int numCooldownTextures = 16;
                                int cooldownTextureIndex = (int) Math.floor((1 - cooldownFraction) * numCooldownTextures);
                                this.client.getTextureManager().bindTexture(COOLDOWN_OVERLAY);
                                Utils.abilitiesDisplay.drawTextureSmooth(matrices,
                                        scaledX, scaledY, scaledIconSize, scaledIconSize,
                                        0, 1, 1f * cooldownTextureIndex / numCooldownTextures, 1f * (cooldownTextureIndex + 1) / numCooldownTextures);
                            }
                            if (options.abilitiesDisplay_offCooldownFlashIntensity > 0 && animTicks < 8) {
                                this.client.getTextureManager().bindTexture(COOLDOWN_FLASH);
                                RenderSystem.color4f(1, 1, 1, options.abilitiesDisplay_offCooldownFlashIntensity * (1 - animTicks / 8f));
                                Utils.abilitiesDisplay.drawTextureSmooth(matrices, scaledX, scaledY, scaledIconSize, scaledIconSize);
                                RenderSystem.color4f(1, 1, 1, 1);
                            }

                            bindTextureOrDefault(getBorderFileIdentifier(abilityInfo.className, abilityHandler.silenceDuration > 0), UNKNOWN_CLASS_BORDER);
                            Utils.abilitiesDisplay.drawTextureSmooth(matrices, scaledX, scaledY, scaledIconSize, scaledIconSize);

                        } else {

                            if ((abilityInfo.remainingCooldown > 0 || abilityHandler.silenceDuration > 0) && options.abilitiesDisplay_showCooldownAsText) {
                                String cooldownString = "" + (int) Math.ceil(Math.max(Math.max(abilityInfo.remainingCooldown, abilityHandler.silenceDuration), 0) / 20f);
                                drawText(matrices, cooldownString,
                                        x + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.getWidth(cooldownString),
                                        y + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.fontHeight,
                                        textColor, inFrontOfChat);
                            }

                            if (abilityInfo.maxCharges > 1) {
                                drawText(matrices, "" + abilityInfo.charges, x + options.abilitiesDisplay_textOffset, y + options.abilitiesDisplay_textOffset, textColor, inFrontOfChat);
                            }

                        }
                    }

                    if (Utils.abilitiesDisplay.isHorizontal()) {
                        x += (Utils.abilitiesDisplay.isAscendingOrder() ? 1 : -1) * (iconSize + iconGap);
                    } else {
                        y += (Utils.abilitiesDisplay.isAscendingOrder() ? 1 : -1) * (iconSize + iconGap);
                    }

                }
            }
        }
    }

    @Unique
    private void bindTextureOrDefault(Identifier identifier, Identifier defaultIdentifier) {
        this.client.getTextureManager().bindTexture(identifier);
        AbstractTexture texture = this.client.getTextureManager().getTexture(identifier);
        if (texture == null || texture == MissingSprite.getMissingSpriteTexture()) {
            this.client.getTextureManager().bindTexture(defaultIdentifier);
        }
    }

    @Unique
    private static final Map<String, Identifier> abilityIdentifiers = new HashMap<>();

    @Unique
    private static Identifier getAbilityFileIdentifier(String className, String name, @Nullable String mode) {
        return abilityIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + "/" + name + (mode == null ? "" : "_" + mode),
                key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
                        "textures/abilities/" + sanitizeForIdentifier(key) + ".png"));
    }

    @Unique
    private static final Map<String, Identifier> borderIdentifiers = new HashMap<>();

    @Unique
    private static Identifier getBorderFileIdentifier(String className, boolean silenced) {
        return borderIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + (silenced ? "_silenced" : ""),
                key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
                        "textures/abilities/" + sanitizeForIdentifier(className == null ? "unknown" : className) + "/border" + (silenced ? "_silenced" : "") + ".png"));
    }

    @Unique
    private static String sanitizeForIdentifier(String string) {
        return IDENTIFIER_SANITATION_PATTERN.matcher(string).replaceAll("_").toLowerCase(Locale.ROOT);
    }

    /**
     * Draws text with a full black border around it
     */
    @Unique
    private void drawText(MatrixStack matrices, String text, int x, int y, int color, boolean inFrontOfChat) {
        matrices.push();
        matrices.translate(0, 0, inFrontOfChat ? 101 : 1); // chat is drawn at +100
        this.client.textRenderer.draw(matrices, text, x - 1, y, 0);
        this.client.textRenderer.draw(matrices, text, x, y - 1, 0);
        this.client.textRenderer.draw(matrices, text, x + 1, y, 0);
        this.client.textRenderer.draw(matrices, text, x, y + 1, 0);
        matrices.translate(0, 0, 0.03f);
        this.client.textRenderer.draw(matrices, text, x, y, color);
        matrices.pop();
    }

    /**
     * If configured, do not show ability messages
     * TOD the messages here are translated, so only work for English. Maybe just check for the ability name in the message? (assuming that one isn't translated as well...)
     * TOD Or maybe make this an option server-side?
     * NOTE about the TODOS above: they're not translated.
     */
    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    void setOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        synchronized (UnofficialMonumentaModClient.abilityHandler) {
            if (!UnofficialMonumentaModClient.options.abilitiesDisplay_hideAbilityRelatedMessages
                    || !UnofficialMonumentaModClient.options.abilitiesDisplay_enabled
                    || UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty()) {
                return;
            }
            String m = message.getString();
            if (StringUtils.startsWithIgnoreCase(m, "You are silenced")
                    || StringUtils.startsWithIgnoreCase(m, "All your cooldowns have been reset")
                    || StringUtils.startsWithIgnoreCase(m, "Cloak stacks:")
                    || StringUtils.startsWithIgnoreCase(m, "Rage:")
                    || StringUtils.startsWithIgnoreCase(m, "Holy energy radiates from your hands")
                    || StringUtils.startsWithIgnoreCase(m, "The light from your hands fades")) {
                ci.cancel();
                return;
            }
            for (AbilityHandler.AbilityInfo abilityInfo : UnofficialMonumentaModClient.abilityHandler.abilityData) {
                if (StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " is now off cooldown")
                        || StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " has been activated")
                        || StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " stacks")
                        || StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " charges")) {
                    ci.cancel();
                    return;
                }
            }
        }
    }

}
