package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
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
            if (!CastiaUtils.getConfig().enableContainerValue) return;

            if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
                ScreenEvents.beforeRender(screen).register((Screen innerScreen, DrawContext drawContext, int mouseX, int mouseY, float deltaTick) -> {
                    render(client, handledScreen, drawContext);
                });
            }
        });
    }

    private static void render(MinecraftClient client, HandledScreen<?> screen, DrawContext drawContext) {
        if (ItemShopTooltip.shouldHideTooltopBecauseOfContainer()) return;
        if (screen.getTitle().getString().equals("Auctions")) return;
        if (
                !(screen instanceof GenericContainerScreen) &&
                !(screen instanceof ShulkerBoxScreen)
        ) return;
        if (screen.getTitle().getString().length() == 2 && screen.getTitle().getString().charAt(0) == 57344 && screen.getTitle().getString().charAt(1) == 57856) return; // hide anyway in chestshops

        int textX = screen.x;
        int textY = screen.y - 10;

        DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.CEILING);
        MutableText containerDescription = Text.literal("Container Value: ").append(Text.literal("$" + df.format(openedContainerValue)).formatted(Formatting.GOLD));

        drawContext.drawText(client.textRenderer, containerDescription, textX, textY, 0xFFFFFF, true);
    }

    public static void onInventoryData(List<ItemStack> data) {
        if (!CastiaUtils.getConfig().enableContainerValue) return;

        openedContainerValue = 0;

        if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            for (int i = 0; i < genericContainerScreen.getScreenHandler().getRows()*9; i++) {
                openedContainerValue += getStackValue(data.get(i));
            }
        }
        if (MinecraftClient.getInstance().currentScreen instanceof ShulkerBoxScreen) {
            for (int i = 0; i < 27; i++) {
                openedContainerValue += getStackValue(data.get(i));
            }
        }
    }

    public static float getStackValue(ItemStack stack) {
        String itemId = ShopLogger.getItemId(stack);
        List<Offer> offers = Offer.getByItem(itemId);
        if (offers.isEmpty()) return 0;
        offers.sort(Comparator.comparing(Offer::getSellPrice));
        List<Offer> sellOffers = offers.stream().filter(offer -> !offer.isFull()).toList().reversed();
        if (!sellOffers.isEmpty() && sellOffers.getFirst().getSellPrice() != 0) {
            Offer bestSellOffer = sellOffers.getFirst();
            return bestSellOffer.getSellPrice() * stack.getCount();
        } else if(CastiaUtils.getConfig().fallbackToBuyOnContainerValue) {
            offers.sort(Comparator.comparing(Offer::getBuyPrice));
            List<Offer> buyOffers = offers.stream().filter(offer -> !offer.isEmpty()).toList();
            if (!buyOffers.isEmpty()) {
                Offer bestBuyOffer = buyOffers.getFirst();
                return bestBuyOffer.getBuyPrice() * stack.getCount();
            }
        }
        return 0;
    }
}
