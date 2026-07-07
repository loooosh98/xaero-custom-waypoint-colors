package com.xaerocustomcolors.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

@Mixin(value = DropDownWidget.class, remap = false)
public interface DropDownWidgetAccessor {

    @Accessor("realOptions")
    String[] xcc_getRealOptions();

    @Accessor("realOptions")
    void xcc_setRealOptions(String[] realOptions);

    @Accessor("options")
    String[] xcc_getOptions();

    @Accessor("options")
    void xcc_setOptions(String[] options);
}
