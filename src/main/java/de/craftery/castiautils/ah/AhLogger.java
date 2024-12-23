package de.craftery.castiautils.ah;

import blue.endless.jankson.annotation.Nullable;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.api.AdditionalDataTooltip;
import de.craftery.castiautils.api.RequestService;
import de.craftery.castiautils.chestshop.ContainerType;
import de.craftery.castiautils.chestshop.ShopLogger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AhLogger {
    private static int currentSyncId = Integer.MIN_VALUE;
    private static final List<AhOffer> pendingOffers = new ArrayList<>();
    private static int commitPendingIn = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CastiaUtils.getConfig().apiEnabled) return;

            if (commitPendingIn > 0) {
                commitPendingIn--;
                if (commitPendingIn == 0) {
                    List<AhOffer> offers = List.copyOf(pendingOffers);
                    pendingOffers.clear();
                    new Thread(() -> {
                        try {
                            RequestService.put("auction", offers.toArray());
                        } catch (CastiaUtilsException e) {
                            if (CastiaUtils.getConfig().devMode) {
                                CastiaUtils.LOGGER.error("Auction contribution failed: " + e.getMessage());
                            }
                        }

                        for (AhOffer offer : offers) {
                            AdditionalDataTooltip.invalidateCache(offer.getItem());
                        }

                    }).start();
                }
            }
        });
    }

    private static void queuePendingOffer(AhOffer offer) {
        if (pendingOffers.stream().noneMatch(pOffer -> pOffer.equals(offer))) {
            commitPendingIn = 5;
            pendingOffers.add(offer);
        }
    }

    private static @Nullable AhOffer parseOffer(ItemStack item) {
        List<Text> lines = item.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC);
        if (lines.size() < 11) return null;

        int linesFound = 0;
        for (Text line : lines) {
            if (line.getString().equals("                                        ") && line.getStyle().isStrikethrough()) linesFound++;
        }
        if (linesFound != 1) return null;

        for (Text line : lines) {
            if (line.getString().startsWith("Price: $")) {
                Pattern pricePattern = Pattern.compile("\\$(\\d+(,\\d+)*(\\.\\d+)?)");
                Matcher priceMatcher = pricePattern.matcher(line.getString());
                if (priceMatcher.find()) {
                    String priceString = priceMatcher.group(1).replaceAll("[,]", "");
                    try {
                        float price = Float.parseFloat(priceString);
                        String itemId = ShopLogger.getItemId(item);
                        return new AhOffer(itemId, price, item.getCount());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    CastiaUtils.LOGGER.error("Auction house price tag could not be parsed: " + line.getString());
                }
            }
        }

        return null;
    }

    public static void onContainerOpen(int syncId) {
        if (!CastiaUtils.getConfig().apiEnabled) return;

        if (ContainerType.getCurrentScreenType() == ContainerType.AUCTION_HOUSE) {
            if (CastiaUtils.getConfig().devMode) {
                CastiaUtils.LOGGER.info("Auction house opened");
            }

            currentSyncId = syncId;
        }
    }

    public static void onSlotData(int syncId, ItemStack data) {
        if (!CastiaUtils.getConfig().apiEnabled) return;

        if (syncId != currentSyncId) return;
        AhOffer offer = parseOffer(data);
        if (offer != null) queuePendingOffer(offer);
    }

    public static void onInventoryData(int syncId, List<ItemStack> data) {
        if (!CastiaUtils.getConfig().apiEnabled) return;

        if (syncId != currentSyncId) return;

        for (int i = 0; i < 44; i++) {
            AhOffer offer = parseOffer(data.get(i));
            if (offer != null) queuePendingOffer(offer);
        }
    }
}