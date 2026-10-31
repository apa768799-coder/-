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

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            
            int y = 5;
            context.drawTextWithShadow(client.textRenderer, "§l§bSMART §fCLIENT §7v5.0", 5, y, 0xFFFFFF);
            y += 12;
            if (modules.autoMine) { context.drawTextWithShadow(client.textRenderer, "§7AutoMine: §aON §8[" + modules.oreNames[modules.targetOreIndex] + "]", 5, y, 0xFFFFFF); y += 10; }
            if (modules.autoFish) { context.drawTextWithShadow(client.textRenderer, "§7AutoFish: §aON", 5, y, 0xFFFFFF); y += 10; }
            if (modules.killAura) { context.drawTextWithShadow(client.textRenderer, "§7KillAura: §aON", 5, y, 0xFFFFFF); y += 10; }
            if (modules.fullBright) { context.drawTextWithShadow(client.textRenderer, "§7FullBright: §aON", 5, y, 0xFFFFFF); y += 10; }
            if (modules.antiAfk) { context.drawTextWithShadow(client.textRenderer, "§7Anti-AFK: §aON", 5, y, 0xFFFFFF); }
        });
    }

    private void msg(MinecraftClient client, String name, boolean state) {
        if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§b[Smart] " + name + ": " + (state ? "§aON" : "§cOFF")), true);
    }
}
