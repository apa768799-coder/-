package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    public ClickGui() {
        super(Text.literal("Smart Client v3"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;

        // Кнопка Авто-Майн
        this.addDrawableChild(ButtonWidget.builder(getText("AutoMine", SmartClient.modules.autoMine), button -> {
            SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            button.setMessage(getText("AutoMine", SmartClient.modules.autoMine));
        }).dimensions(centerX - 80, startY, 160, 20).build());

        // Кнопка выбора руды
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Руда: §e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex]), button -> {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
            button.setMessage(Text.literal("Руда: §e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex]));
        }).dimensions(centerX - 80, startY + 25, 160, 20).build());

        // Кнопка Авто-Рыбалка
        this.addDrawableChild(ButtonWidget.builder(getText("AutoFish", SmartClient.modules.autoFish), button -> {
            SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            button.setMessage(getText("AutoFish", SmartClient.modules.autoFish));
        }).dimensions(centerX - 80, startY + 50, 160, 20).build());

        // Кнопка KillAura
        this.addDrawableChild(ButtonWidget.builder(getText("KillAura", SmartClient.modules.killAura), button -> {
            SmartClient.modules.killAura = !SmartClient.modules.killAura;
            button.setMessage(getText("KillAura", SmartClient.modules.killAura));
        }).dimensions(centerX - 80, startY + 75, 160, 20).build());

        // Закрыть
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> {
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(centerX - 80, startY + 110, 160, 20).build());
    }

    private Text getText(String name, boolean state) {
        return Text.literal(name + ": " + (state ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        // Темная панель с фиолетовой рамкой
        context.fill(centerX - 100, centerY - 80, centerX + 100, centerY + 80, 0xEE101010);
        context.drawBorder(centerX - 100, centerY - 80, 200, 160, 0xFF9900FF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§l§dSMART CLIENT v3.0", centerX, centerY - 70, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7R - AutoMine | F - AutoFish", centerX, centerY + 60, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
