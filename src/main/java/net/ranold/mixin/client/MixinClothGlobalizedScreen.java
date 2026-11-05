package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.ranold.config.ConfigScreens;

// removes the globalized layout's sidebar, our screen only
@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.gui.GlobalizedClothConfigScreen")
public abstract class MixinClothGlobalizedScreen {

    @Inject(method = "getSideSliderPosition", at = @At("RETURN"), cancellable = true, require = 0)
    private void torchlit$noSidebar(CallbackInfoReturnable<Integer> cir) {
        if (ConfigScreens.isOurs(this)) {
            cir.setReturnValue(0);
        }
    }
}
