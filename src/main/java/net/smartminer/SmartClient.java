package net.smartminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SmartClient implements ClientModInitializer {
    public static KeyBinding guiKey, mineKey, fishKey, xrayKey, afkKey;
    public static Modules modules = new Modules();

    @Override
    public void onInitializeClient() {
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartclient"));
        mineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.mine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.smartclient"));
        fishKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.fish", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.smartclient"));
        xrayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.xray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.smartclient"));
        afkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.afk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.smartclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) { if (client.currentScreen == null) client.setScreen(new ClickGui()); }
            while (mineKey.wasPressed()) { modules.autoMine = !modules.autoMine; msg(client, "AutoMine", modules.autoMine); }
            while (fishKey.wasPressed()) { modules.autoFish = !modules.autoFish; msg(client, "AutoFish", modules.autoFish); }
            while (xrayKey.wasPressed()) { modules.fullBright = !modules.fullBright; msg(client, "FullBright", modules.fullBright); }
            while (afkKey.wasPressed()) { modules.antiAfk = !modules.antiAfk; msg(client, "Anti-AFK", modules.antiAfk); }

            if (client.player != null && client.world != null && client.interactionManager != null) {
                modules.onTick(client);
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (MinecraftClient.getInstance().world != null && modules.autoMine) {
                modules.renderESP(context);
            }
        });

        // Красивый HUD (ArrayList)
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            
            int screenWidth = client.getWindow().getScaledWidth();
            int y = 5;
            
            // Собираем активные модули
            List<String> activeModules = new ArrayList<>();
            if (modules.autoMine) activeModules.add("AutoMine §7[" + modules.oreNames[modules.targetOreIndex] + "]");
            if (modules.autoFish) activeModules.add("AutoFish");
            if (modules.killAura) activeModules.add("KillAura");
            if (modules.fullBright) activeModules.add("FullBright");
            if (modules.antiAfk) activeModules.add("Anti-AFK");
            
            // Сортируем по длине текста (чтобы было красиво)
            activeModules.sort(Comparator.comparingInt(String::length).reversed());

            // Рисуем водяной знак
            context.drawTextWithShadow(client.textRenderer, "§l§bSMART §fCLIENT §7v12", 5, 5, 0xFFFFFF);

            // Рисуем ArrayList справа
            for (String mod : activeModules) {
                int width = client.textRenderer.getWidth(mod);
                int x = screenWidth - width - 5;
                
                // Задний фон (полупрозрачный черный)
                context.fill(x - 2, y, screenWidth - 2, y + 10, 0x60000000);
                // Неоновая полоска слева
                context.fill(x - 2, y, x - 1, y + 10, 0xFF00FFFF); 
                
                context.drawTextWithShadow(client.textRenderer, mod, x, y + 1, 0xFFFFFF);
                y += 11;
            }
        });
    }

    private void msg(MinecraftClient client, String name, boolean state) {
        if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§b[Smart] " + name + ": " + (state ? "§aON" : "§cOFF")), true);
    }
}
