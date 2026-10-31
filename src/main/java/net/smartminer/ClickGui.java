package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    private final int COLOR_BG = 0xFF1E1E1E;       
    private final int COLOR_SIDEBAR = 0xFF141414;  
    private final int COLOR_ACCENT = 0xFF00AFFF;   
    private final int COLOR_TEXT = 0xFFFFFFFF;     
    private final int COLOR_TEXT_DIM = 0xFF888888; 
    private final int COLOR_HOVER = 0xFF2C2C2C;    
    private final int COLOR_TOGGLE_ON = 0xFF00FF00;  
    private final int COLOR_TOGGLE_OFF = 0xFF444444; 

    private int currentTab = 0; 
    private int dragX = 0, dragY = 0;
    private boolean isDragging = false;

    private int menuX = -1, menuY = -1;

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (menuX == -1 || menuY == -1) {
            menuX = (this.width - 400) / 2;
            menuY = (this.height - 260) / 2;
        }

        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int x1 = menuX;
        int y1 = menuY;
        int menuW = 400;
        int menuH = 260;
        int x2 = x1 + menuW;
        int y2 = y1 + menuH;

        context.fill(x1 + 5, y1 + 5, x2 + 5, y2 + 5, 0x60000000);
        context.fill(x1, y1, x2, y2, COLOR_BG);
        context.drawBorder(x1, y1, menuW, menuH, COLOR_ACCENT);

        int catWidth = 120;
        context.fill(x1, y1, x1 + catWidth, y2, COLOR_SIDEBAR); 
        context.fill(x1 + catWidth, y1, x1 + catWidth + 1, y2, 0xFF000000);

        context.fill(x1, y1, x2, y1 + 20, 0xFF1A1A1A);
        context.drawText(this.textRenderer, "§lSMART §fCLIENT §7v25", x1 + 10, y1 + 6, COLOR_TEXT, false);

        drawCategory(context, "Combat", 0, y1 + 80, mouseX, mouseY, x1);
        drawCategory(context, "Movement", 1, y1 + 105, mouseX, mouseY, x1);
        drawCategory(context, "Render", 2, y1 + 130, mouseX, mouseY, x1);
        drawCategory(context, "Player", 3, y1 + 155, mouseX, mouseY, x1);

        int startX = x1 + catWidth + 20;
        int startY = y1 + 35;
        int modStartY = startY + 30;
        int x2Pos = x2;

        if (currentTab == 0) { 
            context.drawText(this.textRenderer, "§l§fБОЙ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            drawModule(context, "KillAura", SmartClient.modules.killAura, startX, modStartY, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 1) { 
            context.drawText(this.textRenderer, "§l§fДВИЖЕНИЕ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            drawModule(context, "AutoMine", SmartClient.modules.autoMine, startX, modStartY, x2Pos, mouseX, mouseY);
            
            context.drawText(this.textRenderer, "§7Руда:", startX, modStartY + 30, COLOR_TEXT_DIM, false);
            context.drawText(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], startX + 35, modStartY + 30, COLOR_TEXT, false);
            boolean hoverLeft = isHovered(mouseX, mouseY, x2 - 60, modStartY + 26, 15, 15);
            boolean hoverRight = isHovered(mouseX, mouseY, x2 - 40, modStartY + 26, 15, 15);
            context.drawText(this.textRenderer, hoverLeft ? "§a<" : "§7<", x2 - 60, modStartY + 30, COLOR_TEXT, false);
            context.drawText(this.textRenderer, hoverRight ? "§a>" : "§7>", x2 - 40, modStartY + 30, COLOR_TEXT, false);

            drawModule(context, "AutoFish", SmartClient.modules.autoFish, startX, modStartY + 60, x2Pos, mouseX, mouseY);
            drawModule(context, "Scaffold", SmartClient.modules.scaffold, startX, modStartY + 90, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 2) { 
            context.drawText(this.textRenderer, "§l§fВИЗУАЛЫ", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            drawModule(context, "FullBright", SmartClient.modules.fullBright, startX, modStartY, x2Pos, mouseX, mouseY);
        } 
        else if (currentTab == 3) { 
            context.drawText(this.textRenderer, "§l§fИГРОК", startX, startY, COLOR_TEXT, false);
            context.fill(startX, startY + 12, x2 - 20, startY + 13, 0xFF000000);
            drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, startX, modStartY, x2Pos, mouseX, mouseY);
            drawModule(context, "AutoArmor", SmartClient.modules.autoArmor, startX, modStartY + 30, x2Pos, mouseX, mouseY);
            drawModule(context, "Velocity", SmartClient.modules.velocity, startX, modStartY + 60, x2Pos, mouseX, mouseY);
            drawModule(context, "ChestStealer", SmartClient.modules.chestStealer, startX, modStartY + 90, x2Pos, mouseX, mouseY);
        }

        context.drawText(this.textRenderer, "§8Right Shift - закрыть", x1 + 10, y2 - 15, 0x555555, false);
    }

    private void drawCategory(DrawContext context, String name, int tabIndex, int y, int mouseX, int mouseY, int x1) {
        boolean hover = isHovered(mouseX, mouseY, x1 + 5, y - 3, 110, 16);
        boolean active = (currentTab == tabIndex);
        if (hover || active) {
            context.fill(x1 + 5, y - 3, x1 + 115, y + 13, COLOR_HOVER);
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
        if (hoverRow) context.fill(x - 5, y - 5, x2 - 20, y + 15, COLOR_HOVER);

        context.drawText(this.textRenderer, name, x, y, state ? 0xFFFFFFFF : 0xFF888888, false);
        
        int bgColor = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        int circleX = toggleX + (state ? toggleW - 10 : 2);
        context.fill(circleX, toggleY + 2, circleX + 8, toggleY + 10, 0xFFFFFFFF);
        context.drawBorder(toggleX, toggleY, toggleW, toggleH, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHovered(mouseX, mouseY, menuX, menuY, 400, 20)) {
                isDragging = true;
                dragX = (int) (mouseX - menuX);
                dragY = (int) (mouseY - menuY);
                return true;
            }
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x1 = menuX; 
        int y1 = menuY;

        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 77, 110, 16)) currentTab = 0;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 102, 110, 16)) currentTab = 1;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 127, 110, 16)) currentTab = 2;
        if (isHovered(mouseX, mouseY, x1 + 5, y1 + 152, 110, 16)) currentTab = 3;

        int startX = x1 + 120 + 20;
        int modStartY = y1 + 35 + 30;
        int x2 = x1 + 400;
        int toggleX = x2 - 40 - 26;

        if (currentTab == 0) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        } 
        else if (currentTab == 1) {
            if (isHovered(mouseX, mouseY, toggleX, modStartY - 2, 26, 12)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 58, 26, 12)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 88, 26, 12)) SmartClient.modules.scaffold = !SmartClient.modules.scaffold;

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
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 28, 26, 12)) SmartClient.modules.autoArmor = !SmartClient.modules.autoArmor;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 58, 26, 12)) SmartClient.modules.velocity = !SmartClient.modules.velocity;
            if (isHovered(mouseX, mouseY, toggleX, modStartY + 88, 26, 12)) SmartClient.modules.chestStealer = !SmartClient.modules.chestStealer;
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
            menuX = (int) (mouseX - dragX);
            menuY = (int) (mouseY - dragY);
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
