package ch.njol.unofficialmonumentamod.features.strike;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.Constants;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.options.Options;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.Objects;

public class ChestCountOverlay extends DrawableHelper {
    private static Integer currentCount;//get from actionbar
    private static Integer totalChests;//take from Constants as a HashMap<shard, chestCount> (String, int)
    private static String lastShard;

    public static boolean searchingForShard = false;

    private static final ItemStack CHEST = new ItemStack(Items.CHEST);

    public static boolean shouldRender() {
        return UnofficialMonumentaModClient.options.chestCount_active && totalChests != null && totalChests >= 0;
    }

    public static void testAddToCurrent() {
        //don't add if shouldn't render
        if (shouldRender()) {
            currentCount++;
        }
    }

    protected static boolean shouldRenderDummy = false;

    public static void render(MatrixStack matrices, int scaledWidth, int scaledHeight) {
        render(matrices, scaledWidth, scaledHeight, shouldRenderDummy);
    }

    protected static int width = 64;
    protected static int height = 24;

    private static void render(MatrixStack matrices, int scaledWidth, int scaledHeight, boolean bypass) {
        if (!shouldRender() && !bypass) return;
        final TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        final ItemRenderer Ir = MinecraftClient.getInstance().getItemRenderer();
        final Options options = UnofficialMonumentaModClient.options;

        //8 padding on item (16 + 8 = 24) 40 for text?
        int x = (Math.round(scaledWidth * options.chestCount_offsetXRelative) + options.chestCount_offsetXAbsolute);
        int y = (Math.round(scaledHeight * options.chestCount_offsetYRelative) + options.chestCount_offsetYAbsolute);

        fill(matrices, x, y, x+width, y+height, MinecraftClient.getInstance().options.getTextBackgroundColor(0.3f));
        x+= 8;
        y+= 2;
        Ir.renderGuiItemIcon(CHEST, x, y);
        x+= 16;
        y+= 6;

        Text text;
        if (shouldRenderDummy) {
            text = Text.of("Dummy");
        } else {
            text = Text.of("" + (totalChests != null && totalChests > 0 ? currentCount + "/" + totalChests : currentCount));
        }

        //center text as rest/2 - length of text / 2
        x+= (width - 24) / 2 - tr.getWidth(text) / 2;

        //if total is 0 or the current count is under the total then render in gold else render in bright green
        int color = totalChests != null && totalChests > 0 ? currentCount >= totalChests ? 0xFF1FD655 : 0xFFFCCD12 : 0xFFFCCD12;

        tr.draw(matrices, text, x, y, color);
    }


    public static void onActionbarReceived(Text text) {
        if (text.getString().equals("+1 Chest added to lootroom.")) {
            currentCount++;
        }
    }

    public static void onWorldLoad() {
        searchingForShard = true;
    }

    public static void onPlayerListHeader(Text text) {
        if (!searchingForShard || Locations.getShardFrom(text) == null) return;
        String shard = Locations.getShortShard();
        if (Objects.equals(shard, "unknown")) return;
        if (!Objects.equals(shard, lastShard) &&(Constants.shards.get(shard).shardType == Constants.ShardType.strike)) {//reset
            totalChests = Constants.getMaxChests(shard);//if null then non strike, if 0 then strike but max is unknown, > 0 means it's known so then render the max
            currentCount = 0;
            lastShard = shard;
            searchingForShard = false;
        }
    }
}
