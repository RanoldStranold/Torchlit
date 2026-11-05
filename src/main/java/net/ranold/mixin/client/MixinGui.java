package net.ranold.mixin.client;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.ranold.client.ContainerLocation;
import net.ranold.client.GuiDarkness;

// screens live in Gui in 26.3. ties an opening screen to the block just clicked
@Mixin(Gui.class)
public abstract class MixinGui {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void torchlit$trackContainerBlock(@Nullable Screen screen, CallbackInfo ci) {
        ContainerLocation.onScreenOpened(screen != null && GuiDarkness.belongsToBlock(screen));

        GuiDarkness.resetSmoothing();
    }
}
