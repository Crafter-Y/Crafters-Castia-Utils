package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ContainerValueProvider {
    private static float openedContainerValue = 0;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!CastiaUtils.getConfig().enableChestValue) return;

            if (client.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                ScreenEvents.beforeRender(screen).register((Screen innerScreen, DrawContext drawContext, int mouseX, int mouseY, float deltaTick) -> {
                    render(client, genericContainerScreen, drawContext);
                });
            }
        });
    }

    private static void render(MinecraftClient client, GenericContainerScreen screen, DrawContext drawContext) {
        if (ItemShopTooltip.shouldHideTooltopBecauseOfContainer()) return;

        int textX = screen.x;
        int textY = screen.y - 10;

        DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.CEILING);
        MutableText containerDescription = Text.literal("Container Value: ").append(Text.literal("$" + df.format(openedContainerValue)).formatted(Formatting.GOLD));

        drawContext.drawText(client.textRenderer, containerDescription, textX, textY, 0xFFFFFF, true);
    }

    public static void onInventoryData(List<ItemStack> data) {
        if (!CastiaUtils.getConfig().enableChestValue) return;

        openedContainerValue = 0;

        if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            for (int i = 0; i < genericContainerScreen.getScreenHandler().getRows()*9; i++) {
                ItemStack item = data.get(i);
                String itemId = ShopLogger.getItemId(item);
                List<Offer> offers = Offer.getByItem(itemId);
                if (offers.isEmpty()) continue;
                offers.sort(Comparator.comparing(Offer::getSellPrice));
                List<Offer> sellOffers = offers.stream().filter(offer -> !offer.isFull()).toList().reversed();
                if (sellOffers.isEmpty()) continue;
                Offer bestSellOffer = sellOffers.getFirst();
                openedContainerValue += (bestSellOffer.getSellPrice() * item.getCount());
            }
        }
    }
}
