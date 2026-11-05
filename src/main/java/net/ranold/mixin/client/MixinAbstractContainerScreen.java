package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.ranold.client.GuiShapeRecorder;

// hovered-slot highlight stays lit, darkened it inverts
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"))
    private void torchlit$pauseForHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        GuiShapeRecorder.pause();
    }

    @Inject(method = "extractSlotHighlightBack", at = @At("RETURN"))
    private void torchlit$resumeAfterHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        GuiShapeRecorder.resume();
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"))
    private void torchlit$pauseForHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        GuiShapeRecorder.pause();
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("RETURN"))
    private void torchlit$resumeAfterHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        GuiShapeRecorder.resume();
    }
}
