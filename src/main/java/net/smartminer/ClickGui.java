package net.smartminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {

    public static Modules modules;
    
    // Цвета интерфейса
    public static final int ACCENT_COLOR = new Color(0, 255, 180).getRGB();
    public static final int BG_COLOR = new Color(15, 15, 20, 220).getRGB();
    public static final int PANEL_COLOR = new Color(25, 25, 30, 255).getRGB();
    public static final int HEADER_COLOR = new Color(35, 35, 45, 255).getRGB();
    public static final int TEXT_COLOR = new Color(220, 220, 220).getRGB();
    public static final int TEXT_DARK = new Color(150, 150, 150).getRGB();

    private final List<Panel> panels = new ArrayList<>();
    private Panel draggingPanel = null;
    private double dragX, dragY;

    public ClickGui() {
        super(Text.literal("SmartMiner GUI"));
        if (modules == null) modules = SmartClient.modules;

        // Инициализация панелей (координаты X, Y)
        int x = 20;
        panels.add(new Panel("Combat", x, 20));
        panels.add(new Panel("Movement", x + 140, 20));
        panels.add(new Panel("Render", x + 280, 20));
        panels.add(new Panel("Player", x + 420, 20));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Стандартный фон игры
        super.render(context, mouseX, mouseY, delta);

        // 2. Затемнение экрана
        context.fill(0, 0, this.width, this.height, new Color(0, 0, 0, 120).getRGB());

        // 3. Отрисовка панелей
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY, delta);
        }

        // 4. Watermark (Логотип)
        String watermark = "SmartMiner v5.0 Delta";
        int wmWidth = this.textRenderer.getWidth(watermark);
        context.fill(10, 10, 10 + wmWidth + 10, 25, BG_COLOR);
        context.drawBorder(10, 10, wmWidth + 10, 15, ACCENT_COLOR);
        context.drawTextWithShadow(this.textRenderer, watermark, 15, 14, ACCENT_COLOR);

        // 5. ArrayList (Список активных модулей)
        renderArrayList(context);
    }

    private void renderArrayList(DrawContext context) {
        List<String> active = new ArrayList<>();
        if (modules.killAura) active.add("KillAura (" + modules.killAuraMode + ")");
        if (modules.autoMine) active.add("AutoMine");
        if (modules.autoCrystal) active.add("AutoCrystal");
        if (modules.scaffold) active.add("Scaffold");
        if (modules.surround) active.add("Surround");
        if (modules.reach) active.add("Reach");
        if (modules.velocity) active.add("Velocity");
        if (modules.autoSwap) active.add("AutoSwap");
        if (modules.xray) active.add("X-Ray");
        if (modules.blockESP) active.add("BlockESP");
        if (modules.entityBox) active.add("EntityBox");
        if (modules.playerESP) active.add("PlayerESP");

        // Сортировка по ширине текста (чтобы было красиво)
        active.sort((a, b) -> textRenderer.getWidth(b) - textRenderer.getWidth(a));

        int y = 25;
        for (String s : active) {
            int width = textRenderer.getWidth(s) + 8;
            int x = this.width - width - 5;
            
            // Фон
            context.fill(x, y, this.width - 5, y + 12, new Color(20, 20, 25, 200).getRGB());
            // Полоска слева
            context.fill(x, y, x + 2, y + 12, ACCENT_COLOR);
            // Текст (белый)
            context.drawTextWithShadow(textRenderer, s, x + 5, y + 2, -1); 
            y += 14;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // ЛКМ
            for (Panel panel : panels) {
                if (panel.mouseClicked(mouseX, mouseY, button)) {
                    if (panel.isDraggingHeader) {
                        draggingPanel = panel;
                        dragX = mouseX - panel.x;
                        dragY = mouseY - panel.y;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanel = null;
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Перетаскивание панели
        if (draggingPanel != null) {
            draggingPanel.x = (int) (mouseX - dragX);
            draggingPanel.y = (int) (mouseY - dragY);
            return true;
        }
        
        // Обновление ползунков (слайдеров) при перетаскивании мыши
        for (Panel panel : panels) {
            for (ModuleButton btn : panel.buttons) {
                if (btn.expanded && btn.animation > 0.9f) {
                    for (int i = 0; i < btn.settings.size(); i++) {
                        SettingComponent s = btn.settings.get(i);
                        if (s.dragging && s.type == SettingType.SLIDER) {
                            int px = panel.x;
                            int sy = panel.y + panel.headerHeight - panel.scroll;
                            for (ModuleButton b : panel.buttons) {
                                if (b == btn) break;
                                sy += b.getHeight();
                            }
                            sy += 16;
                            for (int j = 0; j < i; j++) sy += 14;
                            s.updateValue(mouseX, px, panel.width);
                        }
                    }
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Panel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ==========================================
    // КЛАСС ПАНЕЛИ (Combat, Render и т.д.)
    // ==========================================
    public class Panel {
        public String title;
        public int x, y;
        public int width = 130; // Увеличили ширину, чтобы влезали названия
        public int headerHeight = 18;
        public int scroll = 0;
        public int maxHeight = 250; // Максимальная высота панели

        public List<ModuleButton> buttons = new ArrayList<>();
        public boolean isDraggingHeader = false;
        public boolean extended = true;

        public Panel(String title, int x, int y) {
            this.title = title;
            this.x = x;
            this.y = y;
            initButtons();
        }

        private void initButtons() {
            switch (title) {
                case "Combat":
                    buttons.add(new ModuleButton("KillAura", this, () -> modules.killAura, v -> modules.killAura = v)
                            .addSlider("Range", () -> modules.killAuraRange, v -> modules.killAuraRange = v, 1f, 6f)
                            .addMode("Mode", () -> modules.killAuraMode.equals("FunTime") ? 1f : 0f, v -> modules.killAuraMode = v > 0.5f ? "FunTime" : "Legit"));
                    buttons.add(new ModuleButton("Reach", this, () -> modules.reach, v -> modules.reach = v)
                            .addSlider("Dist", () -> modules.reachDistance, v -> modules.reachDistance = v, 3f, 6f));
                    buttons.add(new ModuleButton("Velocity", this, () -> modules.velocity, v -> modules.velocity = v)
                            .addSlider("Percent", () -> modules.velocityPercent * 100f, v -> modules.velocityPercent = v / 100f, 0f, 100f));
                    buttons.add(new ModuleButton("AutoSwap", this, () -> modules.autoSwap, v -> modules.autoSwap = v));
                    buttons.add(new ModuleButton("AutoCrystal", this, () -> modules.autoCrystal, v -> modules.autoCrystal = v));
                    buttons.add(new ModuleButton("Surround", this, () -> modules.surround, v -> modules.surround = v));
                    break;
                case "Movement":
                    buttons.add(new ModuleButton("AutoMine", this, () -> modules.autoMine, v -> modules.autoMine = v)
                            .addSlider("MinHP", () -> (float)modules.minHpToMine, v -> modules.minHpToMine = v.intValue(), 1f, 20f)
                            .addSlider("Priority", () -> (float)modules.minOrePriority, v -> modules.minOrePriority = v.intValue(), 0f, 100f));
                    buttons.add(new ModuleButton("Scaffold", this, () -> modules.scaffold, v -> modules.scaffold = v));
                    buttons.add(new ModuleButton("Sprint", this, () -> modules.sprint, v -> modules.sprint = v));
                    buttons.add(new ModuleButton("NoFall", this, () -> modules.noFall, v -> modules.noFall = v));
                    break;
                case "Render":
                    buttons.add(new ModuleButton("X-Ray", this, () -> modules.xray, v -> modules.xray = v));
                    buttons.add(new ModuleButton("BlockESP", this, () -> modules.blockESP, v -> modules.blockESP = v));
                    buttons.add(new ModuleButton("EntityBox", this, () -> modules.entityBox, v -> modules.entityBox = v));
                    buttons.add(new ModuleButton("PlayerESP", this, () -> modules.playerESP, v -> modules.playerESP = v));
                    buttons.add(new ModuleButton("Tracers", this, () -> modules.tracers, v -> modules.tracers = v));
                    buttons.add(new ModuleButton("FullBright", this, () -> modules.fullBright, v -> modules.fullBright = v));
                    break;
                case "Player":
                    buttons.add(new ModuleButton("AutoTotem", this, () -> modules.autoTotem, v -> modules.autoTotem = v));
                    buttons.add(new ModuleButton("AutoArmor", this, () -> modules.autoArmor, v -> modules.autoArmor = v));
                    buttons.add(new ModuleButton("AutoEat", this, () -> modules.autoEat, v -> modules.autoEat = v));
                    break;
            }
        }

        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            int totalHeight = headerHeight;
            for (ModuleButton b : buttons) totalHeight += b.getHeight();
            
            // Отрисовка фона панели
            context.fill(x, y, x + width, y + maxHeight, PANEL_COLOR);
            context.drawBorder(x, y, width, maxHeight, new Color(50, 50, 60).getRGB());
            
            // Заголовок панели
            context.fill(x, y, x + width, y + headerHeight, HEADER_COLOR);
            context.drawTextWithShadow(textRenderer, title, x + 5, y + 5, ACCENT_COLOR);
            
            // Кнопка свернуть/развернуть
            context.drawTextWithShadow(textRenderer, extended ? "-" : "+", x + width - 12, y + 5, TEXT_COLOR);

            // Отрисовка кнопок внутри панели (со скроллом)
            if (extended) {
                context.enableScissor(x, y + headerHeight, x + width, y + maxHeight);
                int cy = y + headerHeight - scroll;
                for (ModuleButton b : buttons) {
                    b.render(context, mouseX, mouseY, x, cy, width);
                    cy += b.getHeight();
                }
                context.disableScissor();
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            isDraggingHeader = false;
            
            // Клик по заголовку
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (mouseX >= x + width - 15) {
                    extended = !extended;
                } else {
                    isDraggingHeader = true;
                }
                return true;
            }

            // Клик по кнопкам модулей
            if (extended && mouseX >= x && mouseX <= x + width && mouseY > y + headerHeight && mouseY < y + maxHeight) {
                int cy = y + headerHeight - scroll;
                for (ModuleButton b : buttons) {
                    if (mouseY >= cy && mouseY <= cy + b.getHeight()) {
                        if (b.mouseClicked(mouseX, mouseY, button, x, cy, width)) return true;
                    }
                    cy += b.getHeight();
                }
            }
            return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
            isDraggingHeader = false;
            for (ModuleButton b : buttons) b.mouseReleased(mouseX, mouseY, button);
        }

        public void mouseScrolled(double mouseX, double mouseY, double amount) {
            if (mouseX >= x && mouseX <= x + width && mouseY > y + headerHeight && mouseY < y + maxHeight) {
                int totalHeight = 0;
                for (ModuleButton b : buttons) totalHeight += b.getHeight();
                int maxScroll = Math.max(0, totalHeight - maxHeight);
                
                scroll -= (int) (amount * 15);
                if (scroll < 0) scroll = 0;
                if (scroll > maxScroll) scroll = maxScroll;
            }
        }
    }

    // ==========================================
    // КЛАСС КНОПКИ МОДУЛЯ
    // ==========================================
    public class ModuleButton {
        public String name;
        public Panel parent;
        public java.util.function.BooleanSupplier getter;
        public java.util.function.Consumer<Boolean> setter;
        public List<SettingComponent> settings = new ArrayList<>();
        public boolean expanded = false;
        public float animation = 0f;

        public ModuleButton(String name, Panel parent, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
            this.name = name;
            this.parent = parent;
            this.getter = getter;
            this.setter = setter;
        }

        public ModuleButton addSlider(String name, java.util.function.Supplier<Float> getter, java.util.function.Consumer<Float> setter, float min, float max) {
            settings.add(new SettingComponent(name, SettingType.SLIDER, getter, setter, min, max));
            return this;
        }

        public ModuleButton addMode(String name, java.util.function.Supplier<Float> getter, java.util.function.Consumer<Float> setter) {
            settings.add(new SettingComponent(name, SettingType.MODE, getter, setter, 0, 1));
            return this;
        }

        public int getHeight() {
            return 16 + (int) (animation * (settings.size() * 14));
        }

        public void render(DrawContext context, int mouseX, int mouseY, int px, int py, int width) {
            boolean hovered = mouseX >= px && mouseX <= px + width && mouseY >= py && mouseY <= py + 16;
            boolean enabled = getter.getAsBoolean();

            // Анимация раскрытия
            float target = expanded ? 1f : 0f;
            animation = MathHelper.lerp(0.2f, animation, target);

            // Цвет кнопки (зеленый если вкл, темный если выкл)
            int color = enabled ? ACCENT_COLOR : (hovered ? new Color(60, 60, 70).getRGB() : new Color(35, 35, 45).getRGB());
            context.fill(px, py, px + width, py + 16, color);
            context.drawBorder(px, py, width, 16, new Color(50, 50, 60).getRGB());

            // ИСПРАВЛЕНО: Текст черный если вкл, светлый если выкл
            int textColor = enabled ? 0xFF000000 : TEXT_COLOR;
            context.drawTextWithShadow(textRenderer, name, px + 4, py + 4, textColor);

            // Стрелочка для настроек
            if (!settings.isEmpty()) {
                context.drawTextWithShadow(textRenderer, expanded ? "v" : ">", px + width - 10, py + 4, textColor);
            }

            // Отрисовка настроек (если открыты)
            if (animation > 0.01f) {
                context.enableScissor(px, py + 16, px + width, py + 16 + (int) (animation * (settings.size() * 14)));
                int sy = py + 16;
                for (SettingComponent s : settings) {
                    s.render(context, mouseX, mouseY, px, sy, width);
                    sy += (int)(14 * animation);
                }
                context.disableScissor();
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button, int px, int py, int width) {
            // Клик по самой кнопке модуля
            if (mouseX >= px && mouseX <= px + width && mouseY >= py && mouseY <= py + 16) {
                if (button == 0) {
                    setter.accept(!getter.getAsBoolean()); // ЛКМ - включить/выключить
                } else if (button == 1 && !settings.isEmpty()) {
                    expanded = !expanded; // ПКМ - открыть настройки
                }
                return true;
            }

            // Клик по настройкам
            if (expanded && animation > 0.9f) {
                int sy = py + 16;
                for (SettingComponent s : settings) {
                    if (s.mouseClicked(mouseX, mouseY, button, px, sy, width)) return true;
                    sy += 14;
                }
            }
            return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
            for (SettingComponent s : settings) s.mouseReleased(mouseX, mouseY, button);
        }
    }

    // ==========================================
    // КЛАСС НАСТРОЙКИ (СЛАЙДЕР / ПЕРЕКЛЮЧАТЕЛЬ)
    // ==========================================
    public enum SettingType { SLIDER, MODE }

    public class SettingComponent {
        public String name;
        public SettingType type;
        public java.util.function.Supplier<Float> getter;
        public java.util.function.Consumer<Float> setter;
        public float min, max;
        public boolean dragging = false;

        public SettingComponent(String name, SettingType type, java.util.function.Supplier<Float> getter, java.util.function.Consumer<Float> setter, float min, float max) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
        }

        public void render(DrawContext context, int mouseX, int mouseY, int px, int py, int width) {
            float value = getter.get();

            if (type == SettingType.SLIDER) {
                context.drawTextWithShadow(textRenderer, name, px + 8, py + 2, TEXT_DARK);
                
                int sliderY = py + 10;
                int sliderWidth = width - 16;
                float percent = (value - min) / (max - min);
                percent = MathHelper.clamp(percent, 0f, 1f);

                // Фон и заполнение
                context.fill(px + 8, sliderY, px + 8 + sliderWidth, sliderY + 2, new Color(50, 50, 60).getRGB());
                context.fill(px + 8, sliderY, px + 8 + (int)(sliderWidth * percent), sliderY + 2, ACCENT_COLOR);
                // Кружок
                context.fill(px + 8 + (int)(sliderWidth * percent) - 2, sliderY - 2, px + 8 + (int)(sliderWidth * percent) + 2, sliderY + 4, -1);
                
                // Текст значения
                String valStr = String.format("%.1f", value);
                context.drawTextWithShadow(textRenderer, valStr, px + width - 22, py + 2, TEXT_COLOR);

            } else if (type == SettingType.MODE) {
                // Режим (например, FunTime / Legit)
                context.drawTextWithShadow(textRenderer, name, px + 8, py + 2, TEXT_DARK);
                
                boolean isMode1 = value > 0.5f;
                String displayMode = isMode1 ? "FunTime" : "Legit";
                int modeColor = isMode1 ? new Color(255, 100, 100).getRGB() : new Color(100, 255, 100).getRGB();
                
                context.drawTextWithShadow(textRenderer, displayMode, px + width - textRenderer.getWidth(displayMode) - 8, py + 2, modeColor);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button, int px, int py, int width) {
            if (button != 0) return false;

            if (type == SettingType.SLIDER) {
                int sliderY = py + 10;
                if (mouseY >= sliderY - 4 && mouseY <= sliderY + 4 && mouseX >= px + 8 && mouseX <= px + width - 8) {
                    dragging = true;
                    updateValue(mouseX, px, width);
                    return true;
                }
            } else if (type == SettingType.MODE) {
                // Клик по всему полю переключает режим
                if (mouseY >= py && mouseY <= py + 12) {
                    setter.accept(getter.get() > 0.5f ? 0f : 1f);
                    return true;
                }
            }
            return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
            dragging = false;
        }
        
        public void updateValue(double mouseX, int px, int width) {
            if (!dragging) return;
            int sliderWidth = width - 16;
            float percent = (float)(mouseX - (px + 8)) / sliderWidth;
            percent = MathHelper.clamp(percent, 0f, 1f);
            float val = min + (max - min) * percent;
            setter.accept(val);
        }
    }
}
