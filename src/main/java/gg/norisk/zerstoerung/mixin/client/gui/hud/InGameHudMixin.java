package gg.norisk.zerstoerung.mixin.client.gui.hud;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import gg.norisk.zerstoerung.modules.InventoryManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow
    private int scaledHeight;

    @Shadow
    private int scaledWidth;

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0))
    private void injected(DrawContext drawContext, Identifier identifier, int a, int b, int c, int d) {
        if (InventoryManager.INSTANCE.isEnabled()) {
            InventoryManager.INSTANCE.renderHotbar(drawContext, this.scaledWidth, this.scaledHeight);
        } else {
            drawContext.drawGuiTexture(identifier, a, b, c, d);
        }
    }

    @WrapWithCondition(
            method = "renderHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V")
    )
    private boolean handleItemRendering(InGameHud instance, DrawContext drawContext, int i, int j, float f, PlayerEntity playerEntity, ItemStack itemStack, int k) {
        return !(itemStack.isOf(Items.BARRIER) && InventoryManager.INSTANCE.isEnabled());
    }
}
