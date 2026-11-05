package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int torchlit$leftPos();

    @Accessor("topPos")
    int torchlit$topPos();

    @Accessor("imageWidth")
    int torchlit$imageWidth();

    @Accessor("imageHeight")
    int torchlit$imageHeight();

    @Accessor("menu")
    AbstractContainerMenu torchlit$menu();
}
