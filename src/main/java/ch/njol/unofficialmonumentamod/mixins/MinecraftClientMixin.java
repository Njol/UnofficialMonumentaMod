package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.managers.CooldownManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow @Nullable public HitResult crosshairTarget;

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    void disconnect(Screen screen, CallbackInfo ci) {
        UnofficialMonumentaModClient.onDisconnect();
    }

    // Send a position update before firing a crossbow for the Recoil fix to work
    // NB: not needed on MC 1.17
    @Inject(method = "doItemUse()V", at = @At("HEAD"))
    void doItemUse_crossbowFix(CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (CooldownManager.shouldRender() && CooldownManager.getCooldownFromItem(player.getMainHandStack()) != null) {
            CooldownManager.addCooldownToItem(player.getMainHandStack(), CooldownManager.Trigger.MAIN_HAND);
        } else if (!CooldownManager.shouldRender()) {
            CooldownManager.addCooldownToItem(player.getMainHandStack(), CooldownManager.Trigger.MAIN_HAND);
        }
        if (!UnofficialMonumentaModClient.options.crossbowFix) return;
        if (player.getMainHandStack() != null && player.getMainHandStack().getItem() == Items.CROSSBOW
                || player.getOffHandStack() != null && player.getOffHandStack().getItem() == Items.CROSSBOW) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(player.yaw, player.pitch, player.isOnGround()));
        }
    }

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
    private void onEntityAttacked(CallbackInfo ci) {
        if (this.crosshairTarget == null) return;
        Entity entity = ((EntityHitResult)this.crosshairTarget).getEntity();
        if (entity.getType() == EntityType.ARMOR_STAND && entity.getDisplayName().getString().matches(".*'s Grave") && !Objects.equals(entity.getDisplayName().getString(), String.format("%s's Grave", MinecraftClient.getInstance().player.getName().getString()))) {
            if (CooldownManager.shouldRender() && CooldownManager.getCooldownFromItem(player.getMainHandStack()) != null) {
                CooldownManager.addCooldownToItem(player.getMainHandStack(), CooldownManager.Trigger.INTERACT_GRAVE);
            } else if (!CooldownManager.shouldRender()) {
                CooldownManager.addCooldownToItem(player.getMainHandStack(), CooldownManager.Trigger.INTERACT_GRAVE);
            }
        }
    }

}
