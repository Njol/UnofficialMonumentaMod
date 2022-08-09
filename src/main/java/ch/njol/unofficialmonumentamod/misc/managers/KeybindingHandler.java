package ch.njol.unofficialmonumentamod.misc.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class KeybindingHandler {
    private static ArrayList<Integer> oldTick = new ArrayList<>();

    public static void tick() {
        ArrayList<Integer> newTick = new ArrayList<>();

        for (int keycode: oldTick) {
            if (isPressed(keycode) && !newTick.contains(keycode)) {
                newTick.add(keycode);
            }
        }
        oldTick = newTick;
    }

    private static boolean wasPressed(int keycode) {
        return oldTick.contains(keycode);
    }
    private static boolean isPressed(int keycode) {
        if (!oldTick.contains(keycode) && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keycode)) {
            oldTick.add(keycode);
        }
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keycode);
    }

    public static class Keybinding {
        private final int keycode;

        public Keybinding(int keycode) {
            this.keycode = keycode;
        }

        public int getKeycode() {
            return keycode;
        }
        public String getKeyName() {
            Text text = new TranslatableText(InputUtil.fromKeyCode(keycode, -1).getTranslationKey());
            if (text.getString().matches(".*\\..*")) {
                return text.getString().split("\\.")[2].toUpperCase();
            } else return text.getString();
        }
        public boolean isPressed() {
            return KeybindingHandler.isPressed(keycode);
        }
        public boolean wasPressed() {
            return KeybindingHandler.wasPressed(keycode);
        }

        public boolean wasReleased() {
            return !isPressed() && wasPressed();
        }

        public boolean wasJustPressed() {
            return isPressed() && !wasPressed();
        }
    }
}
