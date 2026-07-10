package com.xaerocustomcolors.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

@Mixin(value = DropDownWidget.class, remap = false)
public interface DropDownWidgetAccessor {

    @Accessor("realOptions")
    Text[] xcc_getRealOptions();

    @Accessor("realOptions")
    void xcc_setRealOptions(Text[] realOptions);

    @Accessor("options")
    Text[] xcc_getOptions();

    @Accessor("options")
    void xcc_setOptions(Text[] options);
}
