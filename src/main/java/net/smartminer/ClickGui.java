package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    // --- ЦВЕТА (Pulse Visuals Style) ---
    private final int COLOR_BG = 0xF2141414;          // Полупрозрачный темный фон
    private final int COLOR_SIDEBAR = 0xFF0A0A0A;     // Почти черная панель
    private final int COLOR_CARD = 0xFF1A1A1A;        // Карточка модуля
    private final int COLOR_CARD_HOVER = 0xFF252525;  // Карточка при наведении
    private final int COLOR_ACCENT_LIGHT = 0xFFFFA500;// Светло-оранжевый
    private final int COLOR_ACCENT_DARK = 0xFFCC6600; // Темно-оранжевый
    private final int COLOR_TEXT = 0xFFFFFFFF;        // Белый
    private final int COLOR_TEXT_DIM = 0xFF888888;    // Серый
    private final int COLOR_BORDER = 0xFF2A2A2A;      // Рамка карточек
    private final int COLOR_TOGGLE_OFF = 0xFF333333;  // Выключенный тумблер

    private int currentTab = 0; // 0=Visuals, 1=Combat, 2=Movement, 3=Player, 4=Config, 5=Logs
    private int menuX, menuY;
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;

    // Увеличили размер для идеальной сетки
    private final int MENU_WIDTH = 580;
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
        // Затемнение и размытие фона игры
        this.renderBackground(context, mouseX, mouseY, delta);

        int x1 = menuX;
        int y1 = menuY;
        int x2 = x1 + MENU_WIDTH;
        int y2 = y1 + MENU_HEIGHT;

        // 1. Глубокая тень под меню
        context.fill(x1 + 10, y1 + 10, x2 + 10, y2 + 10, 0x60000000);
        
        // 2. Основной фон (Стекло)
        context.fill(x1, y1, x2, y2, COLOR_BG);
        
        // 3. Тонкая неоновая рамка вокруг всего меню
        context.drawBorder(x1, y1, MENU_WIDTH, MENU_HEIGHT, 0xFF333333);
        context.drawBorder(x1 - 1, y1 - 1, MENU_WIDTH + 2, MENU_HEIGHT + 2, 0x40FFA500); 

        // 4. Сайдбар (Левая панель)
        int catWidth = 140;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR);
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, COLOR_BORDER); // Разделитель

        // 5. Логотип
        context.drawText(this.textRenderer, "§l§fPULSE", x1 + 15, y1 + 20, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§l§6VISUALS", x1 + 15, y1 + 32, 0xFFFFA500, false);
        context.drawText(this.textRenderer, "§8v3.0", x1 + 105, y1 + 32, 0xFF555555, false);

        // 6. Вкладки
        drawCategory(context, "VISUALS", 0, y1 + 75, mouseX, mouseY, x1);
        drawCategory(context, "COMBAT", 1, y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "MOVEMENT", 2, y1 + 135, mouseX, mouseY, x1);
        drawCategory(context, "PLAYER", 3, y1 + 165, mouseX, mouseY, x1);
        drawCategory(context, "CONFIG", 4, y1 + 195, mouseX, mouseY, x1);
        drawCategory(context, "LOGS", 5, y1 + 225, mouseX, mouseY, x1);

        // 7. Правая панель (Контент)
        int startX = x1 + catWidth + 25;
        int startY = y1 + 20;
        int x2Pos = x2 - 25;

        // Заголовок категории и декоративные кнопки
        context.drawText(this.textRenderer, "§l" + getCategoryName(), startX, startY, COLOR_TEXT, false);
        
        // Кнопка закрытия [X]
        boolean closeHover = isHovered(mouseX, mouseY, x2 - 35, y1 + 15, 15, 15);
        context.drawText(this.textRenderer, closeHover ? "§c[X]" : "§7[X]", x2 - 35, y1 + 15, 0xFFFFFFFF, false);
        
        // Поле поиска (декоративное)
        context.fill(startX, startY + 15, x2Pos, startY + 35, 0xFF0A0A0A);
        drawRoundedBorder(context, startX, startY + 15, x2Pos - startX, 20, 4, COLOR_BORDER);
        context.drawText(this.textRenderer, "§8Поиск...", startX + 8, startY + 21, 0xFF555555, false);

        int modStartY = startY + 50;

        // 8. Отрисовка модулей (Сетка)
        if (currentTab == 0) { // VISUALS
            drawCard(context, "FullBright", "Видеть в темноте", SmartClient.modules.fullBright, startX, modStartY, mouseX, mouseY);
            drawCard(context, "X-Ray", "Подсветка руды", SmartClient.modules.xray, startX + 195, modStartY, mouseX, mouseY);
            
            drawCard(context, "PlayerESP", "Скелеты игроков", SmartClient.modules.playerESP, startX, modStartY + 50, mouseX, mouseY);
            drawCard(context, "ItemESP", "Подсветка предметов", SmartClient.modules.itemESP, startX + 195, modStartY + 50, mouseX, mouseY);
            
            drawCard(context, "ChestESP", "Подсветка сундуков", SmartClient.modules.chestESP, startX, modStartY + 100, mouseX, mouseY);
            drawCard(context, "Tracers", "Линии к цели", SmartClient.modules.tracers, startX + 195, modStartY + 100, mouseX, mouseY);
            
            drawCard(context, "GlowESP", "Свечение сущностей", SmartClient.modules.glowESP, startX, modStartY + 150, mouseX, mouseY);
            drawCard(context, "HoleESP", "Безопасные дырки", SmartClient.modules.holeESP, startX + 195, modStartY + 150, mouseX, mouseY);
        } 
        else if (currentTab == 1) { // COMBAT
            drawCard(context, "KillAura", "Атака целей", SmartClient.modules.killAura, startX, modStartY, mouseX, mouseY);
            drawCard(context, "AutoTotem", "Авто-тотем", SmartClient.modules.autoTotem, startX + 195, modStartY, mouseX, mouseY);
            
            drawCard(context, "Criticals", "Критические удары", SmartClient.modules.criticals, startX, modStartY + 50, mouseX, mouseY);
            drawCard(context, "AutoTrap", "Трапка обсидианом", SmartClient.modules.autoTrap, startX + 195, modStartY + 50, mouseX, mouseY);
            
            drawCard(context, "DamageNumbers", "Числа урона", SmartClient.modules.damageNumbers, startX, modStartY + 100, mouseX, mouseY);
            drawCard(context, "HitCounter", "Счетчик серии", SmartClient.modules.hitCounter, startX + 195, modStartY + 100, mouseX, mouseY);
        } 
        else if (currentTab == 2) { // MOVEMENT
            drawCard(context, "AutoMine", "Авто-шахта", SmartClient.modules.autoMine, startX, modStartY, mouseX, mouseY);
            drawCard(context, "AutoFish", "Авто-рыбалка", SmartClient.modules.autoFish, startX + 195, modStartY, mouseX, mouseY);
            
            drawCard(context, "Scaffold", "Авто-мост", SmartClient.modules.scaffold, startX, modStartY + 50, mouseX, mouseY);
            drawCard(context, "NoFall", "Без урона от падения", SmartClient.modules.noFall, startX + 195, modStartY + 50, mouseX, mouseY);
            
            drawCard(context, "Sprint", "Вечный бег", SmartClient.modules.sprint, startX, modStartY + 100, mouseX, mouseY);
        }
        else if (currentTab == 3) { // PLAYER
            drawCard(context, "Anti-AFK", "Защита от кика", SmartClient.modules.antiAfk, startX, modStartY, mouseX, mouseY);
            drawCard(context, "AutoArmor", "Авто-броня", SmartClient.modules.autoArmor, startX + 195, modStartY, mouseX, mouseY);
            
            drawCard(context, "ChestStealer", "Авто-лут", SmartClient.modules.chestStealer, startX, modStartY + 50, mouseX, mouseY);
            drawCard(context, "Velocity", "Анти-кнокбек", SmartClient.modules.velocity, startX + 195, modStartY + 50, mouseX, mouseY);
            
            drawCard(context, "AutoRespawn", "Авто-возрождение", SmartClient.modules.autoRespawn, startX, modStartY + 100, mouseX, mouseY);
            drawCard(context, "LowHP", "Пульсация при HP<3", SmartClient.modules.lowHp, startX + 195, modStartY + 100, mouseX, mouseY);
        }
        else if (currentTab == 4) { // CONFIG
            drawButton(context, "Сохранить конфиг", startX, modStartY, 200, 24, mouseX, mouseY, 0xFF00AA00);
            drawButton(context, "Загрузить конфиг", startX, modStartY + 35, 200, 24, mouseX, mouseY, 0xFF0055FF);
            context.drawText(this.textRenderer, "§7Файл: .minecraft/config/smartminer.json", startX, modStartY + 70, COLOR_TEXT_DIM, false);
        }
        else if (currentTab == 5) { // LOGS
            int logY = modStartY;
            int startLogIndex = Math.max(0, SmartClient.logs.size() - 12);
            for (int i = startLogIndex; i < SmartClient.logs.size(); i++) {
                String log = SmartClient.logs.get(i);
                if (log.length() > 70) log = log.substring(0, 67) + "...";
                int color = 0xFFAAAAAA;
                if (log.contains("Найдена")) color = 0xFF00FF00; 
                else if (log.contains("Приват") || log.contains("Опасность")) color = 0xFFFF5555; 
                context.drawText(this.textRenderer, log, startX, logY, color, false);
                logY += 12;
            }
        }

        // Футер
        context.drawCenteredTextWithShadow(this.textRenderer, "§8Right Shift - закрыть | ЛКМ на шапку - перетащить", x1 + (MENU_WIDTH/2), y2 - 15, 0x555555);
    }

    private String getCategoryName() {
        return switch (currentTab) {
            case 0 -> "ВИЗУАЛЫ";
            case 1 -> "БОЙ";
            case 2 -> "ДВИЖЕНИЕ";
            case 3 -> "ИГРОК";
            case 4 -> "КОНФИГ";
            case 5 -> "ЛОГИ";
            default -> "";
        };
    }

    // Красивая отрисовка вкладки с градиентом и полоской
    private void drawCategory(DrawContext context, String name, int tabIndex, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1 + 5, y - 5, 130, 20);
        boolean active = (currentTab == tabIndex);

        if (active) {
            // Темно-оранжевый градиентный фон
            context.fillGradient(x1 + 5, y - 5, x1 + 135, y + 15, 0x40FFA500, 0x00FFA500);
            // Яркая полоска слева
            context.fill(x1 + 5, y - 5, x1 + 8, y + 15, 0xFFFFA500); 
            context.drawText(this.textRenderer, name, x1 + 18, y, 0xFFFFA500, false);
        } else if (hover) {
            context.fill(x1 + 5, y - 5, x1 + 135, y + 15, 0x15FFFFFF); // Легкая белая подсветка
            context.drawText(this.textRenderer, name, x1 + 18, y, COLOR_TEXT, false);
        } else {
            context.drawText(this.textRenderer, name, x1 + 18, y, COLOR_TEXT_DIM, false);
        }
    }

    // Карточка модуля с красивым тумблером
    private void drawCard(DrawContext context, String name, String desc, boolean state, int x, int y, int mouseX, int mouseY) {
        int width = 180;
        int height = 42;
        boolean hover = isHovered(mouseX, mouseY, x, y, width, height);

        // Фон карточки
        context.fill(x, y, x + width, y + height, hover ? COLOR_CARD_HOVER : COLOR_CARD);
        drawRoundedBorder(context, x, y, width, height, 4, hover ? 0xFF444444 : COLOR_BORDER);

        // Название и описание
        context.drawText(this.textRenderer, "§l" + name, x + 10, y + 8, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§7" + desc, x + 10, y + 20, COLOR_TEXT_DIM, false);

        // Тумблер (Toggle Pill)
        int toggleX = x + width - 40;
        int toggleY = y + 14;
        int toggleW = 30;
        int toggleH = 14;

        if (state) {
            // Включен (Оранжевый градиент)
            context.fillGradient(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, COLOR_ACCENT_LIGHT, COLOR_ACCENT_DARK);
            // Белый кружок справа
            context.fill(toggleX + 16, toggleY + 2, toggleX + 26, toggleY + 12, 0xFFFFFFFF);
        } else {
            // Выключен (Серый)
            context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, COLOR_TOGGLE_OFF);
            // Белый кружок слева
            context.fill(toggleX + 4, toggleY + 2, toggleX + 14, toggleY + 12, 0xFFFFFFFF);
        }
    }

    // Кнопка конфига
    private void drawButton(DrawContext context, String text, int x, int y, int w, int h, int mouseX, int mouseY, int color) {
        boolean hover = isHovered(mouseX, mouseY, x, y, w, h);
        context.fill(x, y, x + w, y + h, hover ? color : 0xFF1A1A1A);
        drawRoundedBorder(context, x, y, w, h, 4, hover ? color : COLOR_BORDER);
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    // Имитация скругленных углов
    private void drawRoundedBorder(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        context.fill(x + radius, y, x + w - radius, y + 1, color);
        context.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        context.fill(x, y + radius, x + 1, y + h - radius, color);
        context.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
        context.fill(x + 1, y + 1, x + 2, y + 2, color);
        context.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        context.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        context.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Кнопка закрытия
            if (isHovered(mouseX, mouseY, menuX + MENU_WIDTH - 40, menuY + 10, 20, 20)) {
                this.close();
                return true;
            }
            // Перетаскивание
            if (isHovered(mouseX, mouseY, menuX, menuY, MENU_WIDTH, 50)) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - menuX);
                dragOffsetY = (int) (mouseY - menuY);
                return true;
            }
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x1 = menuX;
        int y1 = menuY;

        // Клики по вкладкам
        if (isHovered(mouseX, mouseY, x1, y1 + 70, 140, 20)) currentTab = 0;
        if (isHovered(mouseX, mouseY, x1, y1 + 100, 140, 20)) currentTab = 1;
        if (isHovered(mouseX, mouseY, x1, y1 + 130, 140, 20)) currentTab = 2;
        if (isHovered(mouseX, mouseY, x1, y1 + 160, 140, 20)) currentTab = 3;
        if (isHovered(mouseX, mouseY, x1, y1 + 190, 140, 20)) currentTab = 4;
        if (isHovered(mouseX, mouseY, x1, y1 + 220, 140, 20)) currentTab = 5;

        int startX = x1 + 140 + 25;
        int modStartY = y1 + 20 + 50;
        
        // Обработка кликов по модулям
        if (currentTab == 0) { 
            if (isHovered(mouseX, mouseY, startX, modStartY, 180, 42)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY, 180, 42)) SmartClient.modules.xray = !SmartClient.modules.xray;
            if (isHovered(mouseX, mouseY, startX, modStartY + 50, 180, 42)) SmartClient.modules.playerESP = !SmartClient.modules.playerESP;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 50, 180, 42)) SmartClient.modules.itemESP = !SmartClient.modules.itemESP;
            if (isHovered(mouseX, mouseY, startX, modStartY + 100, 180, 42)) SmartClient.modules.chestESP = !SmartClient.modules.chestESP;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 100, 180, 42)) SmartClient.modules.tracers = !SmartClient.modules.tracers;
            if (isHovered(mouseX, mouseY, startX, modStartY + 150, 180, 42)) SmartClient.modules.glowESP = !SmartClient.modules.glowESP;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 150, 180, 42)) SmartClient.modules.holeESP = !SmartClient.modules.holeESP;
        } 
        else if (currentTab == 1) { 
            if (isHovered(mouseX, mouseY, startX, modStartY, 180, 42)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY, 180, 42)) SmartClient.modules.autoTotem = !SmartClient.modules.autoTotem;
            if (isHovered(mouseX, mouseY, startX, modStartY + 50, 180, 42)) SmartClient.modules.criticals = !SmartClient.modules.criticals;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 50, 180, 42)) SmartClient.modules.autoTrap = !SmartClient.modules.autoTrap;
            if (isHovered(mouseX, mouseY, startX, modStartY + 100, 180, 42)) SmartClient.modules.damageNumbers = !SmartClient.modules.damageNumbers;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 100, 180, 42)) SmartClient.modules.hitCounter = !SmartClient.modules.hitCounter;
        }
        else if (currentTab == 2) { 
            if (isHovered(mouseX, mouseY, startX, modStartY, 180, 42)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY, 180, 42)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            if (isHovered(mouseX, mouseY, startX, modStartY + 50, 180, 42)) SmartClient.modules.scaffold = !SmartClient.modules.scaffold;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 50, 180, 42)) SmartClient.modules.noFall = !SmartClient.modules.noFall;
            if (isHovered(mouseX, mouseY, startX, modStartY + 100, 180, 42)) SmartClient.modules.sprint = !SmartClient.modules.sprint;
        }
        else if (currentTab == 3) { 
            if (isHovered(mouseX, mouseY, startX, modStartY, 180, 42)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY, 180, 42)) SmartClient.modules.autoArmor = !SmartClient.modules.autoArmor;
            if (isHovered(mouseX, mouseY, startX, modStartY + 50, 180, 42)) SmartClient.modules.chestStealer = !SmartClient.modules.chestStealer;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 50, 180, 42)) SmartClient.modules.velocity = !SmartClient.modules.velocity;
            if (isHovered(mouseX, mouseY, startX, modStartY + 100, 180, 42)) SmartClient.modules.autoRespawn = !SmartClient.modules.autoRespawn;
            if (isHovered(mouseX, mouseY, startX + 195, modStartY + 100, 180, 42)) SmartClient.modules.lowHp = !SmartClient.modules.lowHp;
        }
        else if (currentTab == 4) { 
            if (isHovered(mouseX, mouseY, startX, modStartY, 200, 24)) ConfigManager.saveConfig(SmartClient.modules);
            if (isHovered(mouseX, mouseY, startX, modStartY + 35, 200, 24)) ConfigManager.loadConfig(SmartClient.modules);
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
