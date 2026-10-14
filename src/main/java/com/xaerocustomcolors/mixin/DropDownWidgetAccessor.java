package com.xaerocustomcolors.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

@Mixin(value = DropDownWidget.class, remap = false)
public interface DropDownWidgetAccessor {

    @Accessor("realOptions")
    Component[] xcc_getRealOptions();

    @Accessor("realOptions")
    void xcc_setRealOptions(Component[] realOptions);

    @Accessor("options")
    Component[] xcc_getOptions();

    @Accessor("options")
    void xcc_setOptions(Component[] options);
}
