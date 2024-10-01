package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.PlainTextContent;

import java.util.List;

public class ShopLogger {
    private static int currentSyncId = Integer.MIN_VALUE;

    public static void onShopOpen(int syncId) {
        if ((MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen containerScreen)) {
            if(containerScreen.getTitle().getString().length() == 2 && containerScreen.getTitle().getString().charAt(0) == 57344 && containerScreen.getTitle().getString().charAt(1) == 57856) {
                currentSyncId = syncId;
            }
        }
    }

    public static void onShopData(int syncId, List<ItemStack> data) {
        if (syncId == currentSyncId) {
            currentSyncId = Integer.MIN_VALUE;
            PlainTextContent.Literal buyLiteral = (PlainTextContent.Literal) data.get(11).getName().getSiblings().getFirst().getSiblings().getFirst().getContent();
            CastiaUtils.LOGGER.info("Buy price: " + buyLiteral.string().replaceAll("[-$,]", ""));
            CastiaUtils.LOGGER.info(data.get(13).getComponents());
            PlainTextContent.Literal sellLiteral = (PlainTextContent.Literal) data.get(15).getName().getSiblings().getFirst().getSiblings().getFirst().getContent();
            CastiaUtils.LOGGER.info("Sell price: " + sellLiteral.string().replaceAll("[+$,]", ""));
        }
    }
}
