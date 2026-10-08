package com.xaerocustomcolors.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class XcwcSettingsScreen extends Screen {

    private final Screen parent;

    public XcwcSettingsScreen(Screen parent) {
        super(Component.literal("Xaero Custom Waypoint Colors"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Edit Scaling"),
                b -> minecraft.setScreen(ColorPickerScreen.scaleEditor(this)))
                .bounds(width / 2 - 100, height / 4 + 24, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
