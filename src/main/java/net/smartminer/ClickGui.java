package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    private final int COLOR_BG = 0xFF1A1A1A;
    private final int COLOR_SIDEBAR = 0xFF121212;
    private final int COLOR_CARD = 0xFF222222;
    private final int COLOR_CARD_HOVER = 0xFF2A2A2A;
    private final int COLOR_ACCENT = 0xFFFF8C00; // Оранжевый (Pulse)
    private final int COLOR_TEXT = 0xFFFFFFFF;
    private final int COLOR_TEXT_DIM = 0xFF888888;
    private final int COLOR_TOGGLE_OFF = 0xFF444444;

    private int currentTab = 0; // 0=Visuals, 1=Utilities, 2=Combat, 3=Player, 4=Config, 5=Logs

    private int menuX, menuY;
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;

    private final int MENU_WIDTH = 520;
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

        context.fill(x1, y1, x2, y2, COLOR_BG);
        context.drawBorder(x1, y1, MENU_WIDTH, MENU_HEIGHT, 0xFF333333);

        int catWidth = 130;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR);
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF000000);

        context.drawText(this.textRenderer, "§l§fSMART", x1 + 15, y1 + 20, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§l§6CLIENT", x1 + 15, y1 + 32, COLOR_ACCENT, false);

        drawCategory(context, "Visuals", 0, y1 + 70, mouseX, mouseY, x1);
        drawCategory(context, "Utilities", 1, y1 + 100, mouseX, mouseY, x1);
        drawCategory(context, "Combat", 2, y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", 3, y1 + 160, mouseX, mouseY, x1);
        drawCategory(context, "Config", 4, y1 + 190, mouseX, mouseY, x1);
        drawCategory(context, "Logs", 5, y1 + 220, mouseX, mouseY, x1);

        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;

        String title = getCategoryName();
        context.drawText(this.textRenderer, "§l" + title, startX, startY, COLOR_TEXT, false);
        context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333);

        int modStartY = startY + 25;

        if (currentTab == 0) { // VISUALS
            drawCard(context, "FullBright", "Видеть в темноте", SmartClient.modules.fullBright, startX, modStartY, mouseX, mouseY);
            drawCard(context, "X-Ray", "Подсветка руды", SmartClient.modules.xray, startX + 180, modStartY, mouseX, mouseY);
            
            drawCard(context, "PlayerESP", "Скелеты игроков", SmartClient.modules.playerESP, startX, modStartY + 45, mouseX, mouseY);
            drawCard(context, "ItemESP", "Подсветка предметов", SmartClient.modules.itemESP, startX + 180, modStartY + 45, mouseX, mouseY);
            
            drawCard(context, "ChestESP", "Подсветка сундуков", SmartClient.modules.chestESP, startX, modStartY + 90, mouseX, mouseY);
            drawCard(context, "Tracers", "Линии к цели", SmartClient.modules.tracers, startX + 180, modStartY + 90, mouseX, mouseY);
            
            drawCard(context, "GlowESP", "Свечение сущностей", SmartClient.modules.glowESP, startX, modStartY + 135, mouseX, mouseY);
            drawCard(context, "Trajectory", "Траектория стрел", SmartClient.modules.trajectory, startX + 180, modStartY + 135, mouseX, mouseY);
        } 
        else if (currentTab == 1) { // UTILITIES
            drawCard(context, "AutoMine", "Авто-шахта", SmartClient.modules.autoMine, startX, modStartY, mouseX, mouseY);
            drawCard(context, "AutoFish", "Авто-рыбалка", SmartClient.modules.autoFish, startX + 180, modStartY, mouseX, mouseY);
            
            drawCard(context, "Scaffold", "Авто-мост", SmartClient.modules.scaffold, startX, modStartY + 45, mouseX, mouseY);
            drawCard(context, "NoFall", "Без урона от падения", SmartClient.modules.noFall, startX + 180, modStartY + 45, mouseX, mouseY);
            
            drawCard(context, "Sprint", "Вечный бег", SmartClient.modules.sprint, startX, modStartY + 90, mouseX, mouseY);
        } 
        else if (currentTab == 2) { // COMBAT
            drawCard(context, "KillAura", "Атака целей", SmartClient.modules.killAura, startX, modStartY, mouseX, mouseY);
            drawCard(context, "AutoTotem", "Авто-тотем", SmartClient.modules.autoTotem, startX + 180, modStartY, mouseX, mouseY);
            
            drawCard(context, "Criticals", "Критические удары", SmartClient.modules.criticals, startX, modStartY + 45, mouseX, mouseY);
            drawCard(context, "HitParticles", "Частицы при ударе", SmartClient.modules.hitParticles, startX + 180, modStartY + 45, mouseX, mouseY);
            
            drawCard(context, "DamageNumbers", "Числа урона", SmartClient.modules.damageNumbers, startX, modStartY + 90, mouseX, mouseY);
            drawCard(context, "HitCounter", "Счетчик серии", SmartClient.modules.hitCounter, startX + 180, modStartY + 90, mouseX, mouseY);
        } 
        else if (currentTab == 3) { // PLAYER
            drawCard(context, "Anti-AFK", "Защита от кика", SmartClient.modules.antiAfk, startX, modStartY, mouseX, mouseY);
            drawCard(context, "AutoArmor", "Авто-броня", SmartClient.modules.autoArmor, startX + 180, modStartY, mouseX, mouseY);
            
            drawCard(context, "ChestStealer", "Авто-лут", SmartClient.modules.chestStealer, startX, modStartY + 45, mouseX, mouseY);
            drawCard(context, "Velocity", "Анти-кнокбек", SmartClient.modules.velocity, startX + 180, modStartY + 45, mouseX, mouseY);
            
            drawCard(context, "AutoRespawn", "Авто-возрождение", SmartClient.modules.autoRespawn, startX, modStartY + 90, mouseX, mouseY);
            drawCard(context, "LowHP", "Пульсация при HP<3", SmartClient.modules.lowHp, startX + 180, modStartY + 90, mouseX, mouseY);
        }
        else if (currentTab == 4) { // CONFIG
            drawButton(context, "Сохранить конфиг", startX, modStartY, 180, 20, mouseX, mouseY, COLOR_ACCENT);
            drawButton(context, "Загрузить конфиг", startX, modStartY + 30, 180, 20, mouseX, mouseY, 0xFF0055FF);
            context.drawText(this.textRenderer, "§7Файл: .minecraft/config/smartminer.json", startX, modStartY + 70, COLOR_TEXT_DIM, false);
        }
        else if (currentTab == 5) { // LOGS
            int logY = modStartY;
            int startLogIndex = Math.max(0, SmartClient.logs.size() - 12);
            for (int i = startLogIndex; i < SmartClient.logs.size(); i++) {
                String log = SmartClient.logs.get(i);
                if (log.length() > 65) log = log.substring(0, 62) + "...";
                int color = 0xFFAAAAAA;
                if (log.contains("Найдена")) color = 0xFF00FF00; 
                else if (log.contains("Приват") || log.contains("Опасность")) color = 0xFFFF5555; 
                context.drawText(this.textRenderer, log, startX, logY, color, false);
                logY += 12;
            }
        }

        context.drawCenteredTextWithShadow(this.textRenderer, "§8Right Shift - закрыть | Перетаскивание за шапку", x1 + (MENU_WIDTH/2), y2 - 15, 0x555555);
    }

    private String getCategoryName() {
        return switch (currentTab) {
            case 0 -> "ВИЗУАЛЫ (Visuals)";
            case 1 -> "УТИЛИТЫ (Utilities)";
            case 2 -> "БОЙ (Combat)";
            case 3 -> "ИГРОК (Player)";
            case 4 -> "НАСТРОЙКИ (Config)";
            case 5 -> "ЛОГИ (Logs)";
            default -> "";
        };
    }

    private void drawCategory(DrawContext context, String name, int tabIndex, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1, y - 5, 130, 20);
        boolean active = (currentTab == tabIndex);

        if (active) {
            context.fill(x1 + 5, y - 5, x1 + 125, y + 15, COLOR_ACCENT);
            context.drawText(this.textRenderer, name, x1 + 15, y, 0xFF000000, false);
        } else if (hover) {
            context.fill(x1 + 5, y - 5, x1 + 125, y + 15, 0x30FFFFFF);
            context.drawText(this.textRenderer, name, x1 + 15, y, COLOR_TEXT, false);
        } else {
            context.drawText(this.textRenderer, name, x1 + 15, y, COLOR_TEXT_DIM, false);
        }
    }

    private void drawCard(DrawContext context, String name, String desc, boolean state, int x, int y, int mouseX, int mouseY) {
        int width = 170;
        int height = 40;
        boolean hover = isHovered(mouseX, mouseY, x, y, width, height);

        context.fill(x, y, x + width, y + height, hover ? COLOR_CARD_HOVER : COLOR_CARD);
        context.drawBorder(x, y, width, height, 0xFF333333);

        context.drawText(this.textRenderer, "§l" + name, x + 8, y + 6, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§7" + desc, x + 8, y + 18, COLOR_TEXT_DIM, false);

        int toggleX = x + width - 35;
        int toggleY = y + 12;
        int toggleW = 26;
        int toggleH = 14;

        if (state) {
            context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, COLOR_ACCENT);
            context.fill(toggleX + 14, toggleY + 2, toggleX + 24, toggleY + 12, 0xFFFFFFFF);
        } else {
            context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, COLOR_TOGGLE_OFF);
            context.fill(toggleX + 2, toggleY + 2, toggleX + 12, toggleY + 12, 0xFFFFFFFF);
        }
    }

    private void drawButton(DrawContext context, String text, int x, int y, int w, int h, int mouseX, int mouseY, int color) {
        boolean hover = isHovered(mouseX, mouseY, x, y, w, h);
        context.fill(x, y, x + w, y + h, hover ? color : 0xFF222222);
        context.drawBorder(x, y, w, h, 0xFF333333);
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY, menuX, menuY, MENU_WIDTH, 50)) {
            isDragging = true;
            dragOffsetX = (int) (mouseX - menuX);
            dragOffsetY = (int) (mouseY - menuY);
            return true;
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x1 = menuX;
        int y1 = menuY;

        if (isHovered(mouseX, mouseY, x1, y1 + 65, 130, 20)) currentTab = 0;
        if (isHovered(mouseX, mouseY, x1, y1 + 95, 130, 20)) currentTab = 1;
        if (isHovered(mouseX, mouseY, x1, y1 + 125, 130, 20)) currentTab = 2;
        if (isHovered(mouseX, mouseY, x1, y1 + 155, 130, 20)) currentTab = 3;
        if (isHovered(mouseX, mouseY, x1, y1 + 185, 130, 20)) currentTab = 4;
        if (isHovered(mouseX, mouseY, x1, y1 + 215, 130, 20)) currentTab = 5;

        int startX = x1 + 130 + 20;
        int modStartY = y1 + 20 + 25;
        
        if (currentTab == 0) { // Visuals
            if (isHovered(mouseX, mouseY, startX, modStartY, 170, 40)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY, 170, 40)) SmartClient.modules.xray = !SmartClient.modules.xray;
            if (isHovered(mouseX, mouseY, startX, modStartY + 45, 170, 40)) SmartClient.modules.playerESP = !SmartClient.modules.playerESP;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 45, 170, 40)) SmartClient.modules.itemESP = !SmartClient.modules.itemESP;
            if (isHovered(mouseX, mouseY, startX, modStartY + 90, 170, 40)) SmartClient.modules.chestESP = !SmartClient.modules.chestESP;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 90, 170, 40)) SmartClient.modules.tracers = !SmartClient.modules.tracers;
            if (isHovered(mouseX, mouseY, startX, modStartY + 135, 170, 40)) SmartClient.modules.glowESP = !SmartClient.modules.glowESP;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 135, 170, 40)) SmartClient.modules.trajectory = !SmartClient.modules.trajectory;
        } 
        else if (currentTab == 1) { // Utilities
            if (isHovered(mouseX, mouseY, startX, modStartY, 170, 40)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY, 170, 40)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            if (isHovered(mouseX, mouseY, startX, modStartY + 45, 170, 40)) SmartClient.modules.scaffold = !SmartClient.modules.scaffold;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 45, 170, 40)) SmartClient.modules.noFall = !SmartClient.modules.noFall;
            if (isHovered(mouseX, mouseY, startX, modStartY + 90, 170, 40)) SmartClient.modules.sprint = !SmartClient.modules.sprint;
        }
        else if (currentTab == 2) { // Combat
            if (isHovered(mouseX, mouseY, startX, modStartY, 170, 40)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY, 170, 40)) SmartClient.modules.autoTotem = !SmartClient.modules.autoTotem;
            if (isHovered(mouseX, mouseY, startX, modStartY + 45, 170, 40)) SmartClient.modules.criticals = !SmartClient.modules.criticals;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 45, 170, 40)) SmartClient.modules.hitParticles = !SmartClient.modules.hitParticles;
            if (isHovered(mouseX, mouseY, startX, modStartY + 90, 170, 40)) SmartClient.modules.damageNumbers = !SmartClient.modules.damageNumbers;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 90, 170, 40)) SmartClient.modules.hitCounter = !SmartClient.modules.hitCounter;
        }
        else if (currentTab == 3) { // Player
            if (isHovered(mouseX, mouseY, startX, modStartY, 170, 40)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY, 170, 40)) SmartClient.modules.autoArmor = !SmartClient.modules.autoArmor;
            if (isHovered(mouseX, mouseY, startX, modStartY + 45, 170, 40)) SmartClient.modules.chestStealer = !SmartClient.modules.chestStealer;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 45, 170, 40)) SmartClient.modules.velocity = !SmartClient.modules.velocity;
            if (isHovered(mouseX, mouseY, startX, modStartY + 90, 170, 40)) SmartClient.modules.autoRespawn = !SmartClient.modules.autoRespawn;
            if (isHovered(mouseX, mouseY, startX + 180, modStartY + 90, 170, 40)) SmartClient.modules.lowHp = !SmartClient.modules.lowHp;
        }
        else if (currentTab == 4) { // Config
            if (isHovered(mouseX, mouseY, startX, modStartY, 180, 20)) ConfigManager.saveConfig(SmartClient.modules);
            if (isHovered(mouseX, mouseY, startX, modStartY + 30, 180, 20)) ConfigManager.loadConfig(SmartClient.modules);
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
