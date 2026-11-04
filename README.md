# Neon Glass Music HUD — Minecraft Java 26.3

A client-side Fabric mod project recreating the two supplied references: a neon cyan/pink floating music HUD and a three-column glassmorphism design studio. Spotify supplies the live track metadata, current progress, album image, and playback controls.

## Target
- Minecraft Java Edition **26.3**
- Fabric Loader **0.20.1+**
- Fabric API **0.165.2+26.3**
- Java **25**

## Controls
- `P` — toggle the floating player on/off.
- `Right Shift` — open the Design Studio.
- `K` / `O` / `L` — previous / play-pause / next Spotify track.
- `K` / `O` / `L` — previous / play-pause / next Spotify track.
- Design Studio buttons — connect or disconnect Spotify, switch HUD visibility, adjust blur/volume, change the accent preset, and save settings. Volume buttons send a Spotify volume command.

## Spotify setup (first-time connection)
Spotify requires an app Client ID for OAuth. The mod does not contain or ship anyone's credentials.

1. Create a personal app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. In the app settings, add this exact Redirect URI: `http://127.0.0.1:8888/callback`
3. Copy the app's **Client ID**.
4. Start Minecraft once, then close it. Open `.minecraft/config/neonglass-music-hud.json` and paste the ID into `spotifyClientId`.
5. Start Minecraft and press `Right Shift`, then **Connect Spotify**. Approve the requested read-playback and playback-control permissions in the browser.
6. The mod saves the refresh token and initial authorization time locally in the same config file. On later Minecraft launches it attempts to refresh the short-lived access token automatically, so you do not need to log in each session.
7. Use **Disconnect** in Design Studio to clear the locally saved refresh token. You can also revoke the app from your Spotify account settings.

**Important:** Spotify authorization is not truly permanent. Spotify may require reauthorization after its refresh-token lifetime or if you revoke access/change account security. The mod flags the connection for reconnection before the six-month boundary and shows a reconnect message if Spotify rejects a refresh token. Reconnect with **Connect Spotify** in Design Studio.

The token is stored in the local Minecraft config file as plaintext in this version. Do not share that file or commit it to GitHub. The repository includes a `.gitignore` rule for the local `config/` folder, but review your Git status before pushing.

The current song, artist, album cover, duration, and progress are refreshed from Spotify every two seconds. Previous, play/pause, and next issue Spotify playback commands. Spotify may require an active Spotify device; playback-control endpoints can also depend on account eligibility and Spotify's current API rules.

## Build
This environment did not have Java 25 or Gradle installed, so a release JAR could not be compiled or smoke-tested here. On a machine with Java 25 and Gradle 9.6+, open a terminal in this folder and run:

```powershell
gradle build
```

The finished JAR will be in `build/libs/`. Install Fabric Loader for Minecraft 26.3, put the JAR in `.minecraft/mods`, and launch the Fabric 26.3 profile.

## Important implementation note
This is a source project, not a precompiled or runtime-tested JAR. Fabric 26.3 uses Java 25 and the new GUI extraction/rendering APIs. Build it with the listed toolchain, then test it in a clean 26.3 Fabric instance before using it in a main world. The HUD is intentionally client-only and does not need server installation.
