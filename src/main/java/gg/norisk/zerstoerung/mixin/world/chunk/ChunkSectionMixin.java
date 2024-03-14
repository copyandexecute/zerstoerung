package gg.norisk.zerstoerung.mixin.world.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSection.class)
public abstract class ChunkSectionMixin {
    @Shadow
    public abstract RegistryEntry<Biome> getBiome(int i, int j, int k);

    @Inject(method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At(value = "HEAD"), cancellable = true)
    private void injected(int i, int j, int k, BlockState blockState, boolean bl, CallbackInfoReturnable<BlockState> cir) {
        //unsed as i replaced it with chunkMutate
        /*if (this.getBiome(i, j, k).getKey().get().getValue().toString().contains("ocean")) {
            BlockState returnValue = cir.getReturnValue();
            if (returnValue.isLiquid()) {
                System.out.println("Tried to Place Liquid at " + i + " " + j + " " + k);
            }
        }*/
        //BlockManager.INSTANCE.handleSetBlockState(i,j,k,blockState,bl,cir);
    }
}
