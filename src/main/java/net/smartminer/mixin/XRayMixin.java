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
    
    // В 1.21.4 сигнатура метода изменилась: убрали BlockView, BlockPos и otherPos
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockState sideState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        
        // Проверяем, инициализирован ли клиент и включен ли X-Ray
        if (SmartClient.modules != null && SmartClient.modules.xray) {
            
            // Если блок НЕ является рудой (камень, земля и т.д.) — мы говорим игре: "Не рисуй его!"
            if (!SmartClient.modules.isXrayBlock(state)) {
                cir.setReturnValue(false); 
            }
            
        }
    }
}
