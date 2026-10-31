package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    private int x, y, width, height;

    public ClickGui() {
        super(Text.literal("Smart Client Menu"));
    }

    @Override
    protected void init() {
        this.width = 240;
        this.height = 280;
        this.x = (this.width / 2) - (this.width / 2); // Переопределяем в render
        this.x = (MinecraftClient.getInstance().getWindow().getScaledWidth() - this.width) / 2;
        this.y = (MinecraftClient.getInstance().getWindow().getScaledHeight() - this.height) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рисуем фон игры
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int cx = (this.width / 2);
        int cy = (this.height / 2);

        // Тень
        context.fill(cx - 122, cy - 142, cx + 122, cy + 142, 0x50000000);
        // Основной фон (темно-серый)
        context.fill(cx - 120, cy - 140, cx + 120, cy + 140, 0xFF1E1E1E);
        // Неоновая рамка
        context.drawBorder(cx - 120, cy - 140, 240, 280, 0xFF00FFFF);

        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, "§l§bSMART CLIENT §fv6.0", cx, cy - 125, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Кастомное меню", cx, cy - 113, 0x888888);
        
        // Разделитель
        context.fill(cx - 110, cy - 105, cx + 110, cy - 104, 0xFF333333);

        // ================= МОДУЛИ =================
        int startY = cy - 90;
        drawModule(context, "AutoMine", SmartClient.modules.autoMine, cx - 100, startY, mouseX, mouseY);
        
        // Выбор руды
        context.drawTextWithShadow(this.textRenderer, "Target:", cx - 100, startY + 25, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex], cx - 40, startY + 25, 0xFFFFFF);
        // Стрелочки для смены руды
        context.drawTextWithShadow(this.textRenderer, "§7<", cx + 80, startY + 25, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§7>", cx + 90, startY + 25, 0xFFFFFF);

        drawModule(context, "AutoFish", SmartClient.modules.autoFish, cx - 100, startY + 45, mouseX, mouseY);
        drawModule(context, "KillAura", SmartClient.modules.killAura, cx - 100, startY + 70, mouseX, mouseY);
        drawModule(context, "FullBright", SmartClient.modules.fullBright, cx - 100, startY + 95, mouseX, mouseY);
        drawModule(context, "Anti-AFK", SmartClient.modules.antiAfk, cx - 100, startY + 120, mouseX, mouseY);

        // Подсказки внизу
        context.drawCenteredTextWithShadow(this.textRenderer, "§8Right Shift - закрыть", cx, cy + 125, 0x555555);
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawModule(DrawContext context, String name, boolean state, int x, int y, int mouseX, int mouseY) {
        // Текст
        context.drawTextWithShadow(this.textRenderer, name, x, y, 0xFFFFFF);
        
        // Переключатель (Toggle)
        int toggleX = x + 170;
        int toggleY = y - 2;
        int toggleW = 30;
        int toggleH = 12;
        
        // Фон переключателя
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, state ? 0xFF00FF00 : 0xFF555555);
        // Кружочек
        context.fill(toggleX + (state ? 18 : 2), toggleY + 2, toggleX + (state ? 28 : 12), toggleY + 10, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = (this.width / 2);
        int cy = (this.height / 2);
        int startY = cy - 90;
        int toggleX = cx - 100 + 170;

        // Проверка клика по переключателям
        if (isHovered(mouseX, mouseY, toggleX, startY - 2, 30, 12)) SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
        if (isHovered(mouseX, mouseY, toggleX, startY + 43, 30, 12)) SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
        if (isHovered(mouseX, mouseY, toggleX, startY + 68, 30, 12)) SmartClient.modules.killAura = !SmartClient.modules.killAura;
        if (isHovered(mouseX, mouseY, toggleX, startY + 93, 30, 12)) SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
        if (isHovered(mouseX, mouseY, toggleX, startY + 118, 30, 12)) SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;

        // Проверка клика по стрелочкам руды
        if (isHovered(mouseX, mouseY, cx + 80, startY + 23, 10, 10)) {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
        }
        if (isHovered(mouseX, mouseY, cx + 90, startY + 23, 10, 10)) {
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
