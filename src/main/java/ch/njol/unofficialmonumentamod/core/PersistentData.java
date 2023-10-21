package ch.njol.unofficialmonumentamod.core;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

public class PersistentData {
    private static final String PERSISTENCE_PATH = "monumenta/persistent.json";
    private static PersistentData INSTANCE = new PersistentData();
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    private PersistentData() {

    }

    public static PersistentData getInstance() {
        return INSTANCE;
    }

    private static boolean loaded = false;
    private CompletableFuture<?> IoFuture = null;

    private boolean canNewTaskBeSubmitted() {
        return IoFuture == null || IoFuture.isDone() || IoFuture.isCancelled();
    }

    public boolean initialize() {
        if (loaded || !canNewTaskBeSubmitted()) {
            //if already loaded or doing an IO task with the persistent data.
            return false;
        }

        IoFuture = CompletableFuture.runAsync(PersistentData::load, Util.getIoWorkerExecutor());
        IoFuture.thenRun(() -> loaded = true);

        return true;
    }

    public boolean onLogin() {
        CompletableFuture.runAsync(() -> PersistentDataLoadedCallback.EVENT.invoker().invoke(INSTANCE));
        return true;
    }

    public boolean onDisconnect() {
        if (!loaded || !canNewTaskBeSubmitted()) {
            //if unloaded or already doing an IO task with the persistent data.
            return false;
        }

        PersistentDataSavingCallback.EVENT.invoker().invoke(INSTANCE);
        IoFuture = CompletableFuture.runAsync(PersistentData::save, Util.getIoWorkerExecutor());

        return true;
    }

    private static File getFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(PERSISTENCE_PATH).toFile();
    }

    private static void load() {
        File file = getFile();
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            INSTANCE = GSON.fromJson(reader, PersistentData.class);
        } catch (Exception e) {
            UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to load persistent data", e);
        }
    }

    private static void save() {
        File file = getFile();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to create files for slot locking data", e);
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(GSON.toJson(INSTANCE));
        } catch (Exception e) {
            UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to save slot locking data", e);
        }
    }

    @FunctionalInterface
    public interface PersistentDataLoadedCallback {
        Event<PersistentDataLoadedCallback> EVENT = EventFactory.createArrayBacked(PersistentDataLoadedCallback.class,
                (listeners) -> (persistentData) -> {
                    for (PersistentDataLoadedCallback listener : listeners) {
                        //Invoke all event listeners
                        listener.invoke(persistentData);
                    }
                });

        void invoke(PersistentData data);
    }

    @FunctionalInterface
    public interface PersistentDataSavingCallback {
        Event<PersistentDataSavingCallback> EVENT = EventFactory.createArrayBacked(PersistentDataSavingCallback.class,
                (listeners) -> (persistentData) -> {
                    for (PersistentDataSavingCallback listener: listeners) {
                        //Invoke all event listeners
                        listener.invoke(persistentData);
                    }
                });

        void invoke(PersistentData data);
    }

    //Exposed data
    @Expose
    public DatedHolder<String> delveBounty;
    @Expose
    public DatedHolder<ShardedHolder<Short>> chestCount;

    public abstract static class Holder<T> {
        @Expose
        public T value;

        public boolean isEmpty() {
            return value == null;
        }
    }

    public static class DatedHolder<T> extends Holder<T> {
        @Expose
        public Long time;

        public DatedHolder(Long time, T value) {
            this.time = time;
            this.value = value;
        }

        @Override
        public String toString() {
            Instant time = null;
            if (this.time != null) {
                time = Instant.ofEpochMilli(this.time);
            }

            return "[time: %s, value: %s]".formatted(this.time != null ? Date.from(time) : "unset", value.toString());
        }
    }

    public static class ShardedHolder<T> extends Holder<T> {
        @Expose
        public String shard;

        public ShardedHolder(String shard, T value) {
            this.shard = shard;
            this.value = value;
        }

        @Override
        public String toString() {
            return "[shard: %s, value: %s]".formatted(shard, value.toString());
        }
    }
}
