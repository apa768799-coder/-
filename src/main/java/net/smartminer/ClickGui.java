package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    private final int COLOR_BG_TOP = 0xE6101010;
    private final int COLOR_BG_BOTTOM = 0xE60A0A0A;
    private final int COLOR_SIDEBAR = 0xE6151515;
    private final int COLOR_ACCENT = 0xFF00FFFF;
    private final int COLOR_TEXT = 0xFFFFFFFF;
    private final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    private final int COLOR_HOVER = 0x30FFFFFF;
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;
    private final int COLOR_TOGGLE_OFF = 0xFF333333;

    private int currentTab = 0; // 0=Combat, 1=Movement, 2=Visuals, 3=Player, 4=Config, 5=Logs, 6=XRay

    private int menuX, menuY;
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;

    private final int MENU_WIDTH = 480;
    private final int MENU_HEIGHT = 400;

    public ClickGui() {
        super(Text.literal("Pulse Visuals"));
    }

    @Override
    protected void init() {
        if (menuX == 0 && menuY == 0) {
            menuX = (this.width - MENU_WIDTH) / 2;
            menuY = (this.height - MENU_HEIGHT) / 2;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int x1 = menuX;
        int y1 = menuY;
        int x2 = x1 + MENU_WIDTH;
        int y2 = y1 + MENU_HEIGHT;

        // Тень
        context.fill(x1 + 12, y1 + 12, x2 + 12, y2 + 12, 0x60000000);
        // Градиентный фон
        context.fillGradient(x1, y1, x2, y2, COLOR_BG_TOP, COLOR_BG_BOTTOM);
        // Неоновая рамка
        context.drawBorder(x1, y1, MENU_WIDTH, MENU_HEIGHT, COLOR_ACCENT);

        // Сайдбар
        int catWidth = 140;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR);
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF222222);

        // Логотип
        context.drawCenteredTextWithShadow(this.textRenderer, "§l§bPULSE", x1 + catWidth / 2, y1 + 20, COLOR_ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, "§l§fVISUALS §7v1.0", x1 + catWidth / 2, y1 + 32, COLOR_TEXT);

        // Вкладки
        drawCategory(context, "Combat", 0, y1 + 80, mouseX, mouseY, x1);
        drawCategory(context, "Movement", 1, y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "Visuals", 2, y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", 3, y1 + 155, mouseX, mouseY, x1);
        drawCategory(context, "Config", 4, y1 + 180, mouseX, mouseY, x1);
        drawCategory(context, "Logs", 5, y1 + 205, mouseX, mouseY, x1);
        drawCategory(context, "X-Ray Ores", 6, y1 + 230, mouseX, mouseY, x1);

        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;
        int modStartY = startY + 30;
        int x2Pos = x2;

        if (currentTab == 0) { 
            context.drawText(this.textRenderer, "§l§fБОЙ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            drawModule(context, "KillAura [J]", SmartClient.modules.killAura, startX, modStartY, x2Pos, mouseX, mouseY);
            drawModule(context, "AutoTotem [T]", SmartClient.modules.autoTotem, startX, modStartY + 30, x2Pos, mouseX, mouseY);
            drawModule(context, "Criticals", SmartClient.modules.criticals, startX, modStartY + 60, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 1) { 
            context.drawText(this.textRenderer, "§l§fДВИЖЕНИЕ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            drawModule(context, "AutoMine [R]", SmartClient.modules.autoMine, startX, modStartY, x2Pos, mouseX, mouseY);
            context.drawText(this.textRenderer, "§7Цель:", startX, modStartY + 30, COLOR_TEXT_DIM, false);
            context.drawText(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 35, modStartY + 30, COLOR_TEXT, false);
            boolean hoverLeft = isHovered(mouseX, mouseY, x2Pos - 60, modStartY + 26, 15, 15);
            boolean hoverRight = isHovered(mouseX, mouseY, x2Pos - 40, modStartY + 26, 15, 15);
            context.drawText(this.textRenderer, hoverLeft ? "§a<" : "§7<", x2Pos - 60, modStartY + 30, COLOR_TEXT, false);
            context.drawText(this.textRenderer, hoverRight ? "§a>" : "§7>", x2Pos - 40, modStartY + 30, COLOR_TEXT, false);

            drawModule(context, "AutoFish [F]", SmartClient.modules.autoFish, startX, modStartY + 60, x2Pos, mouseX, mouseY);
            drawModule(context, "Scaffold", SmartClient.modules.scaffold, startX, modStartY + 90, x2Pos, mouseX, mouseY);
            drawModule(context, "NoFall", SmartClient.modules.noFall, startX, modStartY + 120, x2Pos, mouseX, mouseY);
            drawModule(context, "Sprint", SmartClient.modules.sprint, startX, modStartY + 150, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 2) { 
            context.drawText(this.textRenderer, "§l§fВИЗУАЛЫ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            drawModule(context, "FullBright [V]", SmartClient.modules.fullBright, startX, modStartY, x2Pos, mouseX, mouseY);
            drawModule(context, "PlayerESP [P]", SmartClient.modules.playerESP, startX, modStartY + 30, x2Pos, mouseX, mouseY);
            drawModule(context, "X-Ray [X]", SmartClient.modules.xray, startX, modStartY + 60, x2Pos, mouseX, mouseY);
            drawModule(context, "ItemESP", SmartClient.modules.itemESP, startX, modStartY + 90, x2Pos, mouseX, mouseY);
            drawModule(context, "ChestESP", SmartClient.modules.chestESP, startX, modStartY + 120, x2Pos, mouseX, mouseY);
            drawModule(context, "Tracers [L]", SmartClient.modules.tracers, startX, modStartY + 150, x2Pos, mouseX, mouseY);
            drawModule(context, "GlowESP [G]", SmartClient.modules.glowESP, startX, modStartY + 180, x2Pos, mouseX, mouseY);
            drawModule(context, "Trajectory", SmartClient.modules.trajectory, startX, modStartY + 210, x2Pos, mouseX, mouseY);
            drawModule(context, "DamageNumbers", SmartClient.modules.damageNumbers, startX, modStartY + 240, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 3) { 
            context.drawText(this.textRenderer, "§l§fИГРОК", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            drawModule(context, "Anti-AFK [K]", SmartClient.modules.antiAfk, startX, modStartY, x2Pos, mouseX, mouseY);
            drawModule(context, "AutoArmor", SmartClient.modules.autoArmor, startX, modStartY + 30, x2Pos, mouseX, mouseY);
            drawModule(context, "ChestStealer", SmartClient.modules.chestStealer, startX, modStartY + 60, x2Pos, mouseX, mouseY);
            drawModule(context, "Velocity", SmartClient.modules.velocity, startX, modStartY + 90, x2Pos, mouseX, mouseY);
            drawModule(context, "AutoRespawn", SmartClient.modules.autoRespawn, startX, modStartY + 120, x2Pos, mouseX, mouseY);
        }
        else if (currentTab == 4) { 
            context.drawText(this.textRenderer, "§l§fНАСТРОЙКИ (CONFIG)", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            
            boolean hoverSave = isHovered(mouseX, mouseY, startX, modStartY, 200, 20);
            context.fill(startX, modStartY, startX + 200, modStartY + 20, hoverSave ? 0xFF00AA00 : 0xFF005500);
            context.drawCenteredTextWithShadow(this.textRenderer, "§lСохранить конфиг", startX + 100, modStartY + 6, 0xFFFFFF);
            
            boolean hoverLoad = isHovered(mouseX, mouseY, startX, modStartY + 30, 200, 20);
            context.fill(startX, modStartY + 30, startX + 200, modStartY + 50, hoverLoad ? 0xFF0000AA : 0xFF000055);
            context.drawCenteredTextWithShadow(this.textRenderer, "§lЗагрузить конфиг", startX + 100, modStartY + 36, 0xFFFFFF);

            context.drawText(this.textRenderer, "§7Файл: .minecraft/config/smartminer.json", startX, modStartY + 70, COLOR_TEXT_DIM, false);
        }
        else if (currentTab == 5) { 
            context.drawText(this.textRenderer, "§l§fЛОГИ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            int logY = startY + 20;
            int startLogIndex = Math.max(0, SmartClient.logs.size() - 15);
            for (int i = startLogIndex; i < SmartClient.logs.size(); i++) {
                String log = SmartClient.logs.get(i);
                if (log.length() > 60) log = log.substring(0, 57) + "...";
                int color = 0xFFAAAAAA;
                if (log.contains("Найдена")) color = 0xFF00FF00; 
                else if (log.contains("Добываю")) color = 0xFF00FFFF; 
                else if (log.contains("Приват") || log.contains("Опасность")) color = 0xFFFF5555; 
                else if (log.contains("Сбор лута")) color = 0xFFFFAA00; 
                context.drawText(this.textRenderer, log, startX, logY, color, false);
                logY += 12;
            }
        }
        else if (currentTab == 6) { 
            context.drawText(this.textRenderer, "§l§fВЫБОР РУД (X-Ray)", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);
            drawModule(context, "Diamond (Алмаз)", SmartClient.modules.xrayDiamond, startX, modStartY, x2Pos, mouseX, mouseY);
            drawModule(context, "Iron (Железо)", SmartClient.modules.xrayIron, startX, modStartY + 25, x2Pos, mouseX, mouseY);
            drawModule(context, "Gold (Золото)", SmartClient.modules.xrayGold, startX, modStartY + 50, x2Pos, mouseX, mouseY);
            drawModule(context, "Emerald (Изумруд)", SmartClient.modules.xrayEmerald, startX, modStartY + 75, x2Pos, mouseX, mouseY);
            drawModule(context, "Redstone (Редстоун)", SmartClient.modules.xrayRedstone, startX, modStartY + 100, x2Pos, mouseX, mouseY);
            drawModule(context, "Lapis (Лазурит)", SmartClient.modules.xrayLapis, startX, modStartY + 125, x2Pos, mouseX, mouseY);
            drawModule(context, "Coal (Уголь)", SmartClient.modules.xrayCoal, startX, modStartY + 150, x2Pos, mouseX, mouseY);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, "§8Right Shift - закрыть | ЛКМ на шапку - перетащить", x1 + (MENU_WIDTH/2), y2 - 15, 0x555555);
    }

    private void drawCategory(DrawContext context, String name, int tabIndex, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1, y - 5, 140, 20);
        boolean active = (currentTab == tabIndex);
        if (hover || active) {
            context.fill(x1, y - 5, x1 + 140, y + 15, COLOR_HOVER);
            context.fillGradient(x1, y - 5, x1 + 4, y + 15, 0xFF00FFFF, 0xFF0055FF);
        }
        context.drawText(this.textRenderer, (active ? "§b" : "§7") + name, x1 + 15, y, COLOR_TEXT, false);
    }

    private void drawModule(DrawContext context, String name, boolean state, int x, int y, int x2, int mouseX, int mouseY) {
        int toggleW = 30;
        int toggleH = 14;
        int toggleX = x2 - 40 - toggleW; 
        int toggleY = y - 3;
        boolean hoverRow = isHovered(mouseX, mouseY, x, y - 5, (x2 - x - 20), 20);
        if (hoverRow) context.fill(x - 5, y - 5, x2 - 20, y + 15, COLOR_HOVER);
        context.drawText(this.textRenderer, name, x, y, state ? 0xFFFFFFFF : 0xFF888888, false);
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        int circleX = toggleX + (state ? toggleW - 12 : 2);
        context.fill(circleX, toggleY + 2, circleX + 10, toggleY + 12, 0xFFFFFFFF);
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY, menuX, menuY, MENU_WIDTH, 40)) {
            isDragging = true;
            dragOffsetX = (int) (mouseX - menuX);
            dragOffsetY = (int) (mouseY - menuY);
            return true;
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x1 = menuX;
        int y1 = menuY;

        if (isHovered(mouseX, mouseY, x1, y1 + 75, 140, 20)) currentTab = 0;
        if (isHovered(mouseX, mouseY, x1, y1 + 100, 140, 20)) currentTab = 1;
        if (isHovered(mouseX, mouseY, x1, y1 + 125, 140, 20)) currentTab = 2;
        if (isHovered(mouseX, mouseY, x1, y1 + 150, 140, 20)) currentTab = 3;
        if (isHovered(mouseX, mouseY, x1, y1 + 175, 140, 20)) currentTab = 4;
        if (isHovered(mouseX, mouseY, x1, y1 + 200, 140, 20)) currentTab = 5;
        if (isHovered(mouseX, mouseY, x1, y1 + 225, 140, 20)) currentTab = 6;

        int startX = x1 + 140 + 20;
        int modStartY = y1 + 20 + 30;
        int x2 = menuX + MENU_WIDTH;
        int toggleX = x2 - 40 - 30;

        if (currentTab == 0) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 3, 30, 14)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 27, 30, 14)) SmartClient.modules.autoTotem = !SmartClient.modules.autoTotem;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 57, 30, 14)) SmartClient.modules.criticals = !SmartClient.modules.criticals;
        } 
        else if (currentTab == 1) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 3, 30, 14)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 57, 30, 14)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 87, 30, 14)) SmartClient.modules.scaffold = !SmartClient.modules.scaffold;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 117, 30, 14)) SmartClient.modules.noFall = !SmartClient.modules.noFall;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 147, 30, 14)) SmartClient.modules.sprint = !SmartClient.modules.sprint;
            if (isHovered(mouseX, mouseY, x2 - 60, modStartY + 26, 15, 15)) SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
            if (isHovered(mouseX, mouseY, x2 - 40, modStartY + 26, 15, 15)) SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex - 1 + SmartClient.modules.oreNames.length) % SmartClient.modules.oreNames.length;
        }
        else if (currentTab == 2) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 3, 30, 14)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 27, 30, 14)) SmartClient.modules.playerESP = !SmartClient.modules.playerESP;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 57, 30, 14)) SmartClient.modules.xray = !SmartClient.modules.xray;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 87, 30, 14)) SmartClient.modules.itemESP = !SmartClient.modules.itemESP;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 117, 30, 14)) SmartClient.modules.chestESP = !SmartClient.modules.chestESP;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 147, 30, 14)) SmartClient.modules.tracers = !SmartClient.modules.tracers;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 177, 30, 14)) SmartClient.modules.glowESP = !SmartClient.modules.glowESP;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 207, 30, 14)) SmartClient.modules.trajectory = !SmartClient.modules.trajectory;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 237, 30, 14)) SmartClient.modules.damageNumbers = !SmartClient.modules.damageNumbers;
        }
        else if (currentTab == 3) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 3, 30, 14)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 27, 30, 14)) SmartClient.modules.autoArmor = !SmartClient.modules.autoArmor;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 57, 30, 14)) SmartClient.modules.chestStealer = !SmartClient.modules.chestStealer;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 87, 30, 14)) SmartClient.modules.velocity = !SmartClient.modules.velocity;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 117, 30, 14)) SmartClient.modules.autoRespawn = !SmartClient.modules.autoRespawn;
        }
        else if (currentTab == 4) { 
            if (isHovered(mouseX, mouseY, startX, modStartY, 200, 20)) {
                ConfigManager.saveConfig(SmartClient.modules);
            }
            if (isHovered(mouseX, mouseY, startX, modStartY + 30, 200, 20)) {
                ConfigManager.loadConfig(SmartClient.modules);
            }
        }
        else if (currentTab == 6) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 3, 30, 14)) SmartClient.modules.xrayDiamond = !SmartClient.modules.xrayDiamond;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 22, 30, 14)) SmartClient.modules.xrayIron = !SmartClient.modules.xrayIron;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 47, 30, 14)) SmartClient.modules.xrayGold = !SmartClient.modules.xrayGold;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 72, 30, 14)) SmartClient.modules.xrayEmerald = !SmartClient.modules.xrayEmerald;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 97, 30, 14)) SmartClient.modules.xrayRedstone = !SmartClient.modules.xrayRedstone;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 122, 30, 14)) SmartClient.modules.xrayLapis = !SmartClient.modules.xrayLapis;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 147, 30, 14)) SmartClient.modules.xrayCoal = !SmartClient.modules.xrayCoal;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            menuX = (int) (mouseX - dragOffsetX);
            menuY = (int) (mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
