package gg.norisk.zerstoerung.mixin.structure;

import gg.norisk.zerstoerung.modules.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @ModifyArgs(method = "place", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/mojang/datafixers/util/Pair;"))
    private void injected(Args args, ServerWorldAccess serverWorldAccess, BlockPos blockPos, BlockPos blockPos2, StructurePlacementData structurePlacementData, Random random, int i) {
        StructureManager.INSTANCE.addBlockPos( serverWorldAccess.toServerWorld(), args.get(0));
    }
}
