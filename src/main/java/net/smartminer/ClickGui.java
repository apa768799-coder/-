package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    public static Modules modules = SmartClient.modules;
    public static final int ACCENT_COLOR = new Color(0, 255, 180).getRGB();
    public static final int PANEL_COLOR = new Color(25, 25, 30, 255).getRGB();
    public static final int HEADER_COLOR = new Color(35, 35, 45, 255).getRGB();
    public static final int TEXT_COLOR = new Color(220, 220, 220).getRGB();

    private final List<Panel> panels = new ArrayList<>();
    private Panel draggingPanel = null;
    private double dragX, dragY;

    public ClickGui() {
        super(Text.literal("SmartMiner"));
        int x = 20;
        panels.add(new Panel("Combat", x, 20));
        panels.add(new Panel("Movement", x + 120, 20));
        panels.add(new Panel("Render", x + 240, 20));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, new Color(0, 0, 0, 120).getRGB());
        for (Panel panel : panels) panel.render(context, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        for (Panel p : panels) if (p.mouseClicked(mx, my, btn)) { draggingPanel = p; dragX = mx - p.x; dragY = my - p.y; return true; }
        return super.mouseClicked(mx, my, btn);
    }
    @Override public boolean mouseReleased(double mx, double my, int btn) { draggingPanel = null; for (Panel p : panels) p.mouseReleased(); return super.mouseReleased(mx, my, btn); }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingPanel != null) { draggingPanel.x = (int)(mx - dragX); draggingPanel.y = (int)(my - dragY); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    public class Panel {
        public String title; public int x, y; public int width = 110, headerHeight = 18, maxHeight = 200;
        public List<ModuleButton> buttons = new ArrayList<>(); public boolean dragging = false;

        public Panel(String title, int x, int y) {
            this.title = title; this.x = x; this.y = y;
            if (title.equals("Combat")) {
                buttons.add(new ModuleButton("KillAura", () -> modules.killAura, v -> modules.killAura = v));
                buttons.add(new ModuleButton("AutoCrystal", () -> modules.autoCrystal, v -> modules.autoCrystal = v));
                buttons.add(new ModuleButton("Surround", () -> modules.surround, v -> modules.surround = v));
            } else if (title.equals("Movement")) {
                buttons.add(new ModuleButton("AutoMine", () -> modules.autoMine, v -> modules.autoMine = v));
                buttons.add(new ModuleButton("Scaffold", () -> modules.scaffold, v -> modules.scaffold = v));
            } else if (title.equals("Render")) {
                buttons.add(new ModuleButton("X-Ray", () -> modules.xray, v -> modules.xray = v));
                buttons.add(new ModuleButton("PlayerESP", () -> modules.playerESP, v -> modules.playerESP = v));
                buttons.add(new ModuleButton("FullBright", () -> modules.fullBright, v -> modules.fullBright = v));
            }
        }

        public void render(DrawContext ctx, int mx, int my) {
            ctx.fill(x, y, x + width, y + maxHeight, PANEL_COLOR);
            ctx.drawBorder(x, y, width, maxHeight, new Color(50, 50, 60).getRGB());
            ctx.fill(x, y, x + width, y + headerHeight, HEADER_COLOR);
            ctx.drawTextWithShadow(textRenderer, title, x + 5, y + 5, ACCENT_COLOR);
            
            int cy = y + headerHeight;
            for (ModuleButton b : buttons) { b.render(ctx, mx, my, x, cy, width); cy += 16; }
        }

        public boolean mouseClicked(double mx, double my, int btn) {
            if (mx >= x && mx <= x + width && my >= y && my <= y + headerHeight) { dragging = true; return true; }
            int cy = y + headerHeight;
            for (ModuleButton b : buttons) {
                if (mx >= x && mx <= x + width && my >= cy && my <= cy + 16) { b.toggle(); return true; }
                cy += 16;
            }
            return false;
        }
        public void mouseReleased() { dragging = false; }
    }

    public class ModuleButton {
        public String name; public java.util.function.BooleanSupplier getter; public java.util.function.Consumer<Boolean> setter;
        public ModuleButton(String name, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
            this.name = name; this.getter = getter; this.setter = setter;
        }
        public void render(DrawContext ctx, int mx, int my, int px, int py, int width) {
            boolean enabled = getter.getAsBoolean();
            boolean hovered = mx >= px && mx <= px + width && my >= py && my <= py + 16;
            int color = enabled ? ACCENT_COLOR : (hovered ? 0xFF3C3C46 : 0xFF23232D);
            ctx.fill(px, py, px + width, py + 16, color);
            ctx.drawTextWithShadow(textRenderer, name, px + 4, py + 4, enabled ? 0 : TEXT_COLOR);
        }
        public void toggle() { setter.accept(!getter.getAsBoolean()); }
    }
}
