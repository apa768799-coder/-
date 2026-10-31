package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    // Идеальные цвета (без прозрачности, чтобы не сливалось)
    private final int COLOR_BG = 0xFF1E1E1E;       // Темно-серый фон
    private final int COLOR_SIDEBAR = 0xFF141414;  // Черная панель слева
    private final int COLOR_ACCENT = 0xFF00AFFF;   // Ярко-синий
    private final int COLOR_TEXT = 0xFFFFFFFF;     // Белый
    private final int COLOR_TEXT_DIM = 0xFF888888; // Серый
    private final int COLOR_HOVER = 0xFF2C2C2C;    // Подсветка при наведении
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;  // Зеленый (ВКЛ)
    private final int COLOR_TOGGLE_OFF = 0xFF444444; // Темно-серый (ВЫКЛ)

    private int currentTab = 0; // 0=Combat, 1=Movement, 2=Render, 3=Player

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Затемняем фон игры вручную (без размытия)
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int cx = this.width / 2;
        int cy = this.height / 2;
        int menuW = 400;
        int menuH = 260;
        int x1 = cx - menuW / 2;
        int y1 = cy - menuH / 2;
        int x2 = cx + menuW / 2;
        int y2 = cy + menuH / 2;

        // 2. Тень
        context.fill(x1 + 5, y1 + 5, x2 + 5, y2 + 5, 0x60000000);
        
        // 3. Основной фон
        context.fill(x1, y1, x2, y2, COLOR_BG);
        
        // 4. Рамка
        context.drawBorder(x1, y1, menuW, menuH, COLOR_ACCENT);

        // ---------------- ЛЕВАЯ ПАНЕЛЬ ----------------
        int catWidth = 120;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR); 
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF000000);

        // Логотип
        context.drawText(this.textRenderer, "§lSMART", x1 + 15, y1 + 20, 0xFF00AFFF, false);
        context.drawText(this.textRenderer, "§lCLIENT", x1 + 15, y1 + 32, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§8v14.0", x1 + 15, y1 + 44, COLOR_TEXT_DIM, false);

        // Категории
        drawCategory(context, "Combat", 0, y1 + 80, mouseX, mouseY, x1);
        drawCategory(context, "Movement", 1, y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "Render", 2, y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", 3, y1 + 155, mouseX, mouseY, x1);

        // ---------------- ПРАВАЯ ПАНЕЛЬ ----------------
        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;
        int modStartY = startY + 30;
        int x2Pos = cx + 200;

        if (currentTab == 0) { // COMBAT
            context.drawText(this.textRenderer, "§l§fБОЙ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "KillAura", SmartClient.modules.killAura, startX, modStartY, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 1) { // MOVEMENT
            context.drawText(this.textRenderer, "§l§fФАРМ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "AutoMine", SmartClient.modules.autoMine, startX, modStartY, x2Pos, mouseX, mouseY);
            
            // Выбор руды
            context.drawText(this.textRenderer, "§7Цель:", startX, modStartY + 30, COLOR_TEXT_DIM, false);
            context.drawText(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 40, modStartY + 30, COLOR_TEXT, false);
            
            boolean hoverLeft = isHovered(mouseX, mouseY, x2 - 60, modStartY + 26, 15, 15);
            boolean hoverRight = isHovered(mouseX, mouseY, x2 - 40, modStartY + 26, 15, 15);
            context.drawText(this.textRenderer, hoverLeft ? "§a<" : "§7<", x2 - 60, modStartY + 30, COLOR_TEXT, false);
            context.drawText(this.textRenderer, hoverRight ? "§a>" : "§7>", x2 - 40, modStartY + 30, COLOR_TEXT, false);

            drawModule(context, "AutoFish", SmartClient.modules.autoFish, startX, modStartY + 60, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 2) { // RENDER
            context.drawText(this.textRenderer, "§l§fВИЗУАЛЫ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "FullBright", SmartClient.modules.fullBright, startX, modStartY, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 3) { // PLAYER
            context.drawText(this.textRenderer, "§l§fИГРОК", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, startX, modStartY, x2Pos, mouseX, mouseY);
        }

        context.drawText(this.textRenderer, "§8Нажми Right Shift, чтобы закрыть", cx - 70, y2 - 15, 0x555555, false);
    }

    private void drawCategory(DrawContext context, String name, int tabIndex, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1 + 5, y - 3, 110, 16);
        boolean active = (currentTab == tabIndex);

        if (hover || active) {
            context.fill(x1 + 5, y - 3, x1 + 115, y + 13, COLOR_HOVER);
            context.fill(x1 + 5, y - 3, x1 + 7, y + 13, COLOR_ACCENT); // Полоска слева
        }
        context.drawText(this.textRenderer, name, x1 + 15, y, (hover || active) ? 0xFFFFFFFF : 0xFFAAAAAA, false);
    }

    private void drawModule(DrawContext context, String name, boolean state, int x, int y, int x2, int mouseX, int mouseY) {
        int toggleW = 26;
        int toggleH = 12;
        int toggleX = x2 - 40 - toggleW; 
        int toggleY = y - 2;

        boolean hoverRow = isHovered(mouseX, mouseY, x, y - 5, (x2 - x - 20), 20);

        if (hoverRow) {
            context.fill(x - 5, y - 5, x2 - 20, y + 15, COLOR_HOVER);
        }

        context.drawText(this.textRenderer, name, x, y, state ? 0xFFFFFFFF : 0xFF888888, false);
        
        // Отрисовка переключателя
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        
        // Кружочек
        int circleX = toggleX + (state ? toggleW - 10 : 2);
        context.fill(circleX, toggleY + 2, circleX + 8, toggleY + 10, 0xFFFFFFFF);
        
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int x1 = cx - 200; 
        int y1 = cy - 130;

        // Клики по категориям
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 77, 110, 16)) currentTab = 0;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 102, 110, 16)) currentTab = 1;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 127, 110, 16)) currentTab = 2;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 152, 110, 16)) currentTab = 3;

        int startX = x1 + 120 + 20;
        int modStartY = y1 + 20 + 30;
        int x2 = cx + 200;
        int toggleX = x2 - 40 - 26;

        // Клики по модулям в зависимости от вкладки
        if (currentTab == 0) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        } 
        else if (currentTab == 1) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 58, 26, 12)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;

            // Клики по стрелочкам руды
            if (isHovered(mouseX, mouseY, x2 - 60, modStartY + 28, 15, 15)) {
                SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
            }
            if (isHovered(mouseX, mouseY, x2 - 40, modStartY + 28, 15, 15)) {
                SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex - 1 + SmartClient.modules.oreNames.length) % SmartClient.modules.oreNames.length;
            }
        }
        else if (currentTab == 2) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
        }
        else if (currentTab == 3) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;
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
