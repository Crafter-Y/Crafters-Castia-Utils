package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.chestshop.relic.RelicPriceEstimation;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ItemShopTooltip {
    public static boolean shouldHideTooltopBecauseOfContainer() {
        if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen) {
            ContainerType type = ContainerType.getCurrentScreenType();

            return type == ContainerType.OTHER;
        }
        return false;
    }


    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!CastiaUtils.getConfig().enableTooltipInfo) return;
            if (shouldHideTooltopBecauseOfContainer()) return;
            if (type != TooltipType.BASIC && type != TooltipType.ADVANCED) return;


            String itemId = ShopLogger.getItemId(stack);

            if (itemId.equals("minecraft:paper")) {
                for (Component<?> component : stack.getComponents()) {
                    if (component.type() == DataComponentTypes.CUSTOM_MODEL_DATA) return; // dont show pagination papers
                }
            }

            DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
            df.setRoundingMode(RoundingMode.CEILING);

            List<Offer> offers = Offer.getByItem(itemId);

            if (!offers.isEmpty()) {
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
                        sellAll.append(Text.literal("$" + df.format(bestSellOffer.getSellPrice() * stack.getCount())).formatted(Formatting.GOLD));
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

                if (!buyOffers.isEmpty()) {
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
            }

            if (itemId.contains("shulker_box")) {
                for (Component<?> component : stack.getComponents()) {
                    if (component.type() == DataComponentTypes.CONTAINER && component.value() instanceof ContainerComponent containerComponent) {
                        List<ItemStack> contents = containerComponent.stream().toList();
                        float containerValue = 0;

                        for (ItemStack containerItem : contents) {
                            containerValue += ContainerValueProvider.getStackValue(containerItem);
                        }
                        if (containerValue != 0) {
                            MutableText containerText = Text.empty();
                            containerText.append(Text.literal("Sell contents: ").formatted(Formatting.GRAY));
                            containerText.append(Text.literal("$" + df.format(containerValue)).formatted(Formatting.GOLD));
                            lines.add(containerText);
                        }
                    }
                }
            }

            if (CastiaUtils.getConfig().enableEstimateRelicPrices) {
                RelicPriceEstimation.estimateItemValue(stack, lines);
            }

            if (type == TooltipType.ADVANCED) {
                for (Text line : lines) {
                    String minecraftId = "minecraft:" + stack.getItem().getTranslationKey().split("\\.")[2];
                    if (line.getString().equals(minecraftId) && line instanceof MutableText mut) {
                        mut.append(Text.literal("\n"+itemId));
                    }
                }
            }
        });
    }
}
