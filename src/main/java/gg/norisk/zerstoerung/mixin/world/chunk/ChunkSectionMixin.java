package gg.norisk.zerstoerung.mixin.world.chunk;

import gg.norisk.zerstoerung.modules.BlockManager;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSection.class)
public abstract class ChunkSectionMixin {
    @Inject(method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At(value = "HEAD"), cancellable = true)
    private void injected(int i, int j, int k, BlockState blockState, boolean bl, CallbackInfoReturnable<BlockState> cir) {
        BlockManager.INSTANCE.handleSetBlockState(i,j,k,blockState,bl,cir);
    }
}
