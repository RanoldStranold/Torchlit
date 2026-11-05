package net.ranold.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.ranold.client.ContainerLocation;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void torchlit$recordUsedBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        ContainerLocation.recordUse(hitResult.getBlockPos(), hitResult.getDirection());
    }
}
