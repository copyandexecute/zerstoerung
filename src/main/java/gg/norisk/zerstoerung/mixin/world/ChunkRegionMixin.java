package gg.norisk.zerstoerung.mixin.world;

import gg.norisk.zerstoerung.modules.StructureManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin {
    @Shadow @Deprecated public abstract ServerWorld toServerWorld();

    @ModifyArgs(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"))
    private void injected0(Args args) {
        StructureManager.INSTANCE.addBlockPos(this.toServerWorld(), args.get(0));
    }
}
