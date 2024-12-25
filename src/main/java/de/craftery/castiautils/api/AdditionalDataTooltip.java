package de.craftery.castiautils.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.chestshop.ItemShopTooltip;
import de.craftery.castiautils.chestshop.ShopLogger;
import de.craftery.castiautils.config.CastiaConfig;
import lombok.Data;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.time4j.PrettyTime;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class AdditionalDataTooltip {
    private static final Map<String, CachedAdditionalTooltip> cachedTooltips = new HashMap<>();
    private static int queryTooltipsIn = 0;

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!CastiaUtils.getConfig().queryAdditionalTooltip) return;
            if (ItemShopTooltip.shouldHideTooltopBecauseOfContainer()) return;

            if (type == TooltipType.BASIC || type == TooltipType.ADVANCED) {
                String itemId = ShopLogger.getItemId(stack);
                if (itemId.equals("minecraft:paper")) {
                    for (Component<?> component : stack.getComponents()) {
                        if (component.type() == DataComponentTypes.CUSTOM_MODEL_DATA) return; // dont show pagination papers
                    }
                }

                lines.addAll(getAdvancedTooltip(stack));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CastiaUtils.getConfig().queryAdditionalTooltip) return;

            if (queryTooltipsIn > 0) {
                queryTooltipsIn--;
                if (queryTooltipsIn == 0) {
                    List<String> items = new ArrayList<>();
                    for (Map.Entry<String, CachedAdditionalTooltip> cacheEntry : cachedTooltips.entrySet()) {
                        if (!cacheEntry.getValue().loaded) items.add(cacheEntry.getKey());
                    }

                    new Thread(() -> {
                        List<List<String>> chunks = chunkArray(items, 100);

                        for (int i = 0; i < chunks.size(); i++) {
                            List<String> chunk = chunks.get(i);

                            if (CastiaUtils.getConfig().devMode) {
                                CastiaUtils.LOGGER.info("Query tooltip chunk " + (i+1) + "/" + chunks.size());
                            }
                            try {
                                JsonElement tooltipResponse = RequestService.get("tooltip", chunk.toArray());
                                Gson gson = new Gson();
                                List<CachedAdditionalTooltip> tooltipData = new ArrayList<>(Arrays.stream(gson.fromJson(tooltipResponse, CachedAdditionalTooltip[].class)).toList());
                                for (CachedAdditionalTooltip tooltip : tooltipData) {
                                    tooltip.setLoaded(true);
                                    cachedTooltips.put(tooltip.getItem(), tooltip);
                                }
                            } catch (CastiaUtilsException e) {
                                CastiaUtils.LOGGER.error("Failed to load tooltips: " + e.getMessage());
                            }
                        }
                    }).start();
                }
            }
        });
    }

    public static<T> List<List<T>> chunkArray(List<T> elements, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        int length = elements.size();

        for (int i = 0; i < length; i += chunkSize) {
            int end = Math.min(length, i + chunkSize);
            List<T> chunk = new ArrayList<>(elements.subList(i, end));
            chunks.add(chunk);
        }

        return chunks;
    }

    private static List<Text> getAdvancedTooltip(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        String itemId = ShopLogger.getItemId(stack);
        CachedAdditionalTooltip cache = cachedTooltips.get(itemId);
        if (cache == null) {
            cache = new CachedAdditionalTooltip(itemId);
            cachedTooltips.put(itemId, cache);
            queryTooltipsIn = 10;
        }
        if (!cache.loaded) {
            MutableText loading = Text.translatable("castiautils.loadingAdditionalData").formatted(Formatting.YELLOW);
            tooltip.add(Text.literal(" "));
            tooltip.add(loading);
            return tooltip;
        }
        DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.CEILING);

        if (cache.hasBuyOfferData() || cache.hasSellOfferData() || cache.hasAuctionData()) {
            tooltip.add(Text.literal(" "));
            MutableText descriptor = formatPriceRow("          ",
                    "Min",
                    "l25",
                    "Avg",
                    "Med",
                    "u25",
                    "Max");
            tooltip.add(descriptor);
        }
        if (cache.hasBuyOfferData()) {
            MutableText buyOffer = formatPriceRow("Buy     ",
                    df.format(cache.minBuyOffer),
                    df.format(cache.l25BuyOffer),
                    df.format(cache.avgBuyOffer),
                    df.format(cache.medBuyOffer),
                    df.format(cache.u25BuyOffer),
                    df.format(cache.maxBuyOffer));
            tooltip.add(buyOffer);
        }
        if (cache.hasSellOfferData()) {
            MutableText sellOffer = formatPriceRow("Sell     ",
                    df.format(cache.minSellOffer),
                    df.format(cache.l25SellOffer),
                    df.format(cache.avgSellOffer),
                    df.format(cache.medSellOffer),
                    df.format(cache.u25SellOffer),
                    df.format(cache.maxSellOffer));
            tooltip.add(sellOffer);
        }
        if (cache.hasAuctionData()) {
            MutableText auctions = formatPriceRow("Auction ",
                    df.format(cache.minAuction),
                    df.format(cache.l25Auction),
                    df.format(cache.avgAuction),
                    df.format(cache.medAuction),
                    df.format(cache.u25Auction),
                    df.format(cache.maxAuction));
            tooltip.add(auctions);

            if(cache.lastAuctionPrice != null || cache.lastAuctionAmount != null) {
                MutableText lastAuction = Text.literal("Last Auction: ").formatted(Formatting.GRAY)
                        .append(Text.literal(cache.lastAuctionAmount + "x ").formatted(Formatting.AQUA))
                        .append(Text.literal("for ").formatted(Formatting.GRAY))
                        .append(Text.literal("$" + df.format(cache.lastAuctionPrice)).formatted(Formatting.GOLD))
                        .append(Text.literal(" (").formatted(Formatting.GRAY))
                        .append(Text.literal("$" + df.format(cache.lastAuctionPrice/cache.lastAuctionAmount)).formatted(Formatting.GOLD))
                        .append(Text.literal("/pc").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(")").formatted(Formatting.GRAY))
                                .append(Text.literal(" " + relativeTimeDifferenceStringFromUnix(cache.lastAuctionUnix)).formatted(Formatting.DARK_GRAY));
                tooltip.add(lastAuction);
            }
        }

        return tooltip;
    }

    private static MutableText formatPriceRow(String prefix, String min, String l25, String avg, String med, String u25, String max) {
        MutableText text = Text.literal(prefix).formatted(Formatting.GRAY)
                .append(Text.literal(min).formatted(Formatting.GOLD))
                .append(Text.literal("/").formatted(Formatting.DARK_GRAY));
        CastiaConfig config = CastiaUtils.getConfig();
        if (config.displayComplexPriceMatrix) {
            text.append(Text.literal(l25).formatted(Formatting.GOLD))
                    .append(Text.literal("/").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(avg).formatted(Formatting.GREEN))
                    .append(Text.literal("/").formatted(Formatting.DARK_GRAY));
        }

        text.append(Text.literal(med).formatted(Formatting.RED))
                .append(Text.literal("/").formatted(Formatting.DARK_GRAY));

        if (config.displayComplexPriceMatrix) {
            text.append(Text.literal(u25).formatted(Formatting.GOLD))
                    .append(Text.literal("/").formatted(Formatting.DARK_GRAY));
        }

        text.append(Text.literal(max).formatted(Formatting.GOLD));

        return text;
    }

    private static String relativeTimeDifferenceStringFromUnix(Long unix) {
        Locale userLocale = CastiaUtils.getMinecraftLocale();
        PrettyTime prettyTime = PrettyTime.of(userLocale);

        return prettyTime.printRelative(Instant.ofEpochSecond(unix / 1000), ZoneId.of("UTC"));
    }

    public static void invalidateCache(String itemId) {
        cachedTooltips.remove(itemId);
    }

    public static void invalidateAll() {
        cachedTooltips.clear();
    }

    @Data
    private static class CachedAdditionalTooltip {
        private transient boolean loaded = true;
        private String item;

        private Float minBuyOffer;
        private Float l25BuyOffer;
        private Float avgBuyOffer;
        private Float medBuyOffer;
        private Float u25BuyOffer;
        private Float maxBuyOffer;

        private Float minSellOffer;
        private Float l25SellOffer;
        private Float avgSellOffer;
        private Float medSellOffer;
        private Float u25SellOffer;
        private Float maxSellOffer;

        private Float minAuction;
        private Float l25Auction;
        private Float avgAuction;
        private Float medAuction;
        private Float u25Auction;
        private Float maxAuction;
        private Long lastAuctionUnix;
        private Float lastAuctionPrice;
        private Integer lastAuctionAmount;

        public CachedAdditionalTooltip(String item) {
            this.item = item;
            this.loaded = false;
        }

        public boolean hasBuyOfferData() {
            return minBuyOffer != null ||
                    l25BuyOffer != null ||
                    avgBuyOffer != null ||
                    medBuyOffer != null ||
                    u25BuyOffer != null ||
                    maxBuyOffer != null;
        }

        public boolean hasSellOfferData() {
            return minSellOffer != null ||
                    l25SellOffer != null ||
                    avgSellOffer != null ||
                    medSellOffer != null ||
                    u25SellOffer != null ||
                    maxSellOffer != null;
        }

        public boolean hasAuctionData() {
            return minAuction != null ||
                    l25Auction != null ||
                    avgAuction != null ||
                    medAuction != null ||
                    u25Auction != null ||
                    maxAuction != null;
        }
    }
}
