package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    // Цвета в формате ARGB (Alpha, Red, Green, Blue)
    private final int COLOR_BG = 0xE6121212;       // Темный полупрозрачный фон
    private final int COLOR_ACCENT = 0xFF00FFFF;   // Неоново-голубой
    private final int COLOR_TEXT = 0xFFFFFFFF;     // Белый
    private final int COLOR_TEXT_DIM = 0xFFAAAAAA; // Серый
    private final int COLOR_HOVER = 0x40FFFFFF;    // Белая полупрозрачная подсветка
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;  // Зеленый (ВКЛ)
    private final int COLOR_TOGGLE_OFF = 0xFF333333; // Темно-серый (ВЫКЛ)

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Размытый фон игры (стандартный для 1.21)
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int cx = this.width / 2;
        int cy = this.height / 2;
        int menuW = 360;
        int menuH = 240;
        int x1 = cx - menuW / 2;
        int y1 = cy - menuH / 2;
        int x2 = cx + menuW / 2;
        int y2 = cy + menuH / 2;

        // Тень меню
        context.fill(x1 + 5, y1 + 5, x2 + 5, y2 + 5, 0x50000000);
        // Основной фон меню
        context.fill(x1, y1, x2, y2, COLOR_BG);
        
        // Неоновая рамка (двойная для эффекта свечения)
        context.drawBorder(x1, y1, menuW, menuH, COLOR_ACCENT);
        context.drawBorder(x1 - 1, y1 - 1, menuW + 2, menuH + 2, 0x4000FFFF);

        // ---------------- ЛЕВАЯ ПАНЕЛЬ (Категории) ----------------
        int catWidth = 90;
        context.fill(x1, y1, x1 + catWidth, y2, 0xFF181818); // Панель категорий чуть темнее
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF333333); // Разделитель

        context.drawTextWithShadow(this.textRenderer, "§l§bSMART", x1 + 15, y1 + 15, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, "§l§fCLIENT", x1 + 15, y1 + 26, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, "§8v6.1", x1 + 15, y1 + 37, COLOR_TEXT_DIM);

        drawCategory(context, "Combat", y1 + 70, mouseX, mouseY);
        drawCategory(context, "Movement", y1 + 95, mouseX, mouseY);
        drawCategory(context, "Render", y1 + 120, mouseX, mouseY);
        drawCategory(context, "Player", y1 + 145, mouseX, mouseY);

        // ---------------- ПРАВАЯ ПАНЕЛЬ (Модули) ----------------
        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;

        // Заголовок
        context.drawTextWithShadow(this.textRenderer, "§lМОДУЛИ", startX, startY, COLOR_ACCENT);
        context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333); // Линия под заголовком

        int modStartY = startY + 25;

        // Отрисовка модулей
        drawModule(context, "AutoMine", SmartClient.modules.autoMine, startX, modStartY, mouseX, mouseY);
        
        // Выбор руды
        context.drawTextWithShadow(this.textRenderer, "§7Цель:", startX, modStartY + 30, COLOR_TEXT_DIM);
        context.drawTextWithShadow(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 40, modStartY + 30, COLOR_TEXT);
        
        boolean hoverLeft = isHovered(mouseX, mouseY, x2 - 60, modStartY + 28, 15, 15);
        boolean hoverRight = isHovered(mouseX, mouseY, x2 - 40, modStartY + 28, 15, 15);
        context.drawTextWithShadow(this.textRenderer, hoverLeft ? "§a<" : "§7<", x2 - 60, modStartY + 30, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, hoverRight ? "§a>" : "§7>", x2 - 40, modStartY + 30, COLOR_TEXT);

        drawModule(context, "AutoFish", SmartClient.modules.autoFish, startX, modStartY + 60, mouseX, mouseY);
        drawModule(context, "KillAura", SmartClient.modules.killAura, startX, modStartY + 90, mouseX, mouseY);
        drawModule(context, "FullBright", SmartClient.modules.fullBright, startX, modStartY + 120, mouseX, mouseY);
        drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, startX, modStartY + 150, mouseX, mouseY);

        // Подсказка внизу
        context.drawCenteredTextWithShadow(this.textRenderer, "§8Нажми Right Shift, чтобы закрыть", cx, y2 - 15, 0x555555);
        
        super.render(context, mouseX, mouseY, delta);
    }

    // Отрисовка категории (просто текст с подсветкой при наведении)
    private void drawCategory(DrawContext context, String name, int y, int mouseX, int mouseY) {
        boolean hover = isHovered(mouseX, mouseY, (this.width / 2) - 180 + 10, y - 2, 70, 15);
        context.drawTextWithShadow(this.textRenderer, (hover ? "§b" : "§7") + name, (this.width / 2) - 180 + 15, y, COLOR_TEXT);
    }

    // Отрисовка модуля с красивым переключателем
    private void drawModule(DrawContext context, String name, boolean state, int x, int y, int mouseX, int mouseY) {
        // Название модуля
        context.drawTextWithShadow(this.textRenderer, (state ? "§f" : "§7") + name, x, y, COLOR_TEXT);
        
        int toggleX = x + 180; // Позиция переключателя
        int toggleY = y - 2;
        int toggleW = 26;
        int toggleH = 12;
        
        boolean hover = isHovered(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH);
        
        // Фон переключателя с анимацией
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        if (hover && !state) bgColor = 0xFF555555; // Подсветка при наведении, если выкл
        
        // Отрисовка "таблетки" переключателя
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        
        // Кружочек (сдвигается вправо, если включено)
        int circleX = toggleX + (state ? 16 : 2);
        context.fill(circleX, toggleY + 2, circleX + 8, toggleY + 10, 0xFFFFFFFF);

        // Тонкая обводка вокруг переключателя
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0x40000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button); // Только ЛКМ

        int cx = this.width / 2;
        int cy = this.height / 2;
        
        int x1 = cx - 180;
        int startX = x1 + 90 + 20;
        int startY = cy - 120 + 20 + 25;
        int toggleX = startX + 180;

        // Клики по переключателям
        if (isHovered(mouseX, mouseY, toggleX, startY - 2, 26, 12)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
        if (isHovered(mouseX, mouseY, toggleX, startY + 58, 26, 12)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
        if (isHovered(mouseX, mouseY, toggleX, startY + 88, 26, 12)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        if (isHovered(mouseX, mouseY, toggleX, startY + 118, 26, 12)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
        if (isHovered(mouseX, mouseY, toggleX, startY + 148, 26, 12)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;

        // Клики по стрелочкам выбора руды
        if (isHovered(mouseX, mouseY, cx + 180 - 60, startY + 28, 15, 15)) {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
        }
        if (isHovered(mouseX, mouseY, cx + 180 - 40, startY + 28, 15, 15)) {
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
