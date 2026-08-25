package com.xaerocustomcolors.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

@Mixin(value = DropDownWidget.class, remap = false)
public interface DropDownWidgetAccessor {

    @Accessor("realOptions")
    Component[] xcwc_getRealOptions();

    @Accessor("realOptions")
    void xcwc_setRealOptions(Component[] realOptions);

    @Accessor("options")
    Component[] xcwc_getOptions();

    @Accessor("options")
    void xcwc_setOptions(Component[] options);
}
