package com.xaerocustomcolors.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private static final int PADDING    = 10;
    private static final int PANEL_W    = 210;
    private static final int PICKER_SIZE = PANEL_W - PADDING * 2; // fills panel width
    private static final int HUE_BAR_H  = 14;
    private static final int PANEL_H    = 325;

    private final Screen parent;
    private final Consumer<Integer> callback;

    // HSV state (0-1)
    private float hue = 0f;
    private float sat = 1f;
    private float val = 1f;

    // Layout (set in init)
    private int panelX, panelY;
    private int svX, svY;
    private int hueX, hueY, hueBarW;

    private TextFieldWidget hexField;
    private TextFieldWidget rField, gField, bField;

    private boolean draggingSV  = false;
    private boolean draggingHue = false;
    private boolean updatingFields = false;

    public ColorPickerScreen(Screen parent, int initialArgb, Consumer<Integer> callback) {
        super(Text.literal("Custom Waypoint Color"));
        this.parent   = parent;
        this.callback = callback;
        fromArgb(initialArgb);
    }

    @Override
    protected void init() {
        panelX  = (width  - PANEL_W) / 2;
        panelY  = (height - PANEL_H) / 2;
        svX     = panelX + PADDING;
        svY     = panelY + 24;
        hueX    = panelX + PADDING;
        hueY    = svY + PICKER_SIZE + 6;
        hueBarW = PANEL_W - PADDING * 2;

        int fieldsY = hueY + HUE_BAR_H + 12;

        hexField = new TextFieldWidget(textRenderer, panelX + PADDING, fieldsY, 115, 16,
                Text.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setChangedListener(this::onHexInput);
        addDrawableChild(hexField);

        int compW = 40;
        int compY = fieldsY + 24;
        rField = new TextFieldWidget(textRenderer, panelX + PADDING,          compY, compW, 16, Text.literal("R"));
        gField = new TextFieldWidget(textRenderer, panelX + PADDING + 44,     compY, compW, 16, Text.literal("G"));
        bField = new TextFieldWidget(textRenderer, panelX + PADDING + 88,     compY, compW, 16, Text.literal("B"));
        for (TextFieldWidget f : new TextFieldWidget[]{ rField, gField, bField }) {
            f.setMaxLength(3);
            f.setChangedListener(s -> onRgbInput());
            addDrawableChild(f);
        }

        int btnY = panelY + PANEL_H - 28;
        int btnW = (PANEL_W - PADDING * 3) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("OK"), b -> {
            callback.accept(getCurrentArgb());
            close();
        }).dimensions(panelX + PADDING, btnY, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(panelX + PADDING * 2 + btnW, btnY, btnW, 20).build());

        refreshFields();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Panel background + border
        ctx.fill(panelX,     panelY,     panelX + PANEL_W,     panelY + PANEL_H,     0xDD1A1A1A);
        ctx.fill(panelX,     panelY,     panelX + PANEL_W,     panelY + 1,           0xFFAAAAAA);
        ctx.fill(panelX,     panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFFAAAAAA);
        ctx.fill(panelX,     panelY,     panelX + 1,           panelY + PANEL_H,     0xFFAAAAAA);
        ctx.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H,  0xFFAAAAAA);

        ctx.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_W / 2, panelY + 8, 0xFFFFFF);

        drawSVSquare(ctx);

        int curX = svX + Math.round(sat * (PICKER_SIZE - 1));
        int curY = svY + Math.round((1f - val) * (PICKER_SIZE - 1));
        ctx.fill(curX - 3, curY - 3, curX + 4, curY + 4, 0xFFFFFFFF);
        ctx.fill(curX - 2, curY - 2, curX + 3, curY + 3, 0xFF000000);
        ctx.fill(curX - 1, curY - 1, curX + 2, curY + 2, 0xFFFFFFFF);

        drawHueBar(ctx);

        int hcX = hueX + Math.round(hue * (hueBarW - 1));
        ctx.fill(hcX - 1, hueY - 2, hcX + 2, hueY + HUE_BAR_H + 2, 0xFFFFFFFF);
        ctx.fill(hcX,     hueY - 1, hcX + 1, hueY + HUE_BAR_H + 1, 0xFF000000);

        int swatchX = panelX + PANEL_W - PADDING - 32;
        int swatchY = hueY + HUE_BAR_H + 12;
        ctx.fill(swatchX - 1, swatchY - 1, swatchX + 33, swatchY + 37, 0xFFAAAAAA);
        ctx.fill(swatchX, swatchY, swatchX + 32, swatchY + 36, getCurrentArgb());

        int fieldsY = hueY + HUE_BAR_H + 12;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Hex:"), panelX + PADDING, fieldsY - 9, 0xCCCCCC);
        int compY = fieldsY + 24;
        ctx.drawTextWithShadow(textRenderer, Text.literal("R"), panelX + PADDING + 15, compY - 9, 0xFF8888);
        ctx.drawTextWithShadow(textRenderer, Text.literal("G"), panelX + PADDING + 59, compY - 9, 0x88FF88);
        ctx.drawTextWithShadow(textRenderer, Text.literal("B"), panelX + PADDING + 103, compY - 9, 0x8888FF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static final int STRIP_W = 3;

    private void drawSVSquare(DrawContext ctx) {
        for (int col = 0; col < PICKER_SIZE; col += STRIP_W) {
            float s = (float) col / (PICKER_SIZE - 1);
            int topColor = hsvToArgb(hue, s, 1f);
            int right = Math.min(svX + col + STRIP_W, svX + PICKER_SIZE);
            ctx.fillGradient(svX + col, svY, right, svY + PICKER_SIZE, topColor, 0xFF000000);
        }
    }

    private void drawHueBar(DrawContext ctx) {
        for (int col = 0; col < hueBarW; col += STRIP_W) {
            float h = (float) col / (hueBarW - 1);
            int right = Math.min(hueX + col + STRIP_W, hueX + hueBarW);
            ctx.fill(hueX + col, hueY, right, hueY + HUE_BAR_H, hsvToArgb(h, 1f, 1f));
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean shifted) {
        double mx = click.x(), my = click.y();
        if (click.button() == 0) {
            if (inSV(mx, my)) {
                draggingSV = true;
                applySV(mx, my);
                return true;
            }
            if (inHueBar(mx, my)) {
                draggingHue = true;
                applyHue(mx);
                return true;
            }
        }
        return super.mouseClicked(click, shifted);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingSV)  { applySV(click.x(), click.y());  return true; }
        if (draggingHue) { applyHue(click.x());             return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingSV  = false;
        draggingHue = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() { return true; }

    private boolean inSV(double mx, double my) {
        return mx >= svX && mx < svX + PICKER_SIZE && my >= svY && my < svY + PICKER_SIZE;
    }

    private boolean inHueBar(double mx, double my) {
        return mx >= hueX && mx < hueX + hueBarW && my >= hueY && my < hueY + HUE_BAR_H;
    }

    private void applySV(double mx, double my) {
        sat = clamp01((float)(mx - svX) / (PICKER_SIZE - 1));
        val = clamp01(1f - (float)(my - svY) / (PICKER_SIZE - 1));
        refreshFields();
    }

    private void applyHue(double mx) {
        hue = clamp01((float)(mx - hueX) / (hueBarW - 1));
        refreshFields();
    }

    private void onHexInput(String text) {
        if (updatingFields) return;
        String t = text.startsWith("#") ? text.substring(1) : text;
        if (t.length() == 6) {
            try {
                fromArgb(0xFF000000 | Integer.parseInt(t, 16));
                updatingFields = true;
                int rgb = getCurrentArgb();
                rField.setText(String.valueOf((rgb >> 16) & 0xFF));
                gField.setText(String.valueOf((rgb >>  8) & 0xFF));
                bField.setText(String.valueOf( rgb        & 0xFF));
                updatingFields = false;
            } catch (NumberFormatException ignored) {}
        }
    }

    private void onRgbInput() {
        if (updatingFields) return;
        try {
            int r = clamp255(Integer.parseInt(rField.getText().trim()));
            int g = clamp255(Integer.parseInt(gField.getText().trim()));
            int b = clamp255(Integer.parseInt(bField.getText().trim()));
            fromArgb(0xFF000000 | (r << 16) | (g << 8) | b);
            updatingFields = true;
            hexField.setText(String.format("#%02X%02X%02X", r, g, b));
            updatingFields = false;
        } catch (NumberFormatException ignored) {}
    }

    private void refreshFields() {
        if (updatingFields || hexField == null) return;
        int argb = getCurrentArgb();
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        updatingFields = true;
        hexField.setText(String.format("#%02X%02X%02X", r, g, b));
        rField.setText(String.valueOf(r));
        gField.setText(String.valueOf(g));
        bField.setText(String.valueOf(b));
        updatingFields = false;
    }

    private int getCurrentArgb() {
        return hsvToArgb(hue, sat, val);
    }

    private void fromArgb(int argb) {
        rgbToHsv((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    static int hsvToArgb(float h, float s, float v) {
        float r, g, b;
        if (s == 0f) {
            r = g = b = v;
        } else {
            h = h * 6f;
            int i = (int) h;
            float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
            switch (i % 6) {
                case 0 -> { r = v; g = t; b = p; }
                case 1 -> { r = q; g = v; b = p; }
                case 2 -> { r = p; g = v; b = t; }
                case 3 -> { r = p; g = q; b = v; }
                case 4 -> { r = t; g = p; b = v; }
                default -> { r = v; g = p; b = q; }
            }
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private void rgbToHsv(int ri, int gi, int bi) {
        float r = ri / 255f, g = gi / 255f, b = bi / 255f;
        float mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), d = mx - mn;
        float h = 0f;
        if (d > 0f) {
            if      (mx == r) h = (g - b) / d / 6f + (g < b ? 1f : 0f);
            else if (mx == g) h = (b - r) / d / 6f + 1f / 3f;
            else               h = (r - g) / d / 6f + 2f / 3f;
        }
        this.hue = h;
        this.sat = (mx == 0f) ? 0f : d / mx;
        this.val = mx;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static int   clamp255(int v)  { return Math.max(0, Math.min(255, v)); }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
