package net.smartminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SmartClient implements ClientModInitializer {
    public static KeyBinding guiKey;
    public static Modules modules = new Modules();

    @Override
    public void onInitializeClient() {
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGui());
                }
            }
            if (client.currentScreen == null && client.player != null && client.world != null && client.interactionManager != null) {
                modules.onTick(client);
            }
        });
    }
}
