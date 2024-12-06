package de.craftery.castiautils.mixin;

import de.craftery.castiautils.ah.AhLogger;
import de.craftery.castiautils.chestshop.ShopLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreens.class)
public class HandledScreensMixin {
    @Inject(at = @At("TAIL"), method = "open(Lnet/minecraft/screen/ScreenHandlerType;Lnet/minecraft/client/MinecraftClient;ILnet/minecraft/text/Text;)V")
    private static <T extends ScreenHandler> void open(ScreenHandlerType<T> type, MinecraftClient client, int id, Text title, CallbackInfo ci) {
        ShopLogger.onContainerOpen(id);
        AhLogger.onContainerOpen(id);
    }
}
