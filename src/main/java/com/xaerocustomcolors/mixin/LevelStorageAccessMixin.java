package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class LevelStorageAccessMixin {

    @Inject(method = "deleteLevel", at = @At("RETURN"))
    private void xcc_deleteWorldColors(CallbackInfo ci) {
        try {
            LevelStorageSource.LevelStorageAccess self = (LevelStorageSource.LevelStorageAccess) (Object) this;
            CustomColorManager.INSTANCE.deleteContainer(
                    XaeroContext.forWorldFolder(self.getLevelDirectory().directoryName()));
        } catch (Throwable t) {
            XaeroCustomColors.LOGGER.error("[XCWC] Failed to delete custom colors for removed world", t);
        }
    }
}
