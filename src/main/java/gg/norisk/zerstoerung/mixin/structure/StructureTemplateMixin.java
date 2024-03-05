package gg.norisk.zerstoerung.mixin.structure;

import gg.norisk.zerstoerung.modules.StructureManager;
import gg.norisk.zerstoerung.Zerstoerung;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @ModifyArgs(method = "place", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/mojang/datafixers/util/Pair;"))
    private void injected(Args args) {
        BlockPos blockPos = args.get(0);
        Zerstoerung.INSTANCE.getLogger().info("Structure Block add " + blockPos);
        StructureManager.INSTANCE.getStructureBlocks().add(blockPos);
    }
}
