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
import net.minecraft.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartClient implements ClientModInitializer {
    public static KeyBinding guiKey, mineKey, fishKey, afkKey, auraKey, totemKey, xrayKey, espKey;
    public static Modules modules = new Modules();
    
    public static List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    private static Map<String, Long> moduleActivationTime = new HashMap<>();
    private static long startTime = System.currentTimeMillis();

    public static void addLog(String message) {
        String time = timeFormat.format(new Date());
        logs.add("[" + time + "] " + message);
        if (logs.size() > 100) logs.remove(0);
    }

    @Override
    public void onInitializeClient() {
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartclient"));
        mineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.mine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.smartclient"));
        fishKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.fish", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.smartclient"));
        afkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.afk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.smartclient"));
        auraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.aura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.smartclient"));
        totemKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.totem", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "category.smartclient"));
        xrayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.xray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.smartclient"));
        espKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.esp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.smartclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) { if (client.currentScreen == null) client.setScreen(new ClickGui()); }
            while (mineKey.wasPressed()) { modules.autoMine = !modules.autoMine; msg(client, "AutoMine", modules.autoMine); }
            while (fishKey.wasPressed()) { modules.autoFish = !modules.autoFish; msg(client, "AutoFish", modules.autoFish); }
            while (afkKey.wasPressed()) { modules.antiAfk = !modules.antiAfk; msg(client, "Anti-AFK", modules.antiAfk); }
            while (auraKey.wasPressed()) { modules.killAura = !modules.killAura; msg(client, "KillAura", modules.killAura); }
            while (totemKey.wasPressed()) { modules.autoTotem = !modules.autoTotem; msg(client, "AutoTotem", modules.autoTotem); }
            while (xrayKey.wasPressed()) { modules.xray = !modules.xray; msg(client, "X-Ray", modules.xray); }
            while (espKey.wasPressed()) { modules.playerESP = !modules.playerESP; msg(client, "PlayerESP", modules.playerESP); }

            if (client.player != null && client.world != null && client.interactionManager != null) {
                modules.onTick(client);
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (MinecraftClient.getInstance().world != null) modules.renderESP(context);
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            
            int fps = MinecraftClient.getInstance().getCurrentFps();
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            String timeStr = String.format("%02d:%02d", uptime / 60, uptime % 60);
            
            String watermark = "§lSMART §fCLIENT §7| §b" + fps + " FPS §7| §e" + timeStr;
            context.drawTextWithShadow(client.textRenderer, watermark, 5, 5, 0xFFFFFF);

            List<String> activeModules = new ArrayList<>();
            if (modules.autoMine) activeModules.add("AutoMine");
            if (modules.killAura) activeModules.add("KillAura");
            if (modules.autoFish) activeModules.add("AutoFish");
            if (modules.xray) activeModules.add("X-Ray");
            if (modules.playerESP) activeModules.add("PlayerESP");
            if (modules.antiAfk) activeModules.add("Anti-AFK");
            if (modules.autoTotem) activeModules.add("AutoTotem");

            activeModules.sort(Comparator.comparingInt(String::length).reversed());

            int screenWidth = client.getWindow().getScaledWidth();
            int y = 20;
            long currentTime = System.currentTimeMillis();

            for (String mod : activeModules) {
                if (!moduleActivationTime.containsKey(mod)) moduleActivationTime.put(mod, currentTime);
                long timeEnabled = currentTime - moduleActivationTime.get(mod);
                float alpha = Math.min(1.0f, timeEnabled / 300.0f); 
                
                int width = client.textRenderer.getWidth(mod);
                
                int targetX = screenWidth - width - 5;
                int x = targetX + (int)((1.0f - alpha) * 50); 

                context.fill(x - 2, y, screenWidth - 2, y + 12, 0x90000000);
                context.fillGradient(x - 2, y, x, y + 12, 0xFF00FFFF, 0xFF0055FF); 
                
                context.drawTextWithShadow(client.textRenderer, mod, x, y + 2, 0xFFFFFFFF);
                y += 14;
            }
            moduleActivationTime.keySet().removeIf(key -> !activeModules.contains(key));

            if (modules.killAuraTarget != null && modules.killAuraTarget.isAlive()) {
                LivingEntity target = modules.killAuraTarget;
                int cx = client.getWindow().getScaledWidth() / 2;
                int cy = client.getWindow().getScaledHeight() / 2 + 40;
                String name = target.getName().getString();
                float healthPercent = Math.max(0, target.getHealth() / target.getMaxHealth());

                context.fill(cx - 65, cy, cx + 65, cy + 30, 0xAA000000);
                context.drawBorder(cx - 65, cy, 130, 30, 0xFF00FFFF);

                context.drawCenteredTextWithShadow(client.textRenderer, name, cx, cy + 4, 0xFFFFFF);
                
                context.fill(cx - 60, cy + 18, cx + 60, cy + 24, 0xFF222222);
                context.fillGradient(cx - 60, cy + 18, (int)(cx - 60 + (120 * healthPercent)), cy + 24, 0xFF00FF00, 0xFF00AA00);
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> checkChat(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> checkChat(message.getString()));
    }

    private void checkChat(String text) {
        if (text.contains("Вы не можете ломать блоки") || text.contains("Вы не можете взаимодействовать")) {
            if (modules.autoMine) {
                modules.triggerRetreat();
                addLog("§c⚠ ОБНАРУЖЕН ПРИВАТ! Разворачиваюсь.");
            }
        }
    }

    private void msg(MinecraftClient client, String name, boolean state) {
        addLog(name + ": " + (state ? "ON" : "OFF"));
        if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§b[Smart] " + name + ": " + (state ? "§aON" : "§cOFF")), true);
    }
}
