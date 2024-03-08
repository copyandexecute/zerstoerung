package gg.norisk.zerstoerung.mixin.client.gui.screen.ingame;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    @Nullable
    protected abstract Slot getSlotAt(double d, double e);

    protected HandledScreenMixin(Text text) {
        super(text);
    }

    @WrapWithCondition(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlight(Lnet/minecraft/client/gui/DrawContext;III)V")
    )
    private boolean onlyRenderIfAllowed(DrawContext drawContext, int i, int j, int k) {
        var slot = this.focusedSlot;
        if (slot != null) {
            ItemStack itemStack = slot.getStack();
            return !itemStack.isOf(Items.BARRIER);
        }
        return true;
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getSlotAt(DD)Lnet/minecraft/screen/slot/Slot;"), cancellable = true)
    protected void injected(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        var slot = this.getSlotAt(d, e);
        if (slot != null) {
            ItemStack itemStack = slot.getStack();
            if (itemStack.isOf(Items.BARRIER)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "handleHotbarKeyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V"), cancellable = true)
    protected void injected(int i, int j, CallbackInfoReturnable<Boolean> cir) {
        var slot = this.focusedSlot;
        if (slot != null) {
            ItemStack itemStack = slot.getStack();
            if (itemStack.isOf(Items.BARRIER)) {
                cir.setReturnValue(false);
            }
        }
    }

    @WrapWithCondition(
            method = "keyPressed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V")
    )
    private boolean onlyRenderIfAllowed(HandledScreen<T> instance, Slot slot, int i, int j, SlotActionType slotActionType) {
        return !slot.getStack().isOf(Items.BARRIER);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void injected(DrawContext drawContext, Slot slot, CallbackInfo ci) {
        int i = slot.x - 1;
        int j = slot.y - 1;
        ItemStack stack = slot.getStack();
        drawContext.drawText(textRenderer, Text.of(String.valueOf(slot.id)), i, j, -1, true);
        if (stack.isOf(Items.BARRIER)) {
            drawContext.fillGradient(RenderLayer.getGuiOverlay(), i, j, i + 18, j + 18, -3750202, -3750202, 0);
            ci.cancel();
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"), cancellable = true)
    protected void injected(DrawContext drawContext, int i, int j, CallbackInfo ci) {
        ItemStack itemStack = this.focusedSlot.getStack();
        if (itemStack.isOf(Items.BARRIER)) {
            ci.cancel();
        }
    }
}
