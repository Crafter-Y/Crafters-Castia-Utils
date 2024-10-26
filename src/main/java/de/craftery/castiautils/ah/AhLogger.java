package de.craftery.castiautils.ah;

import blue.endless.jankson.annotation.Nullable;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.api.AdditionalDataTooltip;
import de.craftery.castiautils.api.RequestService;
import de.craftery.castiautils.chestshop.ShopLogger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
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
            if (!CastiaUtils.getConfig().contributeAuctions) return;

            if (commitPendingIn > 0) {
                commitPendingIn--;
                if (commitPendingIn == 0) {
                    try {
                        RequestService.put("auction", pendingOffers.toArray());
                    } catch (CastiaUtilsException e) {
                        CastiaUtils.LOGGER.error("Auction contribution failed: " + e.getMessage());
                    }

                    for (AhOffer offer : pendingOffers) {
                        AdditionalDataTooltip.invalidateCache(offer.getItem());
                    }
                    pendingOffers.clear();
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
            if (line.getString().contains("-------------------------")) linesFound++;
        }
        if (linesFound != 2) return null;

        for (Text line : lines) {
            if (line.getString().contains("»") && line.getString().contains("$")) {
                Pattern pricePattern = Pattern.compile("\\$(\\d+(,\\d+)*(\\.\\d+)?)");
                Matcher priceMatcher = pricePattern.matcher(line.getString());
                if (priceMatcher.find()) {
                    String priceString = priceMatcher.group(1).replaceAll("[,]", "");
                    try {
                        float price = Float.parseFloat(priceString);
                        return new AhOffer(ShopLogger.getItemId(item), price, item.getCount());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return null;
    }

    public static void onContainerOpen(int syncId) {
        if (!CastiaUtils.getConfig().contributeAuctions) return;

        if ((MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen containerScreen)) {
            if (containerScreen.getTitle().getString().equals("Auctions")) {
                currentSyncId = syncId;
            }
        }
    }

    public static void onSlotData(int syncId, ItemStack data) {
        if (!CastiaUtils.getConfig().contributeAuctions) return;

        if (syncId != currentSyncId) return;
        AhOffer offer = parseOffer(data);
        if (offer != null) queuePendingOffer(offer);
    }

    public static void onInventoryData(int syncId, List<ItemStack> data) {
        if (!CastiaUtils.getConfig().contributeAuctions) return;

        if (syncId != currentSyncId) return;

        for (int i = 0; i < 44; i++) {
            AhOffer offer = parseOffer(data.get(i));
            if (offer != null) queuePendingOffer(offer);
        }
    }
}