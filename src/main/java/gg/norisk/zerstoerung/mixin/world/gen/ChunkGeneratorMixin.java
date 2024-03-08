package gg.norisk.zerstoerung.mixin.world.gen;

import gg.norisk.zerstoerung.modules.BlockManager;
import gg.norisk.zerstoerung.modules.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Redirect(method = "generateFeatures", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void structureInjection(List<StructureStart> instance, Consumer<StructureStart> consumer, StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        StructureManager.INSTANCE.handleStructureGeneration(instance, consumer, world);
    }

    @Inject(method = "generateFeatures", at = @At("TAIL"))
    private void afterGenerateFeatures(StructureWorldAccess structureWorldAccess, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci) {
        BlockManager.INSTANCE.mutateChunk(chunk, structureWorldAccess);
    }
}
