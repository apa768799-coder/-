package net.smartminer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.smartminer.SmartClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.block.Block.class)
public class XRayMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockState sideState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (SmartClient.modules != null && SmartClient.modules.xray) {
            if (!SmartClient.modules.isXrayBlock(state)) {
                cir.setReturnValue(false); 
            }
        }
    }
}
