package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    public ClickGui() {
        super(Text.literal("Smart Client"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        this.addDrawableChild(ButtonWidget.builder(getButtonText("AutoMine", SmartClient.modules.autoMine), button -> {
            SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            button.setMessage(getButtonText("AutoMine", SmartClient.modules.autoMine));
        }).dimensions(centerX - 60, startY, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getButtonText("AutoFish", SmartClient.modules.autoFish), button -> {
            SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            button.setMessage(getButtonText("AutoFish", SmartClient.modules.autoFish));
        }).dimensions(centerX - 60, startY + 25, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getButtonText("KillAura", SmartClient.modules.killAura), button -> {
            SmartClient.modules.killAura = !SmartClient.modules.killAura;
            button.setMessage(getButtonText("KillAura", SmartClient.modules.killAura));
        }).dimensions(centerX - 60, startY + 50, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть (Right Shift)"), button -> {
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(centerX - 60, startY + 90, 120, 20).build());
    }

    private Text getButtonText(String name, boolean state) {
        return Text.literal(name + ": " + (state ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.fill(centerX - 80, centerY - 70, centerX + 80, centerY + 60, 0xCC000000);
        context.drawBorder(centerX - 80, centerY - 70, 160, 130, 0xFF00FFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§l§bSMART CLIENT v2.0", centerX, centerY - 60, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
