package ch.njol.unofficialmonumentamod.misc.screen;

import io.github.cottonmc.cotton.gui.client.CottonClientScreen;

public class ItemCustomizationScreen extends CottonClientScreen {
    private final ItemCustomizationGui gui;

    public ItemCustomizationScreen(ItemCustomizationGui description) {
        super(description);
        gui = description;
    }

    @Override
    public void onClose() {
        super.onClose();
        gui.onClose();
    }

    @Override
    public void tick() {
        super.tick();
    }
}
