package de.craftery.castiautils.mixin;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.chestshop.ItemShopTooltip;
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
        ItemShopTooltip.setCurrentInventoryTitle(title.getString());
        if (!CastiaUtils.getConfig().chestshopDataCollection) return;

        if (title.getString().length() != 2) return;

        if (title.getString().charAt(0) == 57344 && title.getString().charAt(1) == 57856) {
            ShopLogger.onShopOpen(id);
        }
    }
}
