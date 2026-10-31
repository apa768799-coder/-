package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    private final int COLOR_BG = 0xFF2B2D31;       
    private final int COLOR_SIDEBAR = 0xFF1E1F22;  
    private final int COLOR_ACCENT = 0xFF00FFFF;   
    private final int COLOR_TEXT = 0xFFFFFFFF;     
    private final int COLOR_TEXT_DIM = 0xFFAAAAAA; 
    private final int COLOR_HOVER = 0xFF40444B;    
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;  
    private final int COLOR_TOGGLE_OFF = 0xFF80848E; 

    // 0 = Combat, 1 = Movement, 2 = Render, 3 = Player
    private int currentTab = 1; 

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        
        int cx = this.width / 2;
        int cy = this.height / 2;
        int menuW = 380;
        int menuH = 260;
        int x1 = cx - menuW / 2;
        int y1 = cy - menuH / 2;
        int x2 = cx + menuW / 2;
        int y2 = cy + menuH / 2;

        context.fill(x1 + 6, y1 + 6, x2 + 6, y2 + 6, 0x40000000);
        context.fill(x1, y1, x2, y2, COLOR_BG);
        
        context.drawBorder(x1, y1, menuW, menuH, COLOR_ACCENT);
        context.drawBorder(x1 - 1, y1 - 1, menuW + 2, menuH + 2, 0xFF000000);

        // ---------------- ЛЕВАЯ ПАНЕЛЬ (ВКЛАДКИ) ----------------
        int catWidth = 100;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR); 
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF000000);

        context.drawText(this.textRenderer, "§l§bSMART", x1 + 15, y1 + 20, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§l§fCLIENT", x1 + 15, y1 + 32, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "§8v10.0", x1 + 15, y1 + 44, COLOR_TEXT_DIM, false);

        drawCategory(context, "Combat", 0, y1 + 80, mouseX, mouseY, x1);
        drawCategory(context, "Movement", 1, y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "Render", 2, y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", 3, y1 + 155, mouseX, mouseY, x1);

        // ---------------- ПРАВАЯ ПАНЕЛЬ (МОДУЛИ) ----------------
        int startX = x1 + catWidth + 20;
        int startY = y1 + 20;
        int modStartY = startY + 30;
        int x2Pos = cx + 190;

        // Логика переключения вкладок
        if (currentTab == 0) { // COMBAT
            context.drawText(this.textRenderer, "§l§fБОЙ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "KillAura", SmartClient.modules.killAura, startX, modStartY, x2Pos, mouseX, mouseY);
            
        } else if (currentTab == 1) { // MOVEMENT (Farming)
            context.drawText(this.textRenderer, "§l§fФАРМ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "AutoMine", SmartClient.modules.autoMine, startX, modStartY, x2Pos, mouseX, mouseY);
            
            context.drawText(this.textRenderer, "§7Цель:", startX, modStartY + 30, COLOR_TEXT_DIM, false);
            context.drawText(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 40, modStartY + 30, COLOR_TEXT, false);
            
            boolean hoverLeft = isHovered(mouseX, mouseY, x2 - 60, modStartY + 26, 15, 15);
            boolean hoverRight = isHovered(mouseX, mouseY, x2 - 40, modStartY + 26, 15, 15);
            context.drawText(this.textRenderer, hoverLeft ? "§a<" : "§7<", x2 - 60, modStartY + 30, COLOR_TEXT, false);
            context.drawText(this.textRenderer, hoverRight ? "§a>" : "§7>", x2 - 40, modStartY + 30, COLOR_TEXT, false);

            drawModule(context, "AutoFish", SmartClient.modules.autoFish, startX, modStartY + 60, x2Pos, mouseX, mouseY);

        } else if (currentTab == 2) { // RENDER
            context.drawText(this.textRenderer, "§l§fВИЗУАЛЫ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "FullBright", SmartClient.modules.fullBright, startX, modStartY, x2Pos, mouseX, mouseY);

        } else if (currentTab == 3) { // PLAYER
            context.drawText(this.textRenderer, "§l§fИГРОК", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            
            drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, startX, modStartY, x2Pos, mouseX, mouseY);
        }

        context.drawText(this.textRenderer, "§8Right Shift - закрыть", cx - 50, y2 - 15, 0x555555, false);
    }

    private void drawCategory(DrawContext context, String name, int tabIndex, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1 + 5, y - 3, 90, 16);
        boolean active = (currentTab == tabIndex);

        if (hover || active) {
            context.fill(x1 + 5, y - 3, x1 + 95, y + 13, COLOR_HOVER);
            context.fill(x1 + 5, y - 3, x1 + 7, y + 13, COLOR_ACCENT);
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
        
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        
        int circleX = toggleX + (state ? toggleW - 10 : 2);
        context.fill(circleX, toggleY + 2, circleX + 8, toggleY + 10, 0xFFFFFFFF);
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int x1 = cx - 190; 
        int y1 = cy - 130;

        // Клики по вкладкам
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 77, 90, 16)) currentTab = 0;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 102, 90, 16)) currentTab = 1;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 127, 90, 16)) currentTab = 2;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 152, 90, 16)) currentTab = 3;

        int startX = x1 + 100 + 20;
        int modStartY = y1 + 20 + 30;
        int x2 = cx + 190;
        int toggleX = x2 - 40 - 26;

        // Клики по модулям (в зависимости от вкладки)
        if (currentTab == 0) { // Combat
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        } 
        else if (currentTab == 1) { // Movement
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 58, 26, 12)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;

            // Стрелочки руды
            if (isHovered(mouseX, mouseY, x2 - 60, modStartY + 28, 15, 15)) {
                SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
            }
            if (isHovered(mouseX, mouseY, x2 - 40, modStartY + 28, 15, 15)) {
                SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex - 1 + SmartClient.modules.oreNames.length) % SmartClient.modules.oreNames.length;
            }
        }
        else if (currentTab == 2) { // Render
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
        }
        else if (currentTab == 3) { // Player
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
