package net.ranold.mixin.client;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.ranold.client.GuiTint;

// tags what a darkened screen or the hotbar adds; items, text and picture-in-picture become blits/glyphs
// later in GuiRenderer.prepare, so those inherit the tag of the state they came from
@Mixin(GuiRenderState.class)
public abstract class MixinGuiRenderState {

    @Inject(method = "addGuiElement", at = @At("HEAD"))
    private void torchlit$tagElement(GuiElementRenderState state, CallbackInfo ci) {
        GuiTint.tagExtracted(state, state.bounds());
    }

    @Inject(method = "addItem", at = @At("HEAD"))
    private void torchlit$tagItem(GuiItemRenderState state, CallbackInfo ci) {
        GuiTint.tagExtracted(state, state.bounds());
    }

    @Inject(method = "addText", at = @At("HEAD"))
    private void torchlit$tagText(GuiTextRenderState state, CallbackInfo ci) {
        GuiTint.tagExtracted(state, state.bounds());
    }

    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"))
    private void torchlit$tagPictureInPicture(PictureInPictureRenderState state, CallbackInfo ci) {
        GuiTint.tagExtracted(state, state.bounds());
    }

    @Inject(method = "addBlitToCurrentLayer", at = @At("HEAD"))
    private void torchlit$tagPreparedBlit(BlitRenderState state, CallbackInfo ci) {
        GuiTint.tagPrepared(state);
    }

    @Inject(method = "addGlyphToCurrentLayer", at = @At("HEAD"))
    private void torchlit$tagPreparedGlyph(GuiElementRenderState state, CallbackInfo ci) {
        GuiTint.tagPrepared(state);
    }

    @ModifyVariable(method = "forEachItem", at = @At("HEAD"), argsOnly = true)
    private Consumer<GuiItemRenderState> torchlit$itemSource(Consumer<GuiItemRenderState> consumer) {
        return GuiTint.trackSource(consumer);
    }

    @ModifyVariable(method = "forEachText", at = @At("HEAD"), argsOnly = true)
    private Consumer<GuiTextRenderState> torchlit$textSource(Consumer<GuiTextRenderState> consumer) {
        return GuiTint.trackSource(consumer);
    }

    @ModifyVariable(method = "forEachPictureInPicture", at = @At("HEAD"), argsOnly = true)
    private Consumer<PictureInPictureRenderState> torchlit$pictureSource(Consumer<PictureInPictureRenderState> consumer) {
        return GuiTint.trackSource(consumer);
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void torchlit$clearTags(CallbackInfo ci) {
        GuiTint.clear();
    }
}
