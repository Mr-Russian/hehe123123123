package dev.neonglass;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import java.awt.Desktop;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class SpotifyClient {
    private static final String REDIRECT = "http://127.0.0.1:8888/callback";
    private static final String API = "https://api.spotify.com/v1";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "NeonGlass-Spotify"); t.setDaemon(true); return t; });
    private final AtomicReference<Track> current = new AtomicReference<>(Track.empty());
    private volatile String accessToken = "";
    private volatile long expiresAt = 0;
    private volatile long lastPoll = 0;
    private volatile boolean connecting = false;
    private volatile boolean reconnectRequired = false;
    private static final long REFRESH_TOKEN_REAUTH_MS = 175L * 24 * 60 * 60 * 1000;
    private volatile String lastArtworkUrl = "";
    private volatile boolean artworkReady = false;
    private static final Identifier COVER_ID = NeonGlassClient.id("spotify_cover");

    public record Track(String title, String artist, String album, String imageUrl, int durationMs, int progressMs, boolean playing) {
        static Track empty() { return new Track("Spotify not connected", "Open Design Studio to connect", "", "", 0, 0, false); }
    }
    public Track track() { return current.get(); }
    public boolean hasArtwork() { return artworkReady; }

    public void start() {
        worker.scheduleWithFixedDelay(() -> { try { poll(); } catch (Exception ignored) { } }, 1, 2, TimeUnit.SECONDS);
        if (!NeonGlassClient.CONFIG.spotifyRefreshToken.isBlank()) {
            if (NeonGlassClient.CONFIG.spotifyAuthorizedAt > 0 && System.currentTimeMillis() - NeonGlassClient.CONFIG.spotifyAuthorizedAt >= REFRESH_TOKEN_REAUTH_MS) {
                requireReconnect("Spotify authorization is approaching its six-month limit. Reconnect in Design Studio.");
            } else worker.execute(this::refreshToken);
        }
    }

    public void connect() {
        if (connecting) return;
        String clientId = NeonGlassClient.CONFIG.spotifyClientId.trim();
        if (clientId.isEmpty()) { NeonGlassClient.status("Add your Spotify Client ID in config/neonglass-music-hud.json first."); return; }
        connecting = true;
        worker.execute(() -> {
            HttpServer server = null;
            try {
                byte[] random = new byte[48]; new SecureRandom().nextBytes(random);
                String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
                String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
                HttpServer callbackServer = server;
                server.createContext("/callback", exchange -> {
                    String query = exchange.getRequestURI().getRawQuery();
                    String code = param(query, "code");
                    String state = param(query, "state");
                    byte[] body = (code.isEmpty() ? "Spotify authorization failed. You can close this tab." : "Spotify connected! You can close this tab and return to Minecraft.").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
                    if (!code.isEmpty() && state.equals("neonglass")) exchange.getHttpContext().getAttributes().put("auth_code", code);
                });
                server.setExecutor(Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "NeonGlass-OAuth"); t.setDaemon(true); return t; }));
                server.start();
                String scope = "user-read-playback-state user-read-currently-playing user-modify-playback-state";
                String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(clientId) + "&redirect_uri=" + enc(REDIRECT) + "&code_challenge_method=S256&code_challenge=" + enc(challenge) + "&state=neonglass&scope=" + enc(scope);
                Desktop.getDesktop().browse(URI.create(url));
                long end = System.currentTimeMillis() + 180_000;
                String code = "";
                while (System.currentTimeMillis() < end && code.isEmpty()) {
                    Object found = server.getHttpContext("/callback").getAttributes().get("auth_code");
                    if (found != null) code = found.toString();
                    Thread.sleep(250);
                }
                if (code.isEmpty()) throw new IllegalStateException("Spotify login timed out");
                HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("client_id=" + enc(clientId) + "&grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(REDIRECT) + "&code_verifier=" + enc(verifier))).build();
                HttpResponse<String> tokenResponse = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300)
                    throw new IllegalStateException("Spotify token exchange failed (HTTP " + tokenResponse.statusCode() + ")");
                JsonObject token = JsonParser.parseString(tokenResponse.body()).getAsJsonObject();
                accessToken = token.get("access_token").getAsString();
                NeonGlassClient.CONFIG.spotifyRefreshToken = token.get("refresh_token").getAsString();
                NeonGlassClient.CONFIG.spotifyAuthorizedAt = System.currentTimeMillis();
                reconnectRequired = false;
                expiresAt = System.currentTimeMillis() + token.get("expires_in").getAsLong() * 1000;
                NeonGlassClient.CONFIG.save();
                NeonGlassClient.status("Spotify connected.");
                poll();
            } catch (Exception e) { NeonGlassClient.status("Spotify connection failed: " + e.getMessage()); }
            finally { connecting = false; if (server != null) server.stop(0); }
        });
    }

    private void refreshToken() {
        try {
            String form = "client_id=" + enc(NeonGlassClient.CONFIG.spotifyClientId) + "&grant_type=refresh_token&refresh_token=" + enc(NeonGlassClient.CONFIG.spotifyRefreshToken);
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token")).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build();
            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if ((response.statusCode() == 400 || response.statusCode() == 401)
                        && response.body().contains("invalid_grant")) {
                    NeonGlassClient.CONFIG.spotifyRefreshToken = "";
                    NeonGlassClient.CONFIG.spotifyAuthorizedAt = 0L;
                    NeonGlassClient.CONFIG.save();
                    requireReconnect("Spotify authorization expired or was revoked. Choose Connect Spotify to sign in again.");
                }
                accessToken = "";
                return;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            accessToken = json.get("access_token").getAsString();
            if (json.has("refresh_token")) NeonGlassClient.CONFIG.spotifyRefreshToken = json.get("refresh_token").getAsString();
            expiresAt = System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000;
            NeonGlassClient.CONFIG.save();
        } catch (Exception e) { accessToken = ""; }
    }

    private void requireReconnect(String message) {
        accessToken = "";
        reconnectRequired = true;
        current.set(new Track("Spotify reconnect required", message, "", "", 0, 0, false));
        NeonGlassClient.status(message);
    }

    public boolean isConnected() { return !NeonGlassClient.CONFIG.spotifyRefreshToken.isBlank() && !reconnectRequired; }
    public boolean needsReconnect() { return reconnectRequired; }

    public void disconnect() {
        worker.execute(() -> {
            accessToken = ""; expiresAt = 0L; reconnectRequired = false;
            current.set(Track.empty()); lastArtworkUrl = ""; artworkReady = false;
            NeonGlassClient.CONFIG.spotifyRefreshToken = "";
            NeonGlassClient.CONFIG.spotifyAuthorizedAt = 0L;
            NeonGlassClient.CONFIG.save();
            NeonGlassClient.status("Spotify connection removed from this Minecraft installation.");
        });
    }

    private void poll() throws Exception {
        if (accessToken.isBlank()) return;
        if (System.currentTimeMillis() > expiresAt - 60_000) { refreshToken(); if (accessToken.isBlank()) return; }
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + "/me/player")).header("Authorization", "Bearer " + accessToken).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204) { current.set(Track.empty()); return; }
        if (response.statusCode() == 401) { refreshToken(); return; }
        if (response.statusCode() == 429) return;
        if (response.statusCode() != 200) return;
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("item") || root.get("item").isJsonNull()) { current.set(Track.empty()); return; }
        JsonObject item = root.getAsJsonObject("item");
        StringBuilder artists = new StringBuilder();
        item.getAsJsonArray("artists").forEach(a -> { if (artists.length() > 0) artists.append(", "); artists.append(a.getAsJsonObject().get("name").getAsString()); });
        String art = "";
        if (item.has("album")) {
            var images = item.getAsJsonObject("album").getAsJsonArray("images");
            if (images != null && !images.isEmpty()) art = images.get(0).getAsJsonObject().get("url").getAsString();
        }
        current.set(new Track(item.get("name").getAsString(), artists.toString(), item.getAsJsonObject("album").get("name").getAsString(), art,
            item.get("duration_ms").getAsInt(), root.has("progress_ms") && !root.get("progress_ms").isJsonNull() ? root.get("progress_ms").getAsInt() : 0, root.get("is_playing").getAsBoolean()));
        if (!art.isBlank() && !art.equals(lastArtworkUrl)) {
            lastArtworkUrl = art;
            loadArtwork(art);
        }
        lastPoll = System.currentTimeMillis();
    }

    private void loadArtwork(String url) {
        worker.execute(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build();
                byte[] bytes = http.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
                NativeImage image = NativeImage.read(bytes);
                Minecraft.getInstance().execute(() -> {
                    try {
                        DynamicTexture texture = new DynamicTexture(() -> "Neon Glass Spotify album artwork", image);
                        Minecraft.getInstance().getTextureManager().register(COVER_ID, texture);
                        artworkReady = true;
                    } catch (Exception e) { image.close(); }
                });
            } catch (Exception ignored) { }
        });
    }

    public void setVolume(float value) {
        int percent = Math.max(0, Math.min(100, Math.round(value * 100)));
        worker.execute(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(API + "/me/player/volume?volume_percent=" + percent))
                    .header("Authorization", "Bearer " + accessToken).PUT(HttpRequest.BodyPublishers.noBody()).build();
                http.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) { NeonGlassClient.status("Spotify volume control failed: " + e.getMessage()); }
        });
    }

    public void command(String action) {
        worker.execute(() -> {
            try {
                String method = action.equals("play") ? "PUT" : action.equals("pause") ? "PUT" : action.equals("next") || action.equals("previous") ? "POST" : "PUT";
                String endpoint = switch (action) { case "play" -> "/me/player/play"; case "pause" -> "/me/player/pause"; case "next" -> "/me/player/next"; case "previous" -> "/me/player/previous"; default -> ""; };
                if (endpoint.isEmpty()) return;
                HttpRequest req = HttpRequest.newBuilder(URI.create(API + endpoint)).header("Authorization", "Bearer " + accessToken).method(method, HttpRequest.BodyPublishers.noBody()).build();
                http.send(req, HttpResponse.BodyHandlers.discarding());
                Thread.sleep(250); poll();
            } catch (Exception e) { NeonGlassClient.status("Spotify control failed: " + e.getMessage()); }
        });
    }
    private static String param(String query, String name) { if (query == null) return ""; for (String p : query.split("&")) { String[] kv = p.split("=", 2); if (kv.length == 2 && kv[0].equals(name)) return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8); } return ""; }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
