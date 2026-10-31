package net.smartminer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.smartminer.SmartClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.block.Block.class)
public class XRayMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockView world, BlockPos pos, Direction side, BlockPos otherPos, CallbackInfoReturnable<Boolean> cir) {
        // Проверяем, включен ли X-Ray и инициализирован ли мод
        if (SmartClient.modules != null && SmartClient.modules.xray) {
            // Если блок НЕ входит в список видимых (камень, земля) — отменяем его отрисовку
            if (!SmartClient.modules.isXrayBlock(state)) {
                cir.setReturnValue(false);
            }
        }
    }
}
