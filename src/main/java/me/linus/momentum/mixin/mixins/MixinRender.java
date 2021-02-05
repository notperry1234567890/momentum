package me.linus.momentum.mixin.mixins;

import me.linus.momentum.module.ModuleManager;
import me.linus.momentum.util.client.ColorUtil;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linustouchtips
 * @since 01/04/2021
 */

@Mixin(Render.class)
public class MixinRender<T extends Entity> {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    public void getTeamColor(T entityIn, CallbackInfoReturnable callbackInfo) {
        if (ModuleManager.getModuleByName("ESP").isEnabled()) {
            callbackInfo.cancel();
            callbackInfo.setReturnValue(ColorUtil.getEntityColor(entityIn).getRGB());
        }
    }
}