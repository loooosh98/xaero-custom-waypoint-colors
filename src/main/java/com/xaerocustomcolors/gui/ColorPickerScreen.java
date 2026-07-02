package com.xaerocustomcolors.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private static final int PADDING    = 10;
    private static final int PANEL_W    = 210;
    private static final int PICKER_SIZE = PANEL_W - PADDING * 2; // fills panel width
    private static final int HUE_BAR_H  = 14;
    private static final int PANEL_H    = 325;

    private final Screen parent;
    private final Consumer<Integer> callback;

    // HSV values, 0 to 1
    private float hue = 0f;
    private float sat = 1f;
    private float val = 1f;

    // Layout (set in init)
    private int panelX, panelY;
    private int svX, svY;
    private int hueX, hueY, hueBarW;

    private EditBox hexField;
    private EditBox rField, gField, bField;
    private Button okButton;

    private boolean hasColor = false;
    private boolean draggingSV  = false;
    private boolean draggingHue = false;
    private boolean updatingFields = false;

    public ColorPickerScreen(Screen parent, int initialArgb, Consumer<Integer> callback) {
        super(Component.literal("Custom Waypoint Color"));
        this.parent   = parent;
        this.callback = callback;
        hasColor = initialArgb != 0;
        if (hasColor) fromArgb(initialArgb);
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

        hexField = new EditBox(font, panelX + PADDING, fieldsY, 115, 16,
                Component.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setResponder(this::onHexInput);
        hexField.setHint(Component.literal("#FFFFFF").withStyle(EditBox.DEFAULT_HINT_STYLE));
        addRenderableWidget(hexField);

        int compW = 40;
        int compY = fieldsY + 24;
        rField = new EditBox(font, panelX + PADDING,          compY, compW, 16, Component.literal("R"));
        gField = new EditBox(font, panelX + PADDING + 44,     compY, compW, 16, Component.literal("G"));
        bField = new EditBox(font, panelX + PADDING + 88,     compY, compW, 16, Component.literal("B"));
        for (EditBox f : new EditBox[]{ rField, gField, bField }) {
            f.setMaxLength(3);
            f.setResponder(s -> onRgbInput());
            f.setHint(Component.literal("255").withStyle(EditBox.DEFAULT_HINT_STYLE));
            addRenderableWidget(f);
        }

        int btnY = panelY + PANEL_H - 28;
        int btnW = (PANEL_W - PADDING * 3) / 2;
        okButton = addRenderableWidget(Button.builder(Component.literal("OK"), b -> {
            callback.accept(getCurrentArgb());
            onClose();
        }).bounds(panelX + PADDING, btnY, btnW, 20).build());
        okButton.active = hasColor;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(panelX + PADDING * 2 + btnW, btnY, btnW, 20).build());

        if (hasColor) refreshFields();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        // Panel background + border
        ctx.fill(panelX,     panelY,     panelX + PANEL_W,     panelY + PANEL_H,     0xDD1A1A1A);
        ctx.fill(panelX,     panelY,     panelX + PANEL_W,     panelY + 1,           0xFFAAAAAA);
        ctx.fill(panelX,     panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFFAAAAAA);
        ctx.fill(panelX,     panelY,     panelX + 1,           panelY + PANEL_H,     0xFFAAAAAA);
        ctx.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H,  0xFFAAAAAA);

        drawSVSquare(ctx);

        if (hasColor) {
            int curX = svX + Math.round(sat * (PICKER_SIZE - 1));
            int curY = svY + Math.round((1f - val) * (PICKER_SIZE - 1));
            ctx.fill(curX - 3, curY - 3, curX + 4, curY + 4, 0xFFFFFFFF);
            ctx.fill(curX - 2, curY - 2, curX + 3, curY + 3, 0xFF000000);
            ctx.fill(curX - 1, curY - 1, curX + 2, curY + 2, getCurrentArgb());
        }

        drawHueBar(ctx);

        if (hasColor) {
            int hcX = hueX + Math.round(hue * (hueBarW - 1));
            ctx.fill(hcX - 1, hueY - 2, hcX + 2, hueY + HUE_BAR_H + 2, 0xFFFFFFFF);
            ctx.fill(hcX,     hueY - 1, hcX + 1, hueY + HUE_BAR_H + 1, 0xFF000000);
        }

        int swatchX = panelX + PANEL_W - PADDING - 32;
        int swatchY = hueY + HUE_BAR_H + 12;
        if (hasColor) {
            ctx.fill(swatchX - 1, swatchY - 1, swatchX + 33, swatchY + 37, 0xFFAAAAAA);
            ctx.fill(swatchX, swatchY, swatchX + 32, swatchY + 36, getCurrentArgb());
        } else {
            ctx.fill(swatchX - 1, swatchY - 1, swatchX + 33, swatchY,      0xFFAAAAAA);
            ctx.fill(swatchX - 1, swatchY + 36, swatchX + 33, swatchY + 37, 0xFFAAAAAA);
            ctx.fill(swatchX - 1, swatchY,      swatchX,      swatchY + 36, 0xFFAAAAAA);
            ctx.fill(swatchX + 32, swatchY,     swatchX + 33, swatchY + 36, 0xFFAAAAAA);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void drawSVSquare(GuiGraphicsExtractor ctx) {
        ctx.pose().pushMatrix();
        ctx.pose().translate(svX, svY + PICKER_SIZE);
        ctx.pose().rotate((float) (-Math.PI / 2));
        ctx.fillGradient(0, 0, PICKER_SIZE, PICKER_SIZE, 0xFFFFFFFF, hsvToArgb(hue, 1f, 1f));
        ctx.pose().popMatrix();
        ctx.fillGradient(svX, svY, svX + PICKER_SIZE, svY + PICKER_SIZE, 0x00000000, 0xFF000000);
    }

    private void drawHueBar(GuiGraphicsExtractor ctx) {
        ctx.pose().pushMatrix();
        ctx.pose().translate(hueX, hueY + HUE_BAR_H);
        ctx.pose().rotate((float) (-Math.PI / 2));
        for (int i = 0; i < 6; i++) {
            int y1 = Math.round(i * (hueBarW / 6f));
            int y2 = Math.round((i + 1) * (hueBarW / 6f));
            ctx.fillGradient(0, y1, HUE_BAR_H, y2,
                    hsvToArgb(i / 6f, 1f, 1f), hsvToArgb((i + 1) / 6f, 1f, 1f));
        }
        ctx.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean shifted) {
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
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingSV)  { applySV(click.x(), click.y());  return true; }
        if (draggingHue) { applyHue(click.x());             return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingSV  = false;
        draggingHue = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean isPauseScreen() { return true; }

    private boolean inSV(double mx, double my) {
        return mx >= svX && mx < svX + PICKER_SIZE && my >= svY && my < svY + PICKER_SIZE;
    }

    private boolean inHueBar(double mx, double my) {
        return mx >= hueX && mx < hueX + hueBarW && my >= hueY && my < hueY + HUE_BAR_H;
    }

    private void applySV(double mx, double my) {
        sat = clamp01((float)(mx - svX) / (PICKER_SIZE - 1));
        val = clamp01(1f - (float)(my - svY) / (PICKER_SIZE - 1));
        selectColor();
        refreshFields();
    }

    private void applyHue(double mx) {
        hue = clamp01((float)(mx - hueX) / (hueBarW - 1));
        selectColor();
        refreshFields();
    }

    private void selectColor() {
        hasColor = true;
        if (okButton != null) okButton.active = true;
    }

    private void onHexInput(String text) {
        if (updatingFields) return;
        String t = text.startsWith("#") ? text.substring(1) : text;
        if (t.length() == 6) {
            try {
                fromArgb(0xFF000000 | Integer.parseInt(t, 16));
                selectColor();
                updatingFields = true;
                int rgb = getCurrentArgb();
                rField.setValue(String.valueOf((rgb >> 16) & 0xFF));
                gField.setValue(String.valueOf((rgb >>  8) & 0xFF));
                bField.setValue(String.valueOf( rgb        & 0xFF));
                updatingFields = false;
            } catch (NumberFormatException ignored) {}
        }
    }

    private void onRgbInput() {
        if (updatingFields) return;
        try {
            int rRaw = Integer.parseInt(rField.getValue().trim());
            int gRaw = Integer.parseInt(gField.getValue().trim());
            int bRaw = Integer.parseInt(bField.getValue().trim());
            int r = clamp255(rRaw);
            int g = clamp255(gRaw);
            int b = clamp255(bRaw);
            fromArgb(0xFF000000 | (r << 16) | (g << 8) | b);
            selectColor();
            updatingFields = true;
            if (r != rRaw) rField.setValue(String.valueOf(r));
            if (g != gRaw) gField.setValue(String.valueOf(g));
            if (b != bRaw) bField.setValue(String.valueOf(b));
            hexField.setValue(String.format("#%02X%02X%02X", r, g, b));
            updatingFields = false;
        } catch (NumberFormatException ignored) {}
    }

    private void refreshFields() {
        if (updatingFields || hexField == null) return;
        int argb = getCurrentArgb();
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        updatingFields = true;
        hexField.setValue(String.format("#%02X%02X%02X", r, g, b));
        rField.setValue(String.valueOf(r));
        gField.setValue(String.valueOf(g));
        bField.setValue(String.valueOf(b));
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
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
