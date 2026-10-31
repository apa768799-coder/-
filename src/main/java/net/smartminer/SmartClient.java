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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartClient implements ClientModInitializer {
    public static KeyBinding guiKey, mineKey, fishKey, afkKey, auraKey, totemKey, xrayKey, espKey, brightKey, tracerKey, glowKey, criticalsKey, trajectoryKey, damageKey, holeKey, trapKey;
    public static Modules modules = new Modules();
    
    public static List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    private static Map<String, Long> moduleActivationTime = new HashMap<>();
    private static long startTime = System.currentTimeMillis();

    // Для Toast-уведомлений
    private static List<Toast> toasts = new ArrayList<>();

    public static void addLog(String message) {
        String time = timeFormat.format(new Date());
        logs.add("[" + time + "] " + message);
        if (logs.size() > 100) logs.remove(0);
    }

    public static void addToast(String message, boolean enabled) {
        toasts.add(new Toast(message, enabled, System.currentTimeMillis()));
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig(modules);

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.smartclient"));
        mineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.mine", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.smartclient"));
        fishKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.fish", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.smartclient"));
        afkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.afk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.smartclient"));
        auraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.aura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.smartclient"));
        totemKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.totem", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "category.smartclient"));
        xrayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.xray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.smartclient"));
        espKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.esp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.smartclient"));
        brightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.bright", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.smartclient"));
        tracerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.tracer", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "category.smartclient"));
        glowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.glow", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.smartclient"));
        criticalsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.criticals", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.smartclient"));
        trajectoryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.trajectory", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.smartclient"));
        damageKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.damage", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.smartclient"));
        holeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.hole", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.smartclient"));
        trapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartclient.trap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.smartclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) { if (client.currentScreen == null) client.setScreen(new ClickGui()); }
            while (mineKey.wasPressed()) { modules.autoMine = !modules.autoMine; msg(client, "AutoMine", modules.autoMine); }
            while (fishKey.wasPressed()) { modules.autoFish = !modules.autoFish; msg(client, "AutoFish", modules.autoFish); }
            while (afkKey.wasPressed()) { modules.antiAfk = !modules.antiAfk; msg(client, "Anti-AFK", modules.antiAfk); }
            while (auraKey.wasPressed()) { modules.killAura = !modules.killAura; msg(client, "KillAura", modules.killAura); }
            while (totemKey.wasPressed()) { modules.autoTotem = !modules.autoTotem; msg(client, "AutoTotem", modules.autoTotem); }
            while (xrayKey.wasPressed()) { modules.xray = !modules.xray; msg(client, "X-Ray", modules.xray); }
            while (espKey.wasPressed()) { modules.playerESP = !modules.playerESP; msg(client, "PlayerESP", modules.playerESP); }
            while (brightKey.wasPressed()) { modules.fullBright = !modules.fullBright; msg(client, "FullBright", modules.fullBright); }
            while (tracerKey.wasPressed()) { modules.tracers = !modules.tracers; msg(client, "Tracers", modules.tracers); }
            while (glowKey.wasPressed()) { modules.glowESP = !modules.glowESP; msg(client, "GlowESP", modules.glowESP); }
            while (criticalsKey.wasPressed()) { modules.criticals = !modules.criticals; msg(client, "Criticals", modules.criticals); }
            while (trajectoryKey.wasPressed()) { modules.trajectory = !modules.trajectory; msg(client, "Trajectory", modules.trajectory); }
            while (damageKey.wasPressed()) { modules.damageNumbers = !modules.damageNumbers; msg(client, "DamageNumbers", modules.damageNumbers); }
            while (holeKey.wasPressed()) { modules.holeESP = !modules.holeESP; msg(client, "HoleESP", modules.holeESP); }
            while (trapKey.wasPressed()) { modules.autoTrap = !modules.autoTrap; msg(client, "AutoTrap", modules.autoTrap); }

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
            
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            String watermark = "§l§bPULSE §fVISUALS §7| §b" + fps + " FPS §7| §e" + timeStr;
            context.drawTextWithShadow(client.textRenderer, watermark, 5, 5, 0xFFFFFF);

            if (modules.lowHp && client.player.getHealth() < 6.0f) {
                float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
                int alpha = (int) (pulse * 100);
                context.fill(0, 0, screenWidth, screenHeight, (alpha << 24) | 0xFF0000);
            }

            // --- TOAST NOTIFICATIONS ---
            toasts.removeIf(t -> System.currentTimeMillis() - t.spawnTime > 3000);
            int toastY = 30;
            for (int i = toasts.size() - 1; i >= 0; i--) {
                Toast t = toasts.get(i);
                long elapsed = System.currentTimeMillis() - t.spawnTime;
                float alpha = 1.0f;
                if (elapsed > 2500) alpha = 1.0f - (elapsed - 2500) / 500.0f; 
                if (elapsed < 200) alpha = elapsed / 200.0f;

                int a = (int)(alpha * 255) << 24;
                int x = screenWidth - 160;
                context.fill(x, toastY, screenWidth - 5, toastY + 20, 0xCC000000);
                context.fill(x, toastY, x + 3, toastY + 20, t.enabled ? ((a | 0x00FF00)) : ((a | 0xFF0000)));
                context.drawTextWithShadow(client.textRenderer, t.message + (t.enabled ? " §aВКЛ" : " §cВЫКЛ"), x + 8, toastY + 6, a | 0xFFFFFF);
                toastY += 24;
            }

            // --- ARMOR HUD (Права снизу) ---
            int armorX = screenWidth - 100;
            int armorY = screenHeight - 60;
            ItemStack[] armorPieces = {
                client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD),
                client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST),
                client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS),
                client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET)
            };
            for (int i = 0; i < 4; i++) {
                ItemStack stack = armorPieces[i];
                if (!stack.isEmpty()) {
                    int dur = stack.getMaxDamage() - stack.getDamage();
                    float pct = (float) dur / stack.getMaxDamage();
                    context.fill(armorX + (i * 18), armorY, armorX + 16 + (i * 18), armorY + 16, 0x80000000);
                    context.drawItem(stack, armorX + (i * 18), armorY);
                    int barColor = pct > 0.5f ? 0xFF00FF00 : (pct > 0.2f ? 0xFFFFFF00 : 0xFFFF0000);
                    context.fill(armorX + (i * 18), armorY + 16, (int)(armorX + (i * 18) + (16 * pct)), armorY + 18, barColor);
                }
            }

            // --- POTION HUD (Лево снизу) ---
            int potionY = screenHeight - 20;
            for (StatusEffectInstance effect : client.player.getStatusEffects()) {
                String name = net.minecraft.text.Text.translatable(effect.getTranslationKey()).getString();
                int duration = effect.getDuration();
                String time = String.format("%d:%02d", duration / 20 / 60, (duration / 20) % 60);
                context.drawTextWithShadow(client.textRenderer, "§f" + name + " §7" + time, 5, potionY, 0xFFFFFF);
                potionY -= 12;
            }

            List<String> activeModules = new ArrayList<>();
            if (modules.autoMine) activeModules.add("AutoMine");
            if (modules.killAura) activeModules.add("KillAura");
            if (modules.autoFish) activeModules.add("AutoFish");
            if (modules.xray) activeModules.add("X-Ray");
            if (modules.playerESP) activeModules.add("PlayerESP");
            if (modules.fullBright) activeModules.add("FullBright");
            if (modules.antiAfk) activeModules.add("Anti-AFK");
            if (modules.autoTotem) activeModules.add("AutoTotem");
            if (modules.itemESP) activeModules.add("ItemESP");
            if (modules.chestESP) activeModules.add("ChestESP");
            if (modules.tracers) activeModules.add("Tracers");
            if (modules.glowESP) activeModules.add("GlowESP");
            if (modules.criticals) activeModules.add("Criticals");
            if (modules.trajectory) activeModules.add("Trajectory");
            if (modules.damageNumbers) activeModules.add("DamageNumbers");
            if (modules.hitParticles) activeModules.add("HitParticles");
            if (modules.hitCounter) activeModules.add("HitCounter");
            if (modules.lowHp) activeModules.add("LowHP");
            if (modules.holeESP) activeModules.add("HoleESP");
            if (modules.autoTrap) activeModules.add("AutoTrap");

            activeModules.sort(Comparator.comparingInt(String::length).reversed());

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
                int hudY = centerY + 40;
                String name = target.getName().getString();
                float healthPercent = Math.max(0, target.getHealth() / target.getMaxHealth());
                float armor = target.getArmor();
                double distance = client.player.distanceTo(target);

                context.fill(centerX - 75, hudY, centerX + 75, hudY + 45, 0xAA000000);
                context.drawBorder(centerX - 75, hudY, 150, 45, 0xFF00FFFF);

                context.drawTextWithShadow(client.textRenderer, name, centerX - 70, hudY + 4, 0xFFFFFFFF);
                context.drawTextWithShadow(client.textRenderer, String.format("%.1f", distance) + "m", centerX + 40, hudY + 4, 0xFFAAAAAA);

                context.fill(centerX - 70, hudY + 18, centerX + 70, hudY + 24, 0xFF222222);
                context.fillGradient(centerX - 70, hudY + 18, (int)(centerX - 70 + (140 * healthPercent)), hudY + 24, 0xFF00FF00, 0xFF00AA00);
                
                context.drawTextWithShadow(client.textRenderer, "🛡 " + (int)armor, centerX - 70, hudY + 30, 0xFFAAAAAA);
            }

            if (modules.damageNumbers) {
                for (Modules.DamageNumber dn : modules.damageNumbersList) {
                    float alpha = 1.0f - (float)(currentTime - dn.spawnTime) / 1000.0f;
                    if (alpha <= 0) continue;
                    int a = (int)(alpha * 255) << 24;
                    int color = dn.isCrit ? (a | 0xFF0000) : (a | 0xFFAA00); 
                    
                    Vec3d pos = dn.pos;
                    int x = (int)(centerX + (pos.x - client.player.getX()) * 50);
                    int yDmg = (int)(centerY + (pos.y - client.player.getY()) * 50);
                    
                    context.drawCenteredTextWithShadow(client.textRenderer, String.valueOf((int)dn.damage), x, yDmg, color);
                }
            }

            if (modules.hitCounter && modules.currentHitCount > 0) {
                context.drawCenteredTextWithShadow(client.textRenderer, "§l§cHIT: " + modules.currentHitCount, centerX, centerY - 80, 0xFFFFFFFF);
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
        addToast(name, state);
        if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§b[Pulse] " + name + ": " + (state ? "§aON" : "§cOFF")), true);
    }

    private static class Toast {
        String message;
        boolean enabled;
        long spawnTime;
        Toast(String message, boolean enabled, long spawnTime) {
            this.message = message;
            this.enabled = enabled;
            this.spawnTime = spawnTime;
        }
    }
}
