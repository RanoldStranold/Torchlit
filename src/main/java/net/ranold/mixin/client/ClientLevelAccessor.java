package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {

    // getter is 0 when flashes are hidden
    @Invoker("getSkyFlashTime")
    int torchlit$getSkyFlashTime();
}
