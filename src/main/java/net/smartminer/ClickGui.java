package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ClickGui extends Screen {
    public ClickGui() {
        super(Text.literal("Smart Client v5"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 90;

        this.addDrawableChild(ButtonWidget.builder(getText("AutoMine [R]", SmartClient.modules.autoMine), button -> {
            SmartClient.modules.autoMine = !SmartClient.modules.autoMine;
            button.setMessage(getText("AutoMine [R]", SmartClient.modules.autoMine));
        }).dimensions(centerX - 90, startY, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Target: §e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex]), button -> {
            SmartClient.modules.targetOreIndex = (SmartClient.modules.targetOreIndex + 1) % SmartClient.modules.oreNames.length;
            button.setMessage(Text.literal("Target: §e" + SmartClient.modules.oreNames[SmartClient.modules.targetOreIndex]));
        }).dimensions(centerX - 90, startY + 25, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getText("AutoFish [F]", SmartClient.modules.autoFish), button -> {
            SmartClient.modules.autoFish = !SmartClient.modules.autoFish;
            button.setMessage(getText("AutoFish [F]", SmartClient.modules.autoFish));
        }).dimensions(centerX - 90, startY + 50, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getText("KillAura [Crit]", SmartClient.modules.killAura), button -> {
            SmartClient.modules.killAura = !SmartClient.modules.killAura;
            button.setMessage(getText("KillAura [Crit]", SmartClient.modules.killAura));
        }).dimensions(centerX - 90, startY + 75, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getText("FullBright [V]", SmartClient.modules.fullBright), button -> {
            SmartClient.modules.fullBright = !SmartClient.modules.fullBright;
            button.setMessage(getText("FullBright [V]", SmartClient.modules.fullBright));
        }).dimensions(centerX - 90, startY + 100, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(getText("Anti-AFK [K]", SmartClient.modules.antiAfk), button -> {
            SmartClient.modules.antiAfk = !SmartClient.modules.antiAfk;
            button.setMessage(getText("Anti-AFK [K]", SmartClient.modules.antiAfk));
        }).dimensions(centerX - 90, startY + 125, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> {
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(centerX - 90, startY + 165, 180, 20).build());
    }

    private Text getText(String name, boolean state) {
        return Text.literal(name + ": " + (state ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.fill(centerX - 110, centerY - 110, centerX + 110, centerY + 90, 0xEE0F0F0F);
        context.drawBorder(centerX - 110, centerY - 110, 220, 200, 0xFF9900FF); 
        context.drawCenteredTextWithShadow(this.textRenderer, "§l§dSMART CLIENT §7v5.0", centerX, centerY - 100, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
