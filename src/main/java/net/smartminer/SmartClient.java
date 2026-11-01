package net.smartminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SmartClient implements ClientModInitializer {

    public static Modules modules = new Modules();
    public static KeyBinding keyGui;
    public static KeyBinding keyAutoMine;
    public static KeyBinding keyXray;
    public static KeyBinding keyKillAura;

    @Override
    public void onInitializeClient() {
        addLog("Загрузка SmartMiner v6.0...");
        modules = new Modules();

        keyGui = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartminer.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartminer"));
        keyAutoMine = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartminer.automine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.smartminer"));
        keyXray = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartminer.xray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.smartminer"));
        keyKillAura = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartminer.killaura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.smartminer"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (keyGui.wasPressed()) client.setScreen(new ClickGui());
            while (keyAutoMine.wasPressed()) {
                modules.autoMine = !modules.autoMine;
                addLog("AutoMine: " + (modules.autoMine ? "§aВКЛ" : "§cВЫКЛ"));
            }
            while (keyXray.wasPressed()) {
                modules.xray = !modules.xray;
                addLog("X-Ray: " + (modules.xray ? "§aВКЛ" : "§cВЫКЛ"));
            }
            while (keyKillAura.wasPressed()) {
                modules.killAura = !modules.killAura;
                addLog("KillAura: " + (modules.killAura ? "§aВКЛ" : "§cВЫКЛ"));
            }

            if (client.world != null) modules.onTick(client);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (modules != null) modules.onChatMessage(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (modules != null) modules.onChatMessage(message.getString());
        });

        WorldRenderEvents.END.register(context -> {
            if (modules != null) modules.renderESP(context);
        });

        // ИСПРАВЛЕНО: Получаем float из RenderTickCounter
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (modules != null) {
                modules.renderHUD(drawContext, tickCounter.getTickDelta(false));
            }
        });

        addLog("Успешно загружен!");
    }

    public static void addLog(String message) {
        System.out.println("[SmartMiner] " + message);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§b[SmartMiner]§r " + message), false);
        }
    }
}
