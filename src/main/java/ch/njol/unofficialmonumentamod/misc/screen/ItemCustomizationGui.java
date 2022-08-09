package ch.njol.unofficialmonumentamod.misc.screen;

import ch.njol.unofficialmonumentamod.misc.managers.ItemNameSpoofer;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Objects;

public class ItemCustomizationGui extends LightweightGuiDescription {
    private final WTextField renameField;
    private final ItemStack item;
    private final WButton cheatSheetOpenButton;
    private boolean showText = false;

    private final ArrayList<WLabel> formattedTextList = new ArrayList<>();


    public ItemCustomizationGui(ItemStack itemStack) {
        item = itemStack;
        WGridPanel root = new WGridPanel();
        setRootPanel(root);

        renameField = new WTextField();
        cheatSheetOpenButton = new WButton(Text.of("Open Cheat Sheet"));
        root.setSize(20, 20);

        renameField.setMaxLength(65);
        if (!Objects.equals(ItemNameSpoofer.getSpoofedName(item).getString(), item.getName().getString())) {
            renameField.setText(ItemNameSpoofer.getSpoofedName(item).getString().replaceAll("§", "&&"));
        }
        root.add(renameField, 0, 0, 20, 1);

        WTextLabel label = new WTextLabel(() -> {
            if (!Objects.equals(renameField.getText(), "")) {
                return Text.of(renameField.getText().replaceAll("&&", "§"));
            } else return item.getName();
        });
        root.add(label, 0, 2, 10, 1);

        cheatSheetOpenButton.setOnClick(() -> {
            showText = !showText;
            if (showText) {
                registerText(root);
                root.remove(cheatSheetOpenButton);
            }

        });

        root.add(cheatSheetOpenButton, 15, 1, 5, 1);

        root.validate(this);
    }

    public void registerText(WGridPanel root) {
        formattedTextList.clear();
        String[] formats = new String[]{
                "§0&&0Black",
                "§1&&1Dark Blue",
                "§2&&2Dark Green",
                "§3&&3Dark Aqua",
                "§4&&4Dark Red",
                "§5&&5Dark Purple",
                "§6&&6Gold",
                "§7&&7Gray",
                "§8&&8Dark Gray",
                "§9&&9Blue",
                "§a&&aGreen",
                "§b&&bAqua",
                "§c&&cRed",
                "§d&&dLight Purple",
                "§e&&eYellow§r",
                "§f&&fWhite§r",
                "&&kObfuscate§kd§r",
                "§l&&lBold§r",
                "§m&&mStrikethrough§r",
                "§n&&nUnderline§r",
                "§o&&oItalic§r",
                "§r&&rReset formatting"}
                ;


        int baseX = 0;
        int increment = 5;
        int x = 0;
        int y = 3;
        final int width = 5;
        final int height = 1;




        for (String format: formats) {
            WLabel formatted = new WLabel(format);
            root.add(formatted, x, y, width, height);
            formattedTextList.add(formatted);
            if (x > increment * 3) {
                y++;
                x = baseX;
            } else x=x+increment;
        }

    }

    public void onClose() {
        if (!Objects.equals(renameField.getText(), "")) {
            ItemNameSpoofer.addSpoof(new ItemNameSpoofer.Spoof(ItemNameSpoofer.getUuid(MinecraftClient.getInstance().player.getMainHandStack()), renameField.getText().replaceAll("&&", "§")));

        } else {
            ItemNameSpoofer.remove(ItemNameSpoofer.getUuid(item));
        }
    }
}
