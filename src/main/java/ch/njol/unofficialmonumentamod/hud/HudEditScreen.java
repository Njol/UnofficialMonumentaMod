package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.InGameHudAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {

	private final Screen parent;

	private final Hud hud = Hud.INSTANCE;

	public HudEditScreen(Screen parent) {
		super(Text.of(UnofficialMonumentaModClient.MOD_IDENTIFIER + " HUD Edit Screen"));
		this.parent = parent;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return hud.mouseClicked(this, mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return hud.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return hud.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void removed() {
		hud.removed();
	}

	@Override
	public void tick() {
		assert client != null;
		client.inGameHud.setOverlayMessage(Text.of(OverlayMessage.EDIT_SAMPLE_MESSAGE), false);
		((InGameHudAccessor) client.inGameHud).setHeldItemTooltipFade(40);
		ItemStack fakeHeldItem = new ItemStack(Items.OAK_PLANKS, 1);
		fakeHeldItem.setCustomName(HeldItemTooltip.EDIT_SAMPLE_MESSAGE);
		((InGameHudAccessor) client.inGameHud).setCurrentStack(fakeHeldItem);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		assert client != null;

//		hud.renderTooltip(this, matrices, mouseX, mouseY);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		matrices.push();
		matrices.translate(0, 0, 100);
		client.textRenderer.drawWithShadow(matrices, "Rearrange elements by holding ctrl and then dragging them around. ESC to close.", 5, 5, 0xffffffff);
		matrices.pop();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (UnofficialMonumentaModClient.openHudEditScreenKeybinding.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		assert client != null;
		client.setScreen(parent);
		client.inGameHud.setOverlayMessage(Text.of(""), false);
		((InGameHudAccessor) client.inGameHud).setHeldItemTooltipFade(0);
		((InGameHudAccessor) client.inGameHud).setCurrentStack(ItemStack.EMPTY);
	}

}
