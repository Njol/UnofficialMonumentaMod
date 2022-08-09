package ch.njol.unofficialmonumentamod.misc.managers;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.misc.Locations;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CooldownManager {
    //TODO add a use case to get the cooldown logic for tesseract of light, or not -> because WHY does it have to work differently than all the of other tesseracts combined.
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<Item, CooldownEntry> entries = Maps.newHashMap();
    private static int tick;

    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("^Cooldown : (?<cooldown>[0-9]{0,3})(?<unit>s|m)(?<post>.*)");
    private static final Pattern CHARGES_PATTERN = Pattern.compile("^Charges : (?<charges>[0-9]{0,3})");

    private static final Pattern MAINHAND_PATTERN = Pattern.compile(".*(while in the mainhand|Right Button to).*", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern INVENTORY_PATTERN = Pattern.compile(".*(while in the mainhand or inventory).*", Pattern.MULTILINE | Pattern.DOTALL);

    public static boolean shouldRender() {
        return UnofficialMonumentaModClient.options.renderItemCooldowns;
    }

    public static String getFullTooltip(ItemStack itemStack) {
        StringBuilder fullTooltip = new StringBuilder();
        for (Text text: Utils.getTooltip(itemStack)) {
            fullTooltip.append("\n").append(text.getString());
        }

        return fullTooltip.toString();
    }

    public static void addCooldownToItem(ItemStack itemStack, Trigger trigger) {
        addCooldownToItem(itemStack, getCooldownFromItem(itemStack), trigger);
    }
    public static void addCooldownToItem(ItemStack itemStack, MonumentaCooldownEntry entry, Trigger trigger) {
        if (!shouldRender()) {
            entry = null;
        }
        if (trigger == Trigger.MAIN_HAND) {
            if (!MAINHAND_PATTERN.matcher(getFullTooltip(itemStack)).matches()) return;
        } else if (trigger == Trigger.INVENTORY) {
            if (!INVENTORY_PATTERN.matcher(getFullTooltip(itemStack)).matches()) return;
        }

        if (entry != null) {
            set(itemStack.getItem(), entry);
        } else {
            remove(itemStack.getItem());
        }
    }


    public static MonumentaCooldownEntry getCooldownFromItem(ItemStack itemStack) {
        List<Text> tooltip = itemStack.getTooltip(mc.player, TooltipContext.Default.NORMAL);
        int charges = 1;
        int cooldown = 0;

        for (Text text: tooltip) {
            String line = text.getString();

            Matcher cooldown_matcher = COOLDOWN_PATTERN.matcher(line);
            Matcher charges_matcher = CHARGES_PATTERN.matcher(line);

            if (cooldown_matcher.matches()) {
                int baseCooldown = Integer.parseInt(cooldown_matcher.group("cooldown"));
                baseCooldown = Objects.equals(cooldown_matcher.group("unit"), "m") ? baseCooldown * 60 : baseCooldown;

                //Yellow tesseracts override.
                if (MinecraftClient.getInstance().interactionManager != null && cooldown_matcher.group("post").equals(" when used in Survival zones") && MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.ADVENTURE || Objects.equals(Locations.getShortShard(), "playerplots") || Objects.equals(Locations.getShortShard(), "plots")) return null;

                cooldown = baseCooldown * 20;
            } else if (charges_matcher.matches()) {
                charges = Integer.parseInt(charges_matcher.group("charges"));
            }
        }
        if (cooldown > 0) {
            return new MonumentaCooldownEntry(cooldown, charges);
        }

        return null;
    }

    public static boolean isCoolingDown(Item item) {
        return getCooldownProgress(item, 0.0F) > 0.0F;
    }

    public static float getCooldownProgress(Item item, float partialTicks) {
        CooldownEntry entry = entries.get(item);
        if (entry != null && entry.entries.size() > 0) {
            float f = (float)(entry.entries.get(0).endTick - entry.entries.get(0).startTick);
            float g = (float)entry.entries.get(0).endTick - ((float)tick + partialTicks);
            return MathHelper.clamp(g / f, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public static int getItemCharges(Item item) {
        CooldownEntry entry = entries.get(item);
        if (entry != null) {
            return entry.entries.size();
        } else return 0;
    }

    public static int getMaxItemCharges(Item item) {
        CooldownEntry entry = entries.get(item);
        if (entry != null) {
            return entry.max_charges;
        } else return 0;
    }

    public static void update() {
        ++tick;
        if (!entries.isEmpty()) {
            Iterator<Map.Entry<Item, CooldownEntry>> iterator = entries.entrySet().iterator();
            while(iterator.hasNext()) {
                java.util.Map.Entry<Item, CooldownEntry> entry = iterator.next();
                if (entry.getValue().entries.size() > 1 && entry.getValue().entries.get(0).endTick <= tick ) {
                    entry.getValue().entries.remove(0);
                } else if (entry.getValue().entries.get(0).endTick <= tick && entry.getValue().entries.size() == 1) {
                    iterator.remove();
                }
            }
        }

    }

    public static void set(Item item, MonumentaCooldownEntry entry) {
        if (entries.containsKey(item) && entries.get(item).max_charges > entries.get(item).entries.size()) {
                entries.get(item).entries.add(new Entry(tick, tick + entry.cooldown));
        } else if (!entries.containsKey(item)) entries.put(item, new CooldownEntry(tick, tick + entry.cooldown, entry.charges));
    }

    @Environment(EnvType.CLIENT)
    public static void remove(Item item) {
        entries.remove(item);
    }

    static class Entry {
        private final int startTick;
        private final int endTick;

        private Entry(int startTick, int endTick) {
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }

    static class CooldownEntry {
        final ArrayList<Entry> entries;
        final int max_charges;


        private CooldownEntry(int startTick, int endTick, int charges) {
            this.max_charges = charges;
            this.entries = new ArrayList<>();
            entries.add(new Entry(startTick, endTick));
        }
    }

    public static class MonumentaCooldownEntry {
        private final int cooldown;
        private final int charges;

        private MonumentaCooldownEntry(int cooldown, int charges) {
            this.charges = charges;
            this.cooldown = cooldown;
        }
    }

    public enum Trigger {
        MAIN_HAND(),
        INVENTORY(),
        ;
    }
}
