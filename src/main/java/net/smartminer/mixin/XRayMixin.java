package net.smartminer.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.smartminer.SmartClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class XRayMixin {

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    public void getRenderType(CallbackInfoReturnable<BlockRenderType> cir) {
        if (SmartClient.modules != null && SmartClient.modules.xray) {
            BlockState state = (BlockState) (Object) this;
            if (!SmartClient.modules.isXrayBlock(state)) {
                cir.setReturnValue(BlockRenderType.INVISIBLE);
            }
        }
    }

    @Inject(method = "isOpaque", at = @At("HEAD"), cancellable = true)
    public void isOpaque(CallbackInfoReturnable<Boolean> cir) {
        if (SmartClient.modules != null && SmartClient.modules.xray) {
            BlockState state = (BlockState) (Object) this;
            if (!SmartClient.modules.isXrayBlock(state)) {
                cir.setReturnValue(false);
            }
        }
    }
}
