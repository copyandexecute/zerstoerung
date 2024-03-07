package gg.norisk.zerstoerung.mixin.world.chunk;

import gg.norisk.zerstoerung.modules.BlockManager;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WrapperProtoChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WrapperProtoChunk.class)
public abstract class WrapperProtoChunkMixin extends Chunk {
    public WrapperProtoChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> registry, long l, @Nullable ChunkSection[] chunkSections, @Nullable BlendingData blendingData) {
        super(chunkPos, upgradeData, heightLimitView, registry, l, chunkSections, blendingData);
    }

    @Inject(method = "setBlockState", at = @At(value = "HEAD"), cancellable = true)
    private void injected(BlockPos blockPos, BlockState blockState, boolean bl, CallbackInfoReturnable<BlockState> cir) {
        BlockManager.INSTANCE.handleSetBlockState(blockPos, blockState, bl, cir);
    }
}
