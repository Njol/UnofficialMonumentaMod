package ch.njol.unofficialmonumentamod.core.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;

public abstract class InventoryWidget extends AbstractParentElement implements Drawable, Selectable, Element {
    protected final Screen parentScreen;

    private boolean focused;

    private final List<Element> childrens = new ArrayList<>();
    private final List<Drawable> drawables = new ArrayList<>();
    private final List<Selectable> selectables = new ArrayList<>();
    private Element focusedElement;
    private Selectable selected;
    public InventoryWidget(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public abstract Rectangle getDimension();

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Rectangle dimension = getDimension();
        renderBackground(ctx, dimension);
        render(ctx, dimension, mouseX, mouseY, delta);
        for (Drawable dr : getDrawables()) {
            dr.render(ctx, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Element element : this.children()) {
            if (!element.mouseClicked(mouseX, mouseY, button)) {
                continue;
            }

            this.setFocused(element);
            if (button == 0) {
                this.setDragging(true);
            }
            return true;
        }
        return false;
    }



    @Override public boolean isMouseOver(double mouseX, double mouseY) {
        return getDimension().contains(mouseX, mouseY);
    }

    @Override
    public List<? extends Element> children() {
        return childrens;
    }
    public List<? extends Drawable> getDrawables() {
        return drawables;
    }

    protected void clear() {
        childrens.clear();
        drawables.clear();
        selectables.clear();
    }

    protected <T extends Drawable & Selectable & Element> T addDrawableChild(T child) {
        this.drawables.add(child);
        return addSelectableChild(child);
    }

    protected Drawable addDrawable(Drawable drawable) {
        this.drawables.add(drawable);
        return drawable;
    }

    protected <T extends Element & Selectable> T addSelectableChild(T child) {
        this.selectables.add(child);
        this.childrens.add(child);
        return child;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.FOCUSED;
    }

    public abstract void renderBackground(DrawContext ctx, Rectangle dimension);

    public abstract void render(DrawContext ctx, Rectangle dimension, int mouseX, int mouseY, float delta);
}
