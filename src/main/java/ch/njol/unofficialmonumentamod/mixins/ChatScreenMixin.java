package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
	@Unique
	private final MinecraftClient mc = MinecraftClient.getInstance();

	@Unique
	private String draggedAbility = null;

	@Unique
	private boolean draggingAbilityDisplay = false;
	@Unique
	private double dragX;
	@Unique
	private double dragY;

	protected ChatScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "mouseClicked(DDI)Z", at = @At("RETURN"), cancellable = true)
	void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			return;
		}
		if (UnofficialMonumentaModClient.options.abilitiesDisplay_enabled) {
			AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
			synchronized (abilityHandler) {
				List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
				if (abilityInfos.isEmpty()) {
					return;
				}
				abilityInfos = abilityInfos.stream().filter(a -> UnofficialMonumentaModClient.isAbilityVisible(a, true)).collect(Collectors.toList());

				int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY, true);
				if (index < 0) {
					return;
				}
				if (Screen.hasControlDown()) {
					draggingAbilityDisplay = true;
					Point origin = Utils.abilitiesDisplay.getAbilitiesOrigin(abilityInfos, this.width, this.height);
					dragX = mouseX - origin.x;
					dragY = mouseY - origin.y;
				} else {
					draggedAbility = abilityInfos.get(index).getOrderId();
					UnofficialMonumentaModClient.isReorderingAbilities = true;
				}
			}
		}

		cir.setReturnValue(true);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		}
		if (draggedAbility != null) {
			AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
			synchronized (abilityHandler) {
				List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
				if (abilityInfos.isEmpty()) {
					return false;
				}
				int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY, false);
				if (index < 0) {
					return false;
				}
				String abilityAtCurrentPos = abilityInfos.get(index).getOrderId();
				if (abilityAtCurrentPos.equals(draggedAbility)) {
					return false;
				}
				List<String> order = new ArrayList<>(UnofficialMonumentaModClient.options.abilitiesDisplay_order);
				int currentAbiOrderIndex = order.indexOf(abilityAtCurrentPos);
				if (currentAbiOrderIndex < 0) // shouldn't happen
				{
					return false;
				}
				order.remove(draggedAbility);
				order.add(currentAbiOrderIndex, draggedAbility);
				UnofficialMonumentaModClient.options.abilitiesDisplay_order = order;
				abilityHandler.sortAbilities();
			}
			return true;
		} else if (draggingAbilityDisplay) {
			AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
			synchronized (abilityHandler) {
				List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
				if (abilityInfos.isEmpty()) {
					return false;
				}

				Options options = UnofficialMonumentaModClient.options;
				int iconSize = options.abilitiesDisplay_iconSize;
				int iconGap = options.abilitiesDisplay_iconGap;
				boolean horizontal = options.abilitiesDisplay_horizontal;
				int totalSize = iconSize * abilityInfos.size() + iconGap * (abilityInfos.size() - 1);
				int sizeX = horizontal ? totalSize : iconSize;
				int sizeY = horizontal ? iconSize : totalSize;

				double newX = mouseX - dragX;
				double newY = mouseY - dragY;

				// Offsets to the sides and centers of the screen. The smallest offset of each direction will be used as anchor point.
				double left = newX;
				double horizontalMiddle = Math.abs(newX + sizeX / 2.0 - width / 2.0);
				double right = width - (newX + sizeX);
				double top = newY;
				double verticalMiddle = Math.abs(newY + sizeY / 2.0 - height / 2.0);
				double bottom = height - (newY + sizeY);

				options.abilitiesDisplay_offsetXRelative = left < horizontalMiddle && left < right ? 0 : horizontalMiddle < right ? 0.5f : 1;
				options.abilitiesDisplay_offsetYRelative = top < verticalMiddle && top < bottom ? 0 : verticalMiddle < bottom ? 0.5f : 1;
				options.abilitiesDisplay_align = horizontal ? options.abilitiesDisplay_offsetXRelative : options.abilitiesDisplay_offsetYRelative;
				options.abilitiesDisplay_offsetXAbsolute = (int) Math.round(newX + (horizontal ? options.abilitiesDisplay_align * totalSize : 0) - (this.width * options.abilitiesDisplay_offsetXRelative));
				options.abilitiesDisplay_offsetYAbsolute = (int) Math.round(newY + (horizontal ? 0 : options.abilitiesDisplay_align * totalSize) - (this.height * options.abilitiesDisplay_offsetYRelative));

				// snap to center
				if (horizontal && options.abilitiesDisplay_offsetXRelative == 0.5f && Math.abs(options.abilitiesDisplay_offsetXAbsolute) < 10) {
					options.abilitiesDisplay_offsetXAbsolute = 0;
				} else if (!horizontal && options.abilitiesDisplay_offsetYRelative == 0.5f && Math.abs(options.abilitiesDisplay_offsetYAbsolute) < 10) {
					options.abilitiesDisplay_offsetYAbsolute = 0;
				}
			}
			return true;
		}
		return false;
	}

	@Unique
	private Point getClosestInBoundPosition(double x, double y, int width, int height) {
		Point point = new Point();

		if (-1 > x) {
			point.x = -1;
		}  else if ((mc.getWindow().getWidth() / 2) <= x + width) {
			point.x = (mc.getWindow().getWidth() / 2) - width;
		} else point.x = (int) x;

		if (-1 > y) {
			point.y = -1;
		} else if ((mc.getWindow().getHeight() / 2) <= y + height) {
			point.y = (mc.getWindow().getHeight() / 2) - height;
		} else point.y = (int) y;

		return point;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggedAbility != null || draggingAbilityDisplay) {
			draggedAbility = null;
			draggingAbilityDisplay = false;
			UnofficialMonumentaModClient.isReorderingAbilities = false;
			UnofficialMonumentaModClient.saveConfig();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Unique
	private int getClosestAbilityIndex(List<AbilityHandler.AbilityInfo> abilityInfos, double mouseX, double mouseY, boolean initialClick) {

		Point origin = Utils.abilitiesDisplay.getAbilitiesOrigin(abilityInfos, this.width, this.height);
		int x = origin.x;
		int y = origin.y;

		Options options = UnofficialMonumentaModClient.options;
		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;
		boolean horizontal = options.abilitiesDisplay_horizontal;

		int closestAbilityIndex;
		if (horizontal) {
			closestAbilityIndex = (int) Math.floor((mouseX - x + iconGap / 2.0) / (iconSize + iconGap));
		} else {
			closestAbilityIndex = (int) Math.floor((mouseY - y + iconGap / 2.0) / (iconSize + iconGap));
		}
		closestAbilityIndex = Math.max(0, Math.min(closestAbilityIndex, abilityInfos.size() - 1));
		if (initialClick) {
			// on first click make sure we're sufficiently close to the ability icon
			final double clickableFraction = 1; // textures are actually smaller than the whole icon size, so only make a part clickable
			double abiCenterX = (horizontal ? x + closestAbilityIndex * (iconSize + iconGap) : x) + iconSize / 2.0;
			double abiCenterY = (horizontal ? y : y + closestAbilityIndex * (iconSize + iconGap)) + iconSize / 2.0;
			if (Math.abs(abiCenterX - mouseX) > iconSize / 2.0 * clickableFraction
				    || Math.abs(abiCenterY - mouseY) > iconSize / 2.0 * clickableFraction) {
				return -1;
			}
		}
		return closestAbilityIndex;
	}

	@Inject(method = "removed()V", at = @At("HEAD"))
	public void removed(CallbackInfo ci) {
		draggedAbility = null;
		draggingAbilityDisplay = false;
		UnofficialMonumentaModClient.isReorderingAbilities = false;
	}

	@Redirect(method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;getText(DD)Lnet/minecraft/text/Style;"))
	public Style render(ChatHud instance, double x, double y, MatrixStack matrices, int mouseX, int mouseY, float delta) {
		Style style = instance.getText(x, y);
		if (!UnofficialMonumentaModClient.options.abilitiesDisplay_enabled
			    || !UnofficialMonumentaModClient.options.abilitiesDisplay_tooltips
			    || draggingAbilityDisplay
			    || draggedAbility != null
			    || style != null && style.getHoverEvent() != null) {
			return style;
		}
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		synchronized (abilityHandler) {
			List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
			if (abilityInfos.isEmpty()) {
				return style;
			}
			abilityInfos = abilityInfos.stream().filter(a -> UnofficialMonumentaModClient.isAbilityVisible(a, true)).collect(Collectors.toList());

			int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY, true);
			if (index < 0) {
				return style;
			}

			AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(index);
			renderTooltip(matrices, Text.of(abilityInfo.name), mouseX, mouseY);
			// TODO also display ability description NOTE: ability descriptions don't exist / aren't received rn
		}
		return style;
	}

}
