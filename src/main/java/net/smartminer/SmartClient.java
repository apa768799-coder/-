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
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class SmartClient implements ClientModInitializer {

    public static Modules modules = new Modules();
    public static String MOD_NAME = "§b[SmartMiner]§r ";
    
    // Привязки клавиш
    public static KeyBinding keyGui;
    public static KeyBinding keyAutoMine;
    public static KeyBinding keyXray;
    public static KeyBinding keyKillAura;
    public static KeyBinding keyScaffold;
    public static KeyBinding keyAutoCrystal;
    public static KeyBinding keySurround;

    @Override
    public void onInitializeClient() {
        addLog("Загрузка клиента " + MOD_NAME + "...");

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
        keyScaffold = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.scaffold", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.smartminer"));
        keyAutoCrystal = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.crystal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.smartminer"));
        keySurround = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smartminer.surround", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.smartminer"));

        // ==========================================
        // 2. ОБРАБОТКА КЛАВИШ (Каждый тик)
        // ==========================================
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Открытие GUI
            while (keyGui.wasPressed()) {
                // Если у тебя есть свой класс ClickGui, раскомментируй и используй его:
                // client.setScreen(new ClickGui());
                client.player.sendMessage(Text.literal(MOD_NAME + "§eМеню (заглушка). Модули переключаются клавишами."), false);
            }

            // Переключение модулей
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
            while (keyScaffold.wasPressed()) {
                modules.scaffold = !modules.scaffold;
                addLog("Scaffold: " + (modules.scaffold ? "§aВКЛ" : "§cВЫКЛ"));
            }
            while (keyAutoCrystal.wasPressed()) {
                modules.autoCrystal = !modules.autoCrystal;
                addLog("AutoCrystal: " + (modules.autoCrystal ? "§aВКЛ" : "§cВЫКЛ"));
            }
            while (keySurround.wasPressed()) {
                modules.surround = !modules.surround;
                addLog("Surround: " + (modules.surround ? "§aВКЛ" : "§cВЫКЛ"));
            }
        });

        // ==========================================
        // 3. ОБРАБОТКА ЧАТА (Детектор приватов)
        // ==========================================
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (modules != null) {
                modules.onChatMessage(message.getString());
            }
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (modules != null) {
                modules.onChatMessage(message.getString());
            }
        });

        // ==========================================
        // 4. ОСНОВНОЙ ТИК (Логика модулей)
        // ==========================================
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                modules.onTick(client);
            }
        });

        // ==========================================
        // 5. РЕНДЕР (ESP, Tracers, X-Ray)
        // ==========================================
        WorldRenderEvents.END.register(context -> {
            if (modules != null) {
                modules.renderESP(context);
            }
        });

        addLog("Успешно загружен!");
    }

    /**
     * Вывод логов в чат и консоль.
     * Используется в Modules.java для отображения текущего состояния (например, "Добываю алмаз").
     */
    public static void addLog(String message) {
        // Вывод в консоль (для отладки)
        System.out.println("[SmartMiner] " + message);

        // Вывод в чат игроку (если он в игре)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Чтобы не спамило слишком сильно, можно фильтровать сообщения
            Text text = Text.literal(MOD_NAME + message).formatted(Formatting.GRAY);
            client.player.sendMessage(text, true); // true = сообщение над хотбаром (action bar)
        }
    }
}
