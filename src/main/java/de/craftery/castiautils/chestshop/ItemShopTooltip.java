package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import lombok.Setter;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

public class ItemShopTooltip {
    @Setter
    private static String currentInventoryTitle = "";

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!CastiaUtils.getConfig().enableTooltipInfo) return;

            if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen) {
                if (
                        !currentInventoryTitle.isEmpty() &&
                        !currentInventoryTitle.equals("Auctions") && // dont hide in auctions
                        !(currentInventoryTitle.length() == 2 && currentInventoryTitle.charAt(0) == 57344 && currentInventoryTitle.charAt(1) == 57856)) // dont hide in chestshops
                {
                    return;
                }
            }

            if (type == TooltipType.BASIC || type == TooltipType.ADVANCED) {
                String itemId = ShopLogger.getItemId(stack);
                List<Offer> offers = Offer.getByItem(itemId);

                if (offers.isEmpty()) return;

                DecimalFormat df = new DecimalFormat("#.##");
                df.setRoundingMode(RoundingMode.CEILING);

                offers.sort(Comparator.comparing(Offer::getSellPrice));
                List<Offer> sellOffers = offers.stream().filter(offer -> !offer.isFull()).toList().reversed();
                if (!sellOffers.isEmpty()) {
                    Offer bestSellOffer = sellOffers.getFirst();
                    Shop sellShop = Shop.getByName(bestSellOffer.getShop());

                    if (bestSellOffer.getSellPrice() > 0) {
                        if (sellShop == null) {
                            CastiaUtils.LOGGER.error("Sellshop should not be null here! (shop does not exist for offer?): " + bestSellOffer.getShop());
                            return;
                        }
                        MutableText sellAll = Text.empty();

                        sellAll.append(Text.literal("Sell (" + stack.getCount() + ") for ").formatted(Formatting.GRAY));
                        sellAll.append(Text.literal("$" + df.format(bestSellOffer.getSellPrice()*stack.getCount())).formatted(Formatting.GOLD));
                        sellAll.append(Text.literal(" at ").formatted(Formatting.GRAY));

                        sellAll.append(Text.literal(sellShop.getCommand()).formatted(Formatting.AQUA));
                        lines.add(sellAll);

                        MutableText sellSingle = Text.empty();
                        sellSingle.append(Text.literal("Sell (1) for ").formatted(Formatting.GRAY));
                        sellSingle.append(Text.literal("$" + df.format(bestSellOffer.getSellPrice())).formatted(Formatting.GOLD));
                        lines.add(sellSingle);
                    }
                }

                offers.sort(Comparator.comparing(Offer::getBuyPrice));
                List<Offer> buyOffers = offers.stream().filter(offer -> !offer.isEmpty()).toList();
                if (buyOffers.isEmpty()) return;
                Offer bestBuyOffer = buyOffers.getFirst();
                Shop buyShop = Shop.getByName(bestBuyOffer.getShop());

                if (buyShop == null) {
                    CastiaUtils.LOGGER.error("Buyshop should not be null here! (shop does not exist for offer?)");
                    return;
                }

                MutableText buyAll = Text.empty();

                buyAll.append(Text.literal("Buy (1) for ").formatted(Formatting.GRAY));
                buyAll.append(Text.literal("$" + df.format(bestBuyOffer.getBuyPrice())).formatted(Formatting.GOLD));
                buyAll.append(Text.literal(" at ").formatted(Formatting.GRAY));

                buyAll.append(Text.literal(buyShop.getCommand()).formatted(Formatting.AQUA));
                lines.add(buyAll);
            }
        });
    }
}
