package com.xaerocustomcolors.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private static final int PADDING    = 10;
    private static final int PANEL_W    = 210;
    private static final int PICKER_SIZE = PANEL_W - PADDING * 2; // fills panel width
    private static final int HUE_BAR_H  = 14;
    private static final int PANEL_H    = 311;
    private static final int[] HUE_STOPS = new int[7];
    static {
        for (int i = 0; i < 7; i++) HUE_STOPS[i] = hsvToArgb(i / 6f, 1f, 1f);
    }

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

    private TextFieldWidget hexField;
    private TextFieldWidget rField, gField, bField;
    private ButtonWidget okButton;

    private boolean hasColor = false;
    private boolean draggingSV  = false;
    private boolean draggingHue = false;
    private boolean updatingFields = false;

    public ColorPickerScreen(Screen parent, int initialArgb, Consumer<Integer> callback) {
        super(Text.literal("Custom Waypoint Color"));
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
        svY     = panelY + PADDING;
        hueX    = panelX + PADDING;
        hueY    = svY + PICKER_SIZE + 6;
        hueBarW = PANEL_W - PADDING * 2;

        int fieldsY = hueY + HUE_BAR_H + 12;

        hexField = new TextFieldWidget(textRenderer, panelX + PADDING, fieldsY, 115, 16,
                Text.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setChangedListener(this::onHexInput);
        hexField.setPlaceholder(Text.literal("#FFFFFF").formatted(Formatting.GRAY));
        addDrawableChild(hexField);

        int compW = 40;
        int compY = fieldsY + 24;
        rField = new TextFieldWidget(textRenderer, panelX + PADDING,          compY, compW, 16, Text.literal("R"));
        gField = new TextFieldWidget(textRenderer, panelX + PADDING + 44,     compY, compW, 16, Text.literal("G"));
        bField = new TextFieldWidget(textRenderer, panelX + PADDING + 88,     compY, compW, 16, Text.literal("B"));
        for (TextFieldWidget f : new TextFieldWidget[]{ rField, gField, bField }) {
            f.setMaxLength(3);
            f.setChangedListener(s -> onRgbInput());
            f.setPlaceholder(Text.literal("255").formatted(Formatting.GRAY));
            addDrawableChild(f);
        }

        int btnY = panelY + PANEL_H - 28;
        int btnW = (PANEL_W - PADDING * 3) / 2;
        okButton = addDrawableChild(ButtonWidget.builder(Text.literal("OK"), b -> {
            callback.accept(getCurrentArgb());
            close();
        }).dimensions(panelX + PADDING, btnY, btnW, 20).build());
        okButton.active = hasColor;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(panelX + PADDING * 2 + btnW, btnY, btnW, 20).build());

        if (hasColor) refreshFields();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);

        // Panel background + border
        ctx.fill(panelX,     panelY,     panelX + PANEL_W,     panelY + PANEL_H,     0xDD1A1A1A);
        ctx.fill(panelX,     panelY,     panelX + PANEL_W,     panelY + 1,           0xFFAAAAAA);
        ctx.fill(panelX,     panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFFAAAAAA);
        ctx.fill(panelX,     panelY,     panelX + 1,           panelY + PANEL_H,     0xFFAAAAAA);
        ctx.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H,  0xFFAAAAAA);

        drawSVSquare(ctx);

        int cur = hasColor ? getCurrentArgb() : 0;
        if (hasColor) {
            int curX = svX + Math.round(sat * (PICKER_SIZE - 1));
            int curY = svY + Math.round((1f - val) * (PICKER_SIZE - 1));
            ctx.fill(curX - 3, curY - 3, curX + 4, curY + 4, 0xFFFFFFFF);
            ctx.fill(curX - 2, curY - 2, curX + 3, curY + 3, 0xFF000000);
            ctx.fill(curX - 1, curY - 1, curX + 2, curY + 2, cur);
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
            ctx.fill(swatchX, swatchY, swatchX + 32, swatchY + 36, cur);
        } else {
            ctx.fill(swatchX - 1, swatchY - 1, swatchX + 33, swatchY,      0xFFAAAAAA);
            ctx.fill(swatchX - 1, swatchY + 36, swatchX + 33, swatchY + 37, 0xFFAAAAAA);
            ctx.fill(swatchX - 1, swatchY,      swatchX,      swatchY + 36, 0xFFAAAAAA);
            ctx.fill(swatchX + 32, swatchY,     swatchX + 33, swatchY + 36, 0xFFAAAAAA);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawSVSquare(DrawContext ctx) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(svX, svY + PICKER_SIZE, 0);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90));
        ctx.fillGradient(0, 0, PICKER_SIZE, PICKER_SIZE, 0xFFFFFFFF, hsvToArgb(hue, 1f, 1f));
        ctx.getMatrices().pop();
        ctx.fillGradient(svX, svY, svX + PICKER_SIZE, svY + PICKER_SIZE, 0x00000000, 0xFF000000);
    }

    private void drawHueBar(DrawContext ctx) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(hueX, hueY + HUE_BAR_H, 0);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90));
        for (int i = 0; i < 6; i++) {
            int y1 = Math.round(i * (hueBarW / 6f));
            int y2 = Math.round((i + 1) * (hueBarW / 6f));
            ctx.fillGradient(0, y1, HUE_BAR_H, y2, HUE_STOPS[i], HUE_STOPS[i + 1]);
        }
        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mouseX, my = mouseY;
        if (button == 0) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSV)  { applySV(mouseX, mouseY);  return true; }
        if (draggingHue) { applyHue(mouseX);          return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSV  = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
        sat = MathHelper.clamp((float)(mx - svX) / (PICKER_SIZE - 1), 0f, 1f);
        val = MathHelper.clamp(1f - (float)(my - svY) / (PICKER_SIZE - 1), 0f, 1f);
        selectColor();
        refreshFields();
    }

    private void applyHue(double mx) {
        hue = MathHelper.clamp((float)(mx - hueX) / (hueBarW - 1), 0f, 1f);
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
                setRgbFields(getCurrentArgb());
                updatingFields = false;
            } catch (NumberFormatException ignored) {}
        }
    }

    private void onRgbInput() {
        if (updatingFields) return;
        try {
            int rRaw = Integer.parseInt(rField.getText().trim());
            int gRaw = Integer.parseInt(gField.getText().trim());
            int bRaw = Integer.parseInt(bField.getText().trim());
            int r = MathHelper.clamp(rRaw, 0, 255);
            int g = MathHelper.clamp(gRaw, 0, 255);
            int b = MathHelper.clamp(bRaw, 0, 255);
            fromArgb(0xFF000000 | (r << 16) | (g << 8) | b);
            selectColor();
            updatingFields = true;
            if (r != rRaw) rField.setText(String.valueOf(r));
            if (g != gRaw) gField.setText(String.valueOf(g));
            if (b != bRaw) bField.setText(String.valueOf(b));
            hexField.setText(String.format("#%02X%02X%02X", r, g, b));
            updatingFields = false;
        } catch (NumberFormatException ignored) {}
    }

    private void refreshFields() {
        if (updatingFields || hexField == null) return;
        int argb = getCurrentArgb();
        updatingFields = true;
        hexField.setText(String.format("#%02X%02X%02X", (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF));
        setRgbFields(argb);
        updatingFields = false;
    }

    private void setRgbFields(int argb) {
        rField.setText(String.valueOf((argb >> 16) & 0xFF));
        gField.setText(String.valueOf((argb >>  8) & 0xFF));
        bField.setText(String.valueOf( argb        & 0xFF));
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

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
