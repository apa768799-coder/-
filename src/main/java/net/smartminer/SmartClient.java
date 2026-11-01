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
    
    // Дополнительные клавиши (для быстрого доступа, если не хочешь открывать меню)
    public static KeyBinding keyAutoMine;
    public static KeyBinding keyXray;
    public static KeyBinding keyKillAura;

    @Override
    public void onInitializeClient() {
        addLog("Загрузка SmartMiner v6.0 (Delta Edition)...");
        
        // Инициализация модулей
        modules = new Modules();

        // ==========================================
        // 1. РЕГИСТРАЦИЯ КЛАВИШ
        // ==========================================
        keyGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartminer"));
        keyAutoMine = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.automine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.smartminer"));
        keyXray = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.xray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.smartminer"));
        keyKillAura = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.killaura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.smartminer"));

        // ==========================================
        // 2. ОБРАБОТКА КЛАВИШ И ТИКОВ
        // ==========================================
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Открытие GUI на Right Shift
            while (keyGui.wasPressed()) {
                client.setScreen(new ClickGui());
            }

            // Быстрое переключение модулей
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

            // Основной цикл модулей
            if (client.world != null) {
                modules.onTick(client);
            }
        });

        // ==========================================
        // 3. ОБРАБОТКА ЧАТА (Детектор приватов и парсер)
        // ==========================================
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (modules != null) modules.onChatMessage(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (modules != null) modules.onChatMessage(message.getString());
        });

        // ==========================================
        // 4. РЕНДЕР В МИРЕ (ESP, Tracers, X-Ray)
        // ==========================================
        WorldRenderEvents.END.register(context -> {
            if (modules != null) modules.renderESP(context);
        });

        // ==========================================
        // 5. РЕНДЕР НА ЭКРАНЕ (HUD, TargetHUD)
        // ==========================================
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (modules != null) {
                modules.renderHUD(drawContext, tickDelta);
            }
        });

        addLog("Успешно загружен!");
    }

    /**
     * Вывод логов в чат и консоль.
     */
    public static void addLog(String message) {
        System.out.println("[SmartMiner] " + message);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text text = Text.literal("§b[SmartMiner]§r " + message);
            client.player.sendMessage(text, false);
        }
    }
}
