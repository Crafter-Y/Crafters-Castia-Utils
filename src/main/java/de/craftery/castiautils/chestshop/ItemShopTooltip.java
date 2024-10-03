package de.craftery.castiautils.chestshop;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

public class ItemShopTooltip {
    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (type == TooltipType.BASIC || type == TooltipType.ADVANCED) {
                String itemId = ShopLogger.getItemId(stack);
                List<Offer> offers = Offer.getByItem(itemId);

                if (offers.isEmpty()) return;

                DecimalFormat df = new DecimalFormat("#.##");
                df.setRoundingMode(RoundingMode.CEILING);

                offers.sort(Comparator.comparing(Offer::getSellPrice));
                Offer bestSellOffer = offers.stream().filter(offer -> !offer.isFull()).toList().reversed().getFirst();
                Shop sellShop = Shop.getByName(bestSellOffer.getShop());

                if (bestSellOffer.getSellPrice() > 0) {
                    assert sellShop != null;
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
                offers.sort(Comparator.comparing(Offer::getBuyPrice));
                Offer bestBuyOffer = offers.stream().filter(offer -> !offer.isEmpty()).toList().getFirst();
                Shop buyShop = Shop.getByName(bestBuyOffer.getShop());
                assert buyShop != null;
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
