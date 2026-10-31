package net.smartminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SmartClient implements ClientModInitializer {

    public static Modules modules = new Modules();
    public static KeyBinding keyGui;

    @Override
    public void onInitializeClient() {
        modules = new Modules();
        
        // Открытие GUI на Right Shift
        keyGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartminer"));

        // Основной игровой цикл
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                while (keyGui.wasPressed()) {
                    client.setScreen(new ClickGui());
                }
                modules.onTick(client);
            }
        });

        // Обработка чата (детектор привата)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (modules != null) modules.onChatMessage(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (modules != null) modules.onChatMessage(message.getString());
        });

        // Рендер (ESP, Трассеры)
        WorldRenderEvents.END.register(context -> {
            if (modules != null) modules.renderESP(context);
        });
    }

    public static void addLog(String msg) {
        System.out.println("[SmartMiner] " + msg);
    }
}
