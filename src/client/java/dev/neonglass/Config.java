package dev.neonglass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "neonglass-music-hud.json");
    public boolean hudEnabled = true;
    public boolean studioHints = true;
    public int accent = 0xFF27C9FF;
    public int blur = 15;
    public float glassOpacity = 0.15f;
    public float saturation = 1.0f;
    public float brightness = 1.0f;
    public int borderRadius = 30;
    public float borderOpacity = 0.30f;
    public float volume = 0.72f;
    public String spotifyClientId = "";
    public String spotifyRefreshToken = "";
    public long spotifyAuthorizedAt = 0L;
    public int hudScale = 100;

    public static Config load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) { }
        return new Config();
    }
    public synchronized void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(this)); }
        catch (Exception e) { NeonGlassClient.status("Couldn't save settings: " + e.getMessage()); }
    }
}
