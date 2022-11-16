package ch.njol.unofficialmonumentamod.features.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class NotificationToast implements Toast {

	Identifier TEXTURE = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/gui/notifications.png");

	private final Text title;

	@Nullable
	private ArrayList<OrderedText> lines;
	@Nullable
	private final Text originalDescription;

	private Toast.Visibility visibility;
	private long hideTime;

	private RenderType renderType;
	private Alignment alignment;

	public NotificationToast(Text title, @Nullable Text description, long timeBeforeRemove) {
		this.visibility = Visibility.SHOW;
		this.renderType = RenderType.RUSTIC;
		this.alignment = Alignment.CENTER;
		this.hideTime = System.currentTimeMillis() + timeBeforeRemove;

		this.title = title;
		this.originalDescription = description;
		this.lines = getTextAsList(originalDescription, this.renderType.offset);
	}

	private static ArrayList<OrderedText> getTextAsList(@Nullable Text text, @Nullable Integer offset) {
		if (text == null) {
			return new ArrayList<>();
		} else {
			ArrayList<OrderedText> list = new ArrayList<>();
			for (String line : text.getString().split("\n")) {
				list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(StringVisitable.plain(line), 160 - (offset != null ? offset : 0)));
			}
			return list;
		}
	}

	private static ArrayList<OrderedText> getWrappedText(@Nullable Text text, @Nullable Integer maxSize) {
		if (text == null) {
			return new ArrayList<>();
		} else {
			ArrayList<OrderedText> list = new ArrayList<>();
			for (String line : text.getString().split("\n")) {
				list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(StringVisitable.plain(line), maxSize != null ? maxSize : 160));
			}
			return list;
		}
	}

	public void wrapDescription() {
		if (this.originalDescription == null) {
			return;
		}
		this.lines = getTextAsList(this.originalDescription, this.renderType.offset);
	}

	public void wrapDescription(int maxSize) {
		if (this.originalDescription == null) {
			return;
		}
		this.lines = getWrappedText(this.originalDescription, maxSize);
	}

	public NotificationToast setToastRender(RenderType type) {
		this.renderType = type;
		wrapDescription();
		return this;
	}

	public NotificationToast setDescriptionAlignment(Alignment alignment) {
		this.alignment = alignment;
		return this;
	}

	public Visibility getVisibility() {
		return this.visibility;
	}

	public Text getTitle() {
		return this.title;
	}

	@Nullable
	public ArrayList<OrderedText> getDescription() {
		return this.lines;
	}

	public void setHideTime(long newValue) {
		this.hideTime = System.currentTimeMillis() + newValue;
	}

	private void bindTexture() {
		RenderSystem.setShaderTexture(0, TEXTURE);
	}

	@Override
	public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
		if (System.currentTimeMillis() < this.hideTime) {
			bindTexture();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			manager.drawTexture(matrices, 0, 0, 0, (this.renderType.type - 1) * 32, this.getWidth(), this.getHeight());

			int i = this.getWidth();
			int o;
			if (this.lines != null) {
				if (i == 160 && this.lines.size() <= 1) {
					manager.drawTexture(matrices, 0, 0, 0, (this.renderType.type - 1) * 32, i, this.getHeight());
				} else {
					o = this.getHeight() + Math.max(0, this.lines.size() - 1) * 12;
					int m = Math.min(4, o - 28);
					this.drawPart(matrices, manager, i, 0, 0, 28);

					for (int n = 28; n < o - m; n += 10) {
						this.drawPart(matrices, manager, i, 16, n, Math.min(16, o - n - m));
					}

					this.drawPart(matrices, manager, i, 32 - m, o - m, m);
				}
			}

			if (this.lines.size() == 0) {
				manager.getClient().textRenderer.drawWithShadow(matrices, this.title, center(manager.getClient().textRenderer.getWidth(this.title)), 7.0F, 0xff500050);
			} else {
				manager.getClient().textRenderer.drawWithShadow(matrices, this.title, center(manager.getClient().textRenderer.getWidth(this.title)), 7.0F, 0xff500050);
				for (o = 0; o < this.lines.size(); ++o) {
					int alignment = 0;

					if (this.alignment == Alignment.CENTER) {
						alignment = center(manager.getClient().textRenderer.getWidth(this.lines.get(o)));
					} else if (this.alignment == Alignment.LEFT) {
						alignment = align_left(manager.getClient().textRenderer.getWidth(this.lines.get(o)));
					} else if (this.alignment == Alignment.RIGHT) {
						alignment = align_right(manager.getClient().textRenderer.getWidth(this.lines.get(o)));
					}

					manager.getClient().textRenderer.drawWithShadow(matrices, this.lines.get(o), alignment, (float) (18 + o * 12), 0xffcccccc);
				}
			}

			return this.visibility;
		} else {
			return this.visibility = Visibility.HIDE;
		}
	}

	private void drawPart(MatrixStack matrices, ToastManager manager, int width, int textureV, int y, int height) {
		int i = textureV == 0 ? 20 : 5;
		int j = Math.min(60, width - i);
		bindTexture();
		manager.drawTexture(matrices, 0, y, 0, (this.renderType.type - 1) * 32 + textureV, i, height);

		for (int k = i; k < width - j; k += 64) {
			manager.drawTexture(matrices, k, y, 32, (this.renderType.type - 1) * 32 + textureV, Math.min(64, width - k - j), height);
		}

		manager.drawTexture(matrices, width - j, y, 160 - j, (this.renderType.type - 1) * 32 + textureV, j, height);
	}

	private int center(int fontWidth) {
		//toasts are 160x32
		int toastWidth = 160;

		if (((toastWidth - fontWidth) / 2) < renderType.offset) {
			//text overlaps with first offset
			wrapDescription(160 - renderType.offset * 2);
			return renderType.offset + ((toastWidth - fontWidth) / 2);
		} else if (((toastWidth - fontWidth) / 2) > toastWidth - renderType.offset) {
			//text overlaps with second offset
			return ((toastWidth - fontWidth) / 2) - renderType.offset;
		} else {
			return ((toastWidth - fontWidth) / 2);
		}
	}

	private int align_right(int fontWidth) {
		if (fontWidth + this.renderType.offset > 160) {
			//should in theory stop overflowing text.
			wrapDescription((160 - renderType.offset) - fontWidth);
		}
		return (fontWidth - this.renderType.offset);
	}

	private int align_left(int fontWidth) {
		return this.renderType.offset;
	}

	public void hide() {
		this.visibility = Visibility.HIDE;
	}

	public enum RenderType {
		ACHIEVEMENT(1, 10),
		RUSTIC(2, 10),
		SYSTEM(3, 20),
		TUTORIAL(4, 10);


		final int type;
		final int offset;

		RenderType(int type, int offset) {
			this.type = type;
			this.offset = offset;
		}
	}

	public enum Alignment {
		LEFT(),
		RIGHT(),
		CENTER()
	}
}

