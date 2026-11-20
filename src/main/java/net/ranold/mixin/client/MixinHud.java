package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.ranold.client.HotbarDarkness;

@Mixin(Hud.class)
public abstract class MixinHud {

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void torchlit$beginTint(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HotbarDarkness.begin(graphics);
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    private void torchlit$finishTint(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HotbarDarkness.finish();
    }
}
