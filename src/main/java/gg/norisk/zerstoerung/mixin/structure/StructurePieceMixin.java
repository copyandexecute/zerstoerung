package gg.norisk.zerstoerung.mixin.structure;

import gg.norisk.zerstoerung.modules.StructureManager;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.StructureWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {
    @ModifyArgs(method = "addBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/StructureWorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void injected(Args args, StructureWorldAccess structureWorldAccess, BlockState blockState, int i, int j, int k, BlockBox blockBox) {
        StructureManager.INSTANCE.addBlockPos(structureWorldAccess.toServerWorld(), args.get(0));
    }
}
