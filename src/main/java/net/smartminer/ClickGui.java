package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    // АБСОЛЮТНО НЕПРОЗРАЧНЫЕ ЦВЕТА (Альфа = FF)
    private final int COLOR_BG = 0xFF101010;       // Почти черный фон
    private final int COLOR_SIDEBAR = 0xFF181818;  // Панель слева
    private final int COLOR_ACCENT = 0xFF00FFFF;   // Неоново-голубой
    private final int COLOR_TEXT = 0xFFFFFFFF;     // Чистый белый
    private final int COLOR_TEXT_DIM = 0xFFAAAAAA; // Светло-серый
    private final int COLOR_HOVER = 0xFF2A2A2A;    // Подсветка строки
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;  // Ярко-зеленый
    private final int COLOR_TOGGLE_OFF = 0xFF444444; // Темно-серый

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Рисуем затемнение всего экрана вручную (без размытия)
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int cx = this.width / 2;
        int cy = this.height / 2;
        int menuW = 380;
        int menuH = 260;
        int x1 = cx - menuW / 2;
        int y1 = cy - menuH / 2;
        int x2 = cx + menuW / 2;
        int y2 = cy + menuH / 2;

        // 2. Тень от меню
        context.fill(x1 + 8, y1 + 8, x2 + 8, y2 + 8, 0x60000000);
        
        // 3. Основной фон (Непрозрачный!)
        context.fill(x1, y1, x2, y2, COLOR_BG);
        
        // 4. Красивая рамка (Неоновая)
        context.drawBorder(x1, y1, menuW, menuH, COLOR_ACCENT);
        context.drawBorder(x1 - 1, y1 - 1, menuW + 2, menuH + 2, 0xFF005555); // Внешняя тень рамки

        // ---------------- ЛЕВАЯ ПАНЕЛЬ ----------------
        int catWidth = 100;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR); 
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF333333); // Разделитель

        // Логотип
        context.drawTextWithShadow(this.textRenderer, "§l§bSMART", x1 + 15, y1 + 20, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, "§l§fCLIENT", x1 + 15, y1 + 32, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, "§8v8.0", x1 + 15, y1 + 44, COLOR_TEXT_DIM);

        // Категории (просто для красоты, визуально)
        drawCategory(context, "Combat", y1 + 80, mouseX, mouseY, x1);
        drawCategory(context, "Movement", y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "Render", y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", y1 + 155, mouseX, mouseY, x1);

        // ---------------- ПРАВАЯ ПАНЕЛЬ ----------------
        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;

        // Заголовок
        context.drawTextWithShadow(this.textRenderer, "§l§fМОДУЛИ", startX, startY, COLOR_TEXT);
        context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333); // Линия

        int modStartY = startY + 30;

        // Модули
        drawModule(context, "AutoMine", SmartClient.modules.autoMine, startX, modStartY, x2, mouseX, mouseY);
        
        // Выбор руды
        context.drawTextWithShadow(this.textRenderer, "§7Цель:", startX, modStartY + 30, COLOR_TEXT_DIM);
        context.drawTextWithShadow(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 40, modStartY + 30, COLOR_TEXT);
        
        // Стрелочки
        boolean hoverLeft = isHovered(mouseX, mouseY, x2 - 60, modStartY + 26, 15, 15);
        boolean hoverRight = isHovered(mouseX, mouseY, x2 - 40, modStartY + 26, 15, 15);
        context.drawTextWithShadow(this.textRenderer, hoverLeft ? "§a<" : "§7<", x2 - 60, modStartY + 30, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, hoverRight ? "§a>" : "§7>", x2 - 40, modStartY + 30, COLOR_TEXT);

        drawModule(context, "AutoFish", SmartClient.modules.autoFish, startX, modStartY + 60, x2, mouseX, mouseY);
        drawModule(context, "KillAura", SmartClient.modules.killAura, startX, modStartY + 90, x2, mouseX, mouseY);
        drawModule(context, "FullBright", SmartClient.modules.fullBright, startX, modStartY + 120, x2, mouseX, mouseY);
        drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, startX, modStartY + 150, x2, mouseX, mouseY);

        // Подсказка
        context.drawCenteredTextWithShadow(this.textRenderer, "§8Right Shift - закрыть", cx, y2 - 15, 0x555555);
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCategory(DrawContext context, String name, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1 + 10, y - 3, 80, 16);
        if (hover) {
            context.fill(x1 + 5, y - 3, x1 + 95, y + 13, COLOR_HOVER);
            context.fill(x1 + 5, y - 3, x1 + 7, y + 13, COLOR_ACCENT);
        }
        context.drawTextWithShadow(this.textRenderer, (hover ? "§f" : "§7") + name, x1 + 15, y, COLOR_TEXT);
    }

    private void drawModule(DrawContext context, String name, boolean state, int x, int y, int x2, int mouseX, int mouseY) {
        int toggleW = 26;
        int toggleH = 12;
        int toggleX = x2 - 40 - toggleW; 
        int toggleY = y - 2;

        boolean hoverRow = isHovered(mouseX, mouseY, x, y - 5, (x2 - x - 20), 20);

        // Подсветка строки
        if (hoverRow) {
            context.fill(x - 5, y - 5, x2 - 20, y + 15, COLOR_HOVER);
        }

        // Текст
        context.drawTextWithShadow(this.textRenderer, (state ? "§f" : "§7") + name, x, y, COLOR_TEXT);
        
        // Фон тумблера
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        
        // Кружочек
        int circleX = toggleX + (state ? toggleW - 10 : 2);
        context.fill(circleX, toggleY + 2, circleX + 8, toggleY + 10, 0xFFFFFFFF);

        // Обводка
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int cx = this.width / 2;
        int cy = this.height / 2;
        
        int x1 = cx - 190; // menuW / 2
        int startX = x1 + 100 + 20;
        int startY = cy - 130 + 20 + 30; // y1 + 20 + 30
        int x2 = cx + 190;
        int toggleX = x2 - 40 - 26;

        // Клики по тумблерам
        if (isHovered(mouseX, mouseY, toggleX, startY - 2, 26, 12)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
        if (isHovered(mouseX, mouseY, toggleX, startY + 58, 26, 12)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
        if (isHovered(mouseX, mouseY, toggleX, startY + 88, 26, 12)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        if (isHovered(mouseX, mouseY, toggleX, startY + 118, 26, 12)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
        if (isHovered(mouseX, mouseY, toggleX, startY + 148, 26, 12)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;

        // Клики по руде
        if (isHovered(mouseX, mouseY, x2 - 60, startY + 28, 15, 15)) {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
        }
        if (isHovered(mouseX, mouseY, x2 - 40, startY + 28, 15, 15)) {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex - 1 + SmartClient.modules.oreNames.length) % SmartClient.modules.oreNames.length;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
