package de.craftery.castiautils.mixin;

import de.craftery.castiautils.ah.AhLogger;
import de.craftery.castiautils.chestshop.ShopLogger;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(InventoryS2CPacket.class)
public class InventoryS2CPacketMixin {

    @Final
    @Shadow private int syncId;

    @Final
    @Shadow private List<ItemStack> contents;

    @Inject(at = @At("HEAD"), method = "getContents")
    public void getContents(CallbackInfoReturnable<List<ItemStack>> cir) {
        ShopLogger.onInventoryData(syncId, contents);
        AhLogger.onInventoryData(syncId, contents);
    }
}
