package dev.neonglass;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class NeonGlassClient implements ClientModInitializer {
    public static final String MOD_ID = "neonglass";
    public static final Config CONFIG = Config.load();
    public static final SpotifyClient SPOTIFY = new SpotifyClient();
    public static final HudRenderer HUD = new HudRenderer();
    private static KeyMapping.Category category;
    private static KeyMapping toggleHud;
    private static KeyMapping openStudio;
    private static KeyMapping previous;
    private static KeyMapping playPause;
    private static KeyMapping next;

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override
    public void onInitializeClient() {
        category = KeyMapping.Category.register(id("main"));
        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonglass.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_P, category));
        openStudio = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonglass.studio", InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT_SHIFT, category));
        previous = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonglass.previous", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));
        playPause = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonglass.play_pause", InputConstants.Type.KEYSYM, InputConstants.KEY_O, category));
        next = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonglass.next", InputConstants.Type.KEYSYM, InputConstants.KEY_L, category));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("music_hud"), HUD::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHud.consumeClick()) { CONFIG.hudEnabled = !CONFIG.hudEnabled; CONFIG.save(); }
            while (openStudio.consumeClick()) client.setScreen(new DesignStudioScreen());
            while (previous.consumeClick()) SPOTIFY.command("previous");
            while (playPause.consumeClick()) SPOTIFY.command(SPOTIFY.track().playing() ? "pause" : "play");
            while (next.consumeClick()) SPOTIFY.command("next");
        });
        SPOTIFY.start();
    }

    public static void status(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("§b[Neon Glass] §f" + message), true);
    }
}
