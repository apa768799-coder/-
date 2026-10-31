package net.smartminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SmartClient implements ClientModInitializer {
    public static KeyBinding guiKey;
    public static KeyBinding mineKey;
    public static KeyBinding fishKey;
    public static Modules modules = new Modules();

    @Override
    public void onInitializeClient() {
        // Бинд на открытие меню (Right Shift)
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartclient"
        ));
        // Бинд на Авто-Майн (Клавиша R)
        mineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartclient.mine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.smartclient"
        ));
        // Бинд на Авто-Рыбалку (Клавиша F)
        fishKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartclient.fish", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.smartclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            while (mineKey.wasPressed()) {
                modules.autoMine = !modules.autoMine;
                if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§b[Smart] AutoMine: " + (modules.autoMine ? "§aON" : "§cOFF")), true);
            }
            while (fishKey.wasPressed()) {
                modules.autoFish = !modules.autoFish;
                if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§b[Smart] AutoFish: " + (modules.autoFish ? "§aON" : "§cOFF")), true);
            }

            if (client.player != null && client.world != null && client.interactionManager != null) {
                modules.onTick(client);
            }
        });

        // Визуалы (ESP) - рисуем рамки вокруг руды
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (modules.autoMine && client().world != null) {
                modules.renderESP(context);
            }
        });
    }

    private MinecraftClient client() {
        return MinecraftClient.getInstance();
    }
}
