package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    // ЦВЕТА (Без прозрачности, чтобы не сливались с фоном)
    private final int COLOR_BG = 0xFF121212;       // Глубокий черный
    private final int COLOR_SIDEBAR = 0xFF1A1A1A;  // Темно-серый для левой панели
    private final int COLOR_ACCENT = 0xFF00FFFF;   // Неоново-голубой
    private final int COLOR_TEXT = 0xFFFFFFFF;     // Чистый белый
    private final int COLOR_TEXT_DIM = 0xFF888888; // Серый
    private final int COLOR_HOVER = 0xFF2A2A2A;    // Серый для подсветки строки
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;  // Ярко-зеленый
    private final int COLOR_TOGGLE_OFF = 0xFF444444; // Темно-серый

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемняем игру на заднем плане
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int cx = this.width / 2;
        int cy = this.height / 2;
        int menuW = 380;
        int menuH = 260;
        int x1 = cx - menuW / 2;
        int y1 = cy - menuH / 2;
        int x2 = cx + menuW / 2;
        int y2 = cy + menuH / 2;

        // Тень меню
        context.fill(x1 + 6, y1 + 6, x2 + 6, y2 + 6, 0x80000000);
        
        // ОСНОВНОЙ ФОН (Непрозрачный)
        context.fill(x1, y1, x2, y2, COLOR_BG);
        
        // РАМКА (Двойная для красоты)
        context.drawBorder(x1, y1, menuW, menuH, COLOR_ACCENT);
        context.drawBorder(x1 - 1, y1 - 1, menuW + 2, menuH + 2, 0xFF005555); // Темная обводка снаружи

        // ---------------- ЛЕВАЯ ПАНЕЛЬ ----------------
        int catWidth = 100;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR); 
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF333333); // Линия разделителя

        // Логотип
        context.drawTextWithShadow(this.textRenderer, "§l§bSMART", x1 + 15, y1 + 20, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, "§l§fCLIENT", x1 + 15, y1 + 32, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, "§8v7.0", x1 + 15, y1 + 44, COLOR_TEXT_DIM);

        // Категории
        drawCategory(context, "Combat", y1 + 80, mouseX, mouseY, x1);
        drawCategory(context, "Movement", y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "Render", y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", y1 + 155, mouseX, mouseY, x1);

        // ---------------- ПРАВАЯ ПАНЕЛЬ ----------------
        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;

        // Заголовок справа
        context.drawTextWithShadow(this.textRenderer, "§l§fМОДУЛИ", startX, startY, COLOR_TEXT);
        context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF333333); // Линия

        int modStartY = startY + 30;

        // Отрисовка модулей
        drawModule(context, "AutoMine", SmartClient.modules.autoMine, startX, modStartY, x2, mouseX, mouseY);
        
        // Выбор руды
        context.drawTextWithShadow(this.textRenderer, "§7Выбор руды:", startX, modStartY + 30, COLOR_TEXT_DIM);
        context.drawTextWithShadow(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 80, modStartY + 30, COLOR_TEXT);
        
        // Стрелочки
        boolean hoverLeft = isHovered(mouseX, mouseY, x2 - 60, modStartY + 26, 15, 15);
        boolean hoverRight = isHovered(mouseX, mouseY, x2 - 40, modStartY + 26, 15, 15);
        context.drawTextWithShadow(this.textRenderer, hoverLeft ? "§a◀" : "§7◀", x2 - 60, modStartY + 30, COLOR_TEXT);
        context.drawTextWithShadow(this.textRenderer, hoverRight ? "§a▶" : "§7▶", x2 - 40, modStartY + 30, COLOR_TEXT);

        drawModule(context, "AutoFish", SmartClient.modules.autoFish, startX, modStartY + 60, x2, mouseX, mouseY);
        drawModule(context, "KillAura", SmartClient.modules.killAura, startX, modStartY + 90, x2, mouseX, mouseY);
        drawModule(context, "FullBright", SmartClient.modules.fullBright, startX, modStartY + 120, x2, mouseX, mouseY);
        drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, startX, modStartY + 150, x2, mouseX, mouseY);

        // Подсказка внизу
        context.drawCenteredTextWithShadow(this.textRenderer, "§8Нажми Right Shift, чтобы закрыть", cx, y2 - 15, 0x555555);
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCategory(DrawContext context, String name, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1 + 10, y - 3, 80, 16);
        if (hover) {
            // Подсветка фона категории при наведении
            context.fill(x1 + 5, y - 3, x1 + 95, y + 13, COLOR_HOVER);
            // Полоска слева
            context.fill(x1 + 5, y - 3, x1 + 7, y + 13, COLOR_ACCENT);
        }
        context.drawTextWithShadow(this.textRenderer, (hover ? "§f" : "§7") + name, x1 + 15, y, COLOR_TEXT);
    }

    private void drawModule(DrawContext context, String name, boolean state, int x, int y, int x2, int mouseX, int mouseY) {
        int toggleW = 30;
        int toggleH = 14;
        int toggleX = x2 - 40 - toggleW; 
        int toggleY = y - 3;

        boolean hoverRow = isHovered(mouseX, mouseY, x, y - 5, (x2 - x - 20), 20);
        boolean hoverToggle = isHovered(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH);

        // Подсветка всей строки при наведении
        if (hoverRow) {
            context.fill(x - 5, y - 5, x2 - 20, y + 15, COLOR_HOVER);
        }

        // Текст (Белый если вкл, Серый если выкл)
        context.drawTextWithShadow(this.textRenderer, (state ? "§f" : "§7") + name, x, y, COLOR_TEXT);
        
        // ФОН ПЕРЕКЛЮЧАТЕЛЯ
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        if (hoverToggle && !state) bgColor = 0xFF666666; // Чуть светлее при наведении
        
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        
        // КРУЖОЧЕК (Переезжает вправо/влево)
        int circleX = toggleX + (state ? toggleW - 12 : 2);
        context.fill(circleX, toggleY + 2, circleX + 10, toggleY + 12, 0xFFFFFFFF);

        // Легкая обводка
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button); // Только ЛКМ

        int cx = this.width / 2;
        int cy = this.height / 2;
        
        int x1 = cx - 190; // Ширина меню 380
        int startX = x1 + 100 + 20;
        int startY = cy - 130 + 20 + 30; // y1 + 20 + 30
        int x2 = cx + 190;
        int toggleX = x2 - 40 - 30;

        // Клики по переключателям
        if (isHovered(mouseX, mouseY, toggleX, startY - 3, 30, 14)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
        if (isHovered(mouseX, mouseY, toggleX, startY + 57, 30, 14)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
        if (isHovered(mouseX, mouseY, toggleX, startY + 87, 30, 14)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        if (isHovered(mouseX, mouseY, toggleX, startY + 117, 30, 14)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
        if (isHovered(mouseX, mouseY, toggleX, startY + 147, 30, 14)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;

        // Клики по стрелочкам выбора руды
        if (isHovered(mouseX, mouseY, x2 - 60, startY + 26, 15, 15)) {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
        }
        if (isHovered(mouseX, mouseY, x2 - 40, startY + 26, 15, 15)) {
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
