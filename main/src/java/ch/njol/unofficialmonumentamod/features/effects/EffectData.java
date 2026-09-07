package ch.njol.unofficialmonumentamod.features.effects;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public class EffectData {
    private static final HashMap<String, EffectOverride> EFFECTS = new HashMap<>();
    private static final Identifier FILE_IDENTIFIER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "override/effects.json");
    //current format
    //"effect name": {"method": "additive"|"multiplicative"}

    public static void reload() {
        EFFECTS.clear();
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(FILE_IDENTIFIER);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStream stream = resource.get().getInputStream()) {
            HashMap<String, EffectOverride> hash = new GsonBuilder().create().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), new TypeToken<HashMap<String, EffectOverride>>() {
            }.getType());
            if (hash != null) {
                EFFECTS.putAll(hash);
            }
        } catch (IOException | JsonParseException e) {
            UnofficialMonumentaModClient.LOGGER.error("Failed to load effects", e);
        }
    }

    public static String getOverrideMethod(String effectName) {
        EffectOverride effect = getEffectOverride(effectName);
        if (effect == null) {
            return "additive";
        }
        return effect.method;
    }

    public static EffectOverride getEffectOverride(String effectName) {
        return EFFECTS.get(effectName);
    }

    public record EffectOverride(String method) {}
}