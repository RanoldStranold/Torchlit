package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.ranold.client.GuiShapeRecorder;
import net.ranold.client.HotbarDarkness;

@Mixin(Hud.class)
public abstract class MixinHud {

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void torchlit$beginRecording(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        GuiShapeRecorder.begin();
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    private void torchlit$darkenHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HotbarDarkness.render(graphics, GuiShapeRecorder.end());
    }
}
