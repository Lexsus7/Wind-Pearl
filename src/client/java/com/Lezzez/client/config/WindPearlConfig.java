package com.Lezzez.client.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class WindPearlConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("windpearl.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static WindPearlConfig INSTANCE = new WindPearlConfig();
    public String key = "G";
    public boolean useAnalysis = true;
    public boolean manualAutoThrow = true;
    public static void load() {
        try {
            if (Files.exists(FILE)) {
                WindPearlConfig loaded = GSON.fromJson(Files.readString(FILE), WindPearlConfig.class);
                if (loaded != null) INSTANCE = loaded;
            }
        } catch (Exception ignored) { }
        if (keyCode(INSTANCE.key) == -1) INSTANCE.key = "G";
    }
    public static void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(INSTANCE)); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    public static int keyCode(String name) {
        if (name == null) return -1;
        try {
            int code = GLFW.class.getField("GLFW_KEY_" + name.trim().toUpperCase(Locale.ROOT)).getInt(null);
            return code >= GLFW.GLFW_KEY_SPACE && code <= GLFW.GLFW_KEY_LAST ? code : -1;
        } catch (ReflectiveOperationException e) { return -1; }
    }
}
