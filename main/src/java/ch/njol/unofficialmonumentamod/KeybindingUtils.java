package ch.njol.unofficialmonumentamod;

import java.util.Objects;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindingUtils {
    public static boolean isKeyPressed(final KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        }

        if (Objects.equals(KeyBindingHelper.getBoundKeyOf(keyBinding).getCategory(), InputUtil.Type.MOUSE)) {
            return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), getKeyCode(keyBinding)) == 1;
        } else {
            return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), getKeyCode(keyBinding));
        }
    }

    public static int getKeyCode(final KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return -1;
        }

        return KeyBindingHelper.getBoundKeyOf(keyBinding).getCode();
    }
}
