package de.craftery.castiautils.mixin;

import de.craftery.castiautils.chestshop.ContainerValueProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("TAIL"))
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            if (genericContainerScreen.getScreenHandler().getInventory() instanceof SimpleInventory inv) {
                ContainerValueProvider.onInventoryData(inv.getHeldStacks());
            }
        }
    }
}
