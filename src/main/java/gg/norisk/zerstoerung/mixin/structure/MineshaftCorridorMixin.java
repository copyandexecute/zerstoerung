package gg.norisk.zerstoerung.mixin.structure;

import gg.norisk.zerstoerung.modules.StructureManager;
import net.minecraft.block.BlockState;
import net.minecraft.structure.MineshaftGenerator;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MineshaftGenerator.MineshaftCorridor.class)
public abstract class MineshaftCorridorMixin {
    @ModifyArgs(method = "fillDownwards", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/StructureWorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void injected2(Args args, StructureWorldAccess structureWorldAccess, BlockState blockState, int i, int j, int k, BlockBox blockBox) {
        StructureManager.INSTANCE.addBlockPos(structureWorldAccess.toServerWorld(), args.get(0));
    }

    @ModifyArgs(method = "fillColumn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/StructureWorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void injected0(Args args, StructureWorldAccess structureWorldAccess, BlockState blockState, BlockPos.Mutable mutable, int i, int j) {
        StructureManager.INSTANCE.addBlockPos(structureWorldAccess.toServerWorld(), args.get(0));
    }

    @ModifyArgs(method = "fillSupportBeam(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/block/BlockState;IIILnet/minecraft/util/math/BlockBox;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/StructureWorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void injected1(Args args, StructureWorldAccess structureWorldAccess, BlockState blockState, int i, int j, int k, BlockBox blockBox) {
        StructureManager.INSTANCE.addBlockPos(structureWorldAccess.toServerWorld(), args.get(0));
    }
}
