package com.xaerocustomcolors.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.xaerocustomcolors.config.XcwcConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.lwjgl.sdl.SDLMouse;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private static final int PADDING    = 10;
    private static final int PANEL_W    = 210;
    private static final int PICKER_SIZE = PANEL_W - PADDING * 2; // fills panel width
    private static final int HUE_BAR_H  = 14;
    private static final int PANEL_H    = 311;
    private static final float MAX_SCREEN_SHARE = 0.5f;
    private static final float SNAP = 0.05f;
    private static final int SAMPLE_COLOR = 0xFFB877CD;
    private static CursorType resizeCursor;
    private static final int[] HUE_STOPS = new int[7];
    static {
        for (int i = 0; i < 7; i++) HUE_STOPS[i] = hsvToArgb(i / 6f, 1f, 1f);
    }

    private final Screen parent;
    private final Consumer<Integer> callback;
    private final boolean editing;

    private Float share;
    private boolean draggingHandle = false;
    private float grabOffset;

    // HSV values, 0 to 1
    private float hue = 0f;
    private float sat = 1f;
    private float val = 1f;

    // Layout (set in init)
    private float scale = 1f;
    private float offX, offY;
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
        this(parent, initialArgb, callback, false);
    }

    private ColorPickerScreen(Screen parent, int initialArgb, Consumer<Integer> callback, boolean editing) {
        super(Component.literal("Custom Waypoint Color"));
        this.parent   = parent;
        this.callback = callback;
        this.editing  = editing;
        this.share    = XcwcConfig.get().pickerScale;
        hasColor = initialArgb != 0;
        if (hasColor) fromArgb(initialArgb);
    }

    public static ColorPickerScreen scaleEditor(Screen parent) {
        return new ColorPickerScreen(parent, SAMPLE_COLOR, c -> {}, true);
    }

    private static int autoPixels(Window window) {
        int fit = Math.max(1, window.getHeight() / PANEL_H);
        int target = Mth.clamp((int) (window.getHeight() * MAX_SCREEN_SHARE / PANEL_H), Math.min(2, fit), fit);
        return Math.min(window.getGuiScale(), target);
    }

    private static float maxPixels(Window window) {
        return Math.min((float) window.getHeight() / PANEL_H, (float) window.getWidth() / PANEL_W);
    }

    private static float clampPixels(Window window, float px) {
        float max = maxPixels(window);
        return Mth.clamp(px, Math.min(1f, max), max);
    }

    private static float snap(float px, float range) {
        return Math.abs(px - Math.round(px)) < range ? Math.round(px) : px;
    }

    private void updateScale() {
        Window window = minecraft.getWindow();
        float px = share == null
                ? autoPixels(window)
                : snap(clampPixels(window, share * window.getHeight() / PANEL_H), 0.01f);
        scale = px / window.getGuiScale();
        offX  = (width  / scale - PANEL_W) / 2f;
        offY  = (height / scale - PANEL_H) / 2f;
        if (px == Math.round(px)) {
            offX = Mth.floor(offX);
            offY = Mth.floor(offY);
        }
    }

    private MouseButtonEvent toView(MouseButtonEvent click) {
        return new MouseButtonEvent(click.x() / scale - offX, click.y() / scale - offY, click.buttonInfo());
    }

    @Override
    protected void init() {
        updateScale();
        svX     = PADDING;
        svY     = PADDING;
        hueX    = PADDING;
        hueY    = svY + PICKER_SIZE + 6;
        hueBarW = PANEL_W - PADDING * 2;

        int fieldsY = hueY + HUE_BAR_H + 12;

        hexField = new EditBox(font, PADDING, fieldsY, 115, 16,
                Component.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setResponder(this::onHexInput);
        hexField.setHint(Component.literal("#FFFFFF").withStyle(EditBox.DEFAULT_HINT_STYLE));
        addRenderableWidget(hexField);

        int compW = 40;
        int compY = fieldsY + 24;
        rField = new EditBox(font, PADDING,          compY, compW, 16, Component.literal("R"));
        gField = new EditBox(font, PADDING + 44,     compY, compW, 16, Component.literal("G"));
        bField = new EditBox(font, PADDING + 88,     compY, compW, 16, Component.literal("B"));
        for (EditBox f : new EditBox[]{ rField, gField, bField }) {
            f.setMaxLength(3);
            f.setResponder(s -> onRgbInput());
            f.setHint(Component.literal("255").withStyle(EditBox.DEFAULT_HINT_STYLE));
            addRenderableWidget(f);
        }

        int btnY = PANEL_H - 28;
        if (editing) {
            addEditorButtons(btnY);
        } else {
            int btnW = (PANEL_W - PADDING * 3) / 2;
            okButton = addRenderableWidget(Button.builder(Component.literal("OK"), b -> {
                callback.accept(getCurrentArgb());
                onClose();
            }).bounds(PADDING, btnY, btnW, 20).build());
            okButton.active = hasColor;
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds(PADDING * 2 + btnW, btnY, btnW, 20).build());
        }

        if (hasColor) refreshFields();
    }

    private void addEditorButtons(int btnY) {
        for (EditBox f : new EditBox[]{ hexField, rField, gField, bField }) f.active = false;

        int btnW = 56;
        int gap  = (PANEL_W - PADDING * 2 - btnW * 3) / 2;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            XcwcConfig config = XcwcConfig.get();
            config.pickerScale = share;
            config.save();
            onClose();
        }).bounds(PADDING, btnY, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            share = null;
            updateScale();
        }).bounds(PADDING + btnW + gap, btnY, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(PADDING + (btnW + gap) * 2, btnY, btnW, 20).build());
    }

    private int handlePixels() {
        return Math.max(6, minecraft.getWindow().getHeight() / 180);
    }

    private float handleReach() {
        return (float) handlePixels() / minecraft.getWindow().getGuiScale();
    }

    private float handleX() {
        return Math.min((offX + PANEL_W) * scale, width - handleReach());
    }

    private float handleY() {
        return Math.min((offY + PANEL_H) * scale, height - handleReach());
    }

    private boolean onHandle(double mx, double my) {
        float reach = handleReach() * 1.5f;
        return Math.abs(mx - handleX()) <= reach && Math.abs(my - handleY()) <= reach;
    }

    private static CursorType resizeCursor() {
        if (resizeCursor == null) {
            resizeCursor = CursorType.createStandardCursor(
                    SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "resize_nwse", CursorTypes.RESIZE_ALL);
        }
        return resizeCursor;
    }

    private float scaleAt(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        return (float) ((dx * PANEL_W + dy * PANEL_H) * 2 / (PANEL_W * PANEL_W + PANEL_H * PANEL_H));
    }

    private void resizeTo(double mx, double my) {
        Window window = minecraft.getWindow();
        float px = snap(clampPixels(window, (scaleAt(mx, my) + grabOffset) * window.getGuiScale()), SNAP);
        share = px * PANEL_H / window.getHeight();
        updateScale();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);
        ctx.pose().translate(offX, offY);

        // Panel background + border
        ctx.fill(1,           1,           PANEL_W - 1, PANEL_H - 1, 0xDD1A1A1A);
        ctx.fill(0,           0,           PANEL_W,     1,           0xFFAAAAAA);
        ctx.fill(0,           PANEL_H - 1, PANEL_W,     PANEL_H,     0xFFAAAAAA);
        ctx.fill(0,           0,           1,           PANEL_H,     0xFFAAAAAA);
        ctx.fill(PANEL_W - 1, 0,           PANEL_W,     PANEL_H,     0xFFAAAAAA);

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

        int swatchX = PANEL_W - PADDING - 32;
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

        super.extractRenderState(ctx, Mth.floor(mouseX / scale - offX), Mth.floor(mouseY / scale - offY), delta);
        ctx.pose().popMatrix();

        if (editing) {
            int guiScale = minecraft.getWindow().getGuiScale();
            int half = handlePixels(), rim = Math.max(1, half / 4);
            int hx = Math.round(handleX() * guiScale), hy = Math.round(handleY() * guiScale);
            ctx.pose().pushMatrix();
            ctx.pose().scale(1f / guiScale, 1f / guiScale);
            ctx.fill(hx - half - rim, hy - half - rim, hx + half + rim, hy + half + rim, 0xFFFFFFFF);
            ctx.fill(hx - half,       hy - half,       hx + half,       hy + half,       0xFFE03030);
            ctx.pose().popMatrix();
            if (draggingHandle || onHandle(mouseX, mouseY)) ctx.requestCursor(resizeCursor());
        }
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
            ctx.fillGradient(0, y1, HUE_BAR_H, y2, HUE_STOPS[i], HUE_STOPS[i + 1]);
        }
        ctx.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean shifted) {
        if (editing) {
            if (click.button() == InputConstants.MOUSE_BUTTON_LEFT && onHandle(click.x(), click.y())) {
                draggingHandle = true;
                grabOffset = scale - scaleAt(click.x(), click.y());
                return true;
            }
            return super.mouseClicked(toView(click), shifted);
        }
        click = toView(click);
        double mx = click.x(), my = click.y();
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
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
        if (draggingHandle) { resizeTo(click.x(), click.y()); return true; }
        click = toView(click);
        if (draggingSV)  { applySV(click.x(), click.y());  return true; }
        if (draggingHue) { applyHue(click.x());             return true; }
        return super.mouseDragged(click, dx / scale, dy / scale);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingSV  = false;
        draggingHue = false;
        draggingHandle = false;
        return super.mouseReleased(toView(click));
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
        sat = Mth.clamp((float)(mx - svX) / (PICKER_SIZE - 1), 0f, 1f);
        val = Mth.clamp(1f - (float)(my - svY) / (PICKER_SIZE - 1), 0f, 1f);
        selectColor();
        refreshFields();
    }

    private void applyHue(double mx) {
        hue = Mth.clamp((float)(mx - hueX) / (hueBarW - 1), 0f, 1f);
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
                fromArgb(ARGB.opaque(Integer.parseInt(t, 16)));
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
            int rRaw = Integer.parseInt(rField.getValue().trim());
            int gRaw = Integer.parseInt(gField.getValue().trim());
            int bRaw = Integer.parseInt(bField.getValue().trim());
            int r = Mth.clamp(rRaw, 0, 255);
            int g = Mth.clamp(gRaw, 0, 255);
            int b = Mth.clamp(bRaw, 0, 255);
            fromArgb(ARGB.color(r, g, b));
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
        updatingFields = true;
        hexField.setValue(String.format("#%02X%02X%02X", ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb)));
        setRgbFields(argb);
        updatingFields = false;
    }

    private void setRgbFields(int argb) {
        rField.setValue(String.valueOf(ARGB.red(argb)));
        gField.setValue(String.valueOf(ARGB.green(argb)));
        bField.setValue(String.valueOf(ARGB.blue(argb)));
    }

    private int getCurrentArgb() {
        return hsvToArgb(hue, sat, val);
    }

    private void fromArgb(int argb) {
        rgbToHsv(ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb));
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
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
