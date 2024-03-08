package gg.norisk.zerstoerung.mixin.client.gui.screen.ingame;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import gg.norisk.zerstoerung.modules.InventoryManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
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
    private boolean renderDrawSlotHightlight(DrawContext drawContext, int i, int j, int k) {
        return !InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, this.focusedSlot);
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getSlotAt(DD)Lnet/minecraft/screen/slot/Slot;"), cancellable = true)
    protected void injected(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, this.getSlotAt(d, e))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseReleased", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getSlotAt(DD)Lnet/minecraft/screen/slot/Slot;"), cancellable = true)
    protected void injected2(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, this.getSlotAt(d, e))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleHotbarKeyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V"), cancellable = true)
    protected void injected(int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, this.focusedSlot)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onMouseClick(I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V"), cancellable = true)
    protected void injected(int i, CallbackInfo ci) {
        if (InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, this.focusedSlot)) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = "keyPressed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V")
    )
    private boolean handleKeyPressed(HandledScreen<T> instance, Slot slot, int i, int j, SlotActionType slotActionType) {
        return !InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, slot);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void injected(DrawContext drawContext, Slot slot, CallbackInfo ci) {
        InventoryManager.INSTANCE.drawSlot((HandledScreen) (Object) this, drawContext, slot, ci);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"), cancellable = true)
    protected void injected(DrawContext drawContext, int i, int j, CallbackInfo ci) {
        if (InventoryManager.INSTANCE.isSlotBlocked((HandledScreen) (Object) this, this.focusedSlot)) {
            ci.cancel();
        }
    }
}
