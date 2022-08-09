package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.nio.file.Files;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Utils {

    private Utils() {
    }

    public static String readFile(String filePath) throws IOException {
        StringBuilder builder = new StringBuilder();

        List<String> list = Files.readAllLines(FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile().toPath());

        list.forEach(s -> builder.append(s).append("\n"));
        return builder.toString();
    }

    /**
     * Gets the plain display name of an items. This is used by Monumenta to distinguish items.
     *
     * @param itemStack An item stack
     * @return The plain display name of the item, i.e. the value of NBT node plain.display.Name.
     */
    public static String getPlainDisplayName(ItemStack itemStack) {
        return itemStack.getTag() == null ? null : itemStack.getTag().getCompound("plain").getCompound("display").getString("Name");
    }

    public static boolean isChestSortDisabledForInventory(ScreenHandler screenHandler, int slotId) {
        if (screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
            return UnofficialMonumentaModClient.options.chestsortDisabledForInventory;
        if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen
                && !(screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
                && ("Ender Chest".equals(MinecraftClient.getInstance().currentScreen.getTitle().getString()) // fake Ender Chest inventory (opened via Remnant)
                || MinecraftClient.getInstance().currentScreen.getTitle() instanceof TranslatableText
                && "container.enderchest".equals(((TranslatableText) MinecraftClient.getInstance().currentScreen.getTitle()).getKey()))) {
            return UnofficialMonumentaModClient.options.chestsortDisabledForEnderchest;
        }
        return UnofficialMonumentaModClient.options.chestsortDisabledEverywhereElse;
    }

    public static float smoothStep(float f) {
        if (f <= 0) return 0;
        if (f >= 1) return 1;
        return f * f * (3 - 2 * f);
    }

    public static class abilitiesDisplay {
        private static final Options options = UnofficialMonumentaModClient.options;
        private static final int iconSize = options.abilitiesDisplay_iconSize;
        private static final int iconGap = options.abilitiesDisplay_iconGap;

        private static final boolean horizontal = options.abilitiesDisplay_horizontal;
        private static final boolean ascendingRenderOrder = options.abilitiesDisplay_ascendingRenderOrder;

        private static final float align = options.abilitiesDisplay_align;

        public static Point getAbilitiesOrigin(List<AbilityHandler.AbilityInfo> abilityInfoList, int width, int height) {
            return  getAbilitiesOrigin(abilityInfoList, width, height, false);
        }

        public static Point getAbilitiesOrigin(List<AbilityHandler.AbilityInfo> abilityInfoList, int width, int height, boolean isAbilityRender) {
            int totalSize = iconSize * abilityInfoList.size() + iconGap * (abilityInfoList.size() - 1);

            int x = Math.round(width * options.abilitiesDisplay_offsetXRelative) + options.abilitiesDisplay_offsetXAbsolute;
            int y = Math.round(height * options.abilitiesDisplay_offsetYRelative) + options.abilitiesDisplay_offsetYAbsolute;

            if (horizontal) {
                x -= align * totalSize;
            } else {
                y -= align * totalSize;
            }
            if (!ascendingRenderOrder && isAbilityRender) {
                if (horizontal) {
                    x += totalSize - iconSize;
                } else {
                    y += totalSize - iconSize;
                }
            }

            return new Point(x, y);
        }

        public static void drawTextureSmooth(MatrixStack matrices, float x, float y, float width, float height) {
            drawTexturedQuadSmooth(matrices.peek().getModel(), x, x + width, y, y + height, 0, 0, 1, 0, 1);
        }

        public static void drawTextureSmooth(MatrixStack matrices, float x, float y, float width, float height, float u0, float u1, float v0, float v1) {
            drawTexturedQuadSmooth(matrices.peek().getModel(), x, x + width, y, y + height, 0, u0, u1, v0, v1);
        }

        private static void drawTexturedQuadSmooth(Matrix4f matrices, float x0, float x1, float y0, float y1, float z, float u0, float u1, float v0, float v1) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
            bufferBuilder.vertex(matrices, x0, y1, z).texture(u0, v1).next();
            bufferBuilder.vertex(matrices, x1, y1, z).texture(u1, v1).next();
            bufferBuilder.vertex(matrices, x1, y0, z).texture(u1, v0).next();
            bufferBuilder.vertex(matrices, x0, y0, z).texture(u0, v0).next();
            bufferBuilder.end();
            RenderSystem.enableAlphaTest();
            BufferRenderer.draw(bufferBuilder);
        }

        public static boolean isHorizontal() {
            return horizontal;
        }

        public static boolean isAscendingOrder() {
            return ascendingRenderOrder;
        }

    }

    public static List<Text> getTooltip(ItemStack stack) {
        return stack.getTooltip(MinecraftClient.getInstance().player, TooltipContext.Default.NORMAL);
    }

    public static String getUrl(@NotNull URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStreamReader streamReader;

        if (connection.getResponseCode() > 299) {
            streamReader = new InputStreamReader(connection.getErrorStream());
        } else {
            streamReader = new InputStreamReader(connection.getInputStream());
        }

        BufferedReader in = new BufferedReader(
                streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();

        return content.toString();
    }

}
