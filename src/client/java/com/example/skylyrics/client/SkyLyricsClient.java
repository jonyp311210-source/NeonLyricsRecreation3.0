package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.MediaSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class SkyLyricsClient implements ClientModInitializer {
    public static final String MOD_ID = "skylyrics";
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        Config.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.skylyrics.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.category.skylyrics.main"
        ));

        MediaSession.start();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                client.setScreen(new SettingsScreen());
            }
            Lyrics.tick();
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            NowPlayingHud.render(graphics, tickCounter);
            LyricsHud.render(graphics, tickCounter);
        });
    }
}
