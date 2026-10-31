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
import java.util.Date;
import java.util.List;

public class SmartClient implements ClientModInitializer {
    // БИНДЫ (Меняй клавиши тут: GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_X и т.д.)
    public static KeyBinding guiKey, mineKey, fishKey, xrayKey, afkKey, auraKey, totemKey, playerEspKey;
    public static Modules modules = new Modules();
    
    public static List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

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
        playerEspKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.esp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.smartclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) { if (client.currentScreen == null) client.setScreen(new ClickGui()); }
            while (mineKey.wasPressed()) { modules.autoMine = !modules.autoMine; msg(client, "AutoMine", modules.autoMine); }
            while (fishKey.wasPressed()) { modules.autoFish = !modules.autoFish; msg(client, "AutoFish", modules.autoFish); }
            while (afkKey.wasPressed()) { modules.antiAfk = !modules.antiAfk; msg(client, "Anti-AFK", modules.antiAfk); }
            while (auraKey.wasPressed()) { modules.killAura = !modules.killAura; msg(client, "KillAura", modules.killAura); }
            while (totemKey.wasPressed()) { modules.autoTotem = !modules.autoTotem; msg(client, "AutoTotem", modules.autoTotem); }
            while (xrayKey.wasPressed()) { modules.xray = !modules.xray; msg(client, "X-Ray", modules.xray); }
            while (playerEspKey.wasPressed()) { modules.playerESP = !modules.playerESP; msg(client, "PlayerESP", modules.playerESP); }

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
            
            context.drawTextWithShadow(client.textRenderer, "§l§bSMART §fCLIENT §7v120", 8, 8, 0xFFFFFF);
            if (modules.autoMine) context.drawTextWithShadow(client.textRenderer, "§7AI: §e" + modules.currentState, 8, 20, 0xFFFFFF);

            if (modules.killAuraTarget != null && modules.killAuraTarget.isAlive()) {
                LivingEntity target = modules.killAuraTarget;
                int cx = client.getWindow().getScaledWidth() / 2;
                int cy = client.getWindow().getScaledHeight() / 2 + 40;
                String name = target.getName().getString();
                float healthPercent = Math.max(0, target.getHealth() / target.getMaxHealth());

                context.fill(cx - 60, cy, cx + 60, cy + 25, 0x80000000);
                context.drawBorder(cx - 60, cy, 120, 25, 0xFF00FFFF);
                context.drawCenteredTextWithShadow(client.textRenderer, name, cx, cy + 2, 0xFFFFFF);
                context.fill(cx - 55, cy + 15, cx + 55, cy + 20, 0xFF333333);
                context.fill(cx - 55, cy + 15, (int)(cx - 55 + (110 * healthPercent)), cy + 20, 0xFF00FF00);
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
