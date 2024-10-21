package de.craftery.castiautils.mixin;

import de.craftery.castiautils.ah.AhLogger;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandlerSlotUpdateS2CPacket.class)
public class ScreenHandlerSlotUpdateS2CPacketMixin {

    @Shadow @Final private int syncId;
    @Shadow @Final private ItemStack stack;

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/network/RegistryByteBuf;)V")
    private void init(RegistryByteBuf buf, CallbackInfo ci) {
        AhLogger.onSlotData(this.syncId, this.stack);
    }
}
