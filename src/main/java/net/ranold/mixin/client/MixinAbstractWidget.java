package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.ranold.client.GuiDarkness;
import net.ranold.client.GuiShapeRecorder;

// reading screens keep their buttons lit
@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget {

    @Unique
    private boolean torchlit$paused;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void torchlit$pauseForControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a,
                                           CallbackInfo ci) {
        this.torchlit$paused = GuiDarkness.exemptsWidgets(Minecraft.getInstance().gui.screen());
        if (this.torchlit$paused) {
            GuiShapeRecorder.pause();
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void torchlit$resumeAfterControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a,
                                              CallbackInfo ci) {
        if (this.torchlit$paused) {
            this.torchlit$paused = false;
            GuiShapeRecorder.resume();
        }
    }
}
