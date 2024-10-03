package de.craftery.castiautils.chestshop;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.Messages;
import lombok.Setter;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonElement;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopLogger {
    private static int currentSyncId = Integer.MIN_VALUE;

    @Setter
    private static String selectedShop = null;

    private static String seller = null;
    private static String displayName = null;
    private static BlockPos pos = null;

    private static File getConfigFile(String filename) {
        filename = "./config/" + filename;
        try {
            File myObj = new File(filename);
            boolean ignored = myObj.createNewFile();
            return myObj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeState() {
        CastiaUtils.LOGGER.info("Saving state");
        Jankson jankson = Jankson.builder().build();
        JsonElement offers = jankson.toJson(Offer.getAll().toArray());
        JsonElement shops = jankson.toJson(Shop.getAll().toArray());

        BufferedWriter offersWriter;
        BufferedWriter shopsWriter;
        try {
            offersWriter = new BufferedWriter(new FileWriter(getConfigFile("offers.json5")));
            offersWriter.append(offers.toJson());
            offersWriter.close();

            shopsWriter = new BufferedWriter(new FileWriter(getConfigFile("shops.json5")));
            shopsWriter.append(shops.toJson());
            shopsWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void register() {
        Gson gson = new Gson();
        try {
            String json = FileUtils.readFileToString(getConfigFile("offers.json5"), "UTF-8");
            List<Offer> offers = new ArrayList<>(Arrays.stream(gson.fromJson(json, Offer[].class)).toList());
            Offer.setOffers(offers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JsonSyntaxException e) {
            CastiaUtils.LOGGER.info("No offers stored in file");
        }

        try {
            String json = FileUtils.readFileToString(getConfigFile("shops.json5"), "UTF-8");
            List<Shop> shops = new ArrayList<>(Arrays.stream(gson.fromJson(json, Shop[].class)).toList());
            Shop.setShops(shops);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JsonSyntaxException e) {
            CastiaUtils.LOGGER.info(e);
            CastiaUtils.LOGGER.info("No shops stored in file");
        }

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
            if (blockEntity instanceof SignBlockEntity signBlockEntity) {
                Text[] lines = signBlockEntity.getFrontText().getMessages(false);

                if (lines.length != 4) return ActionResult.PASS;
                seller = lines[0].getString();
                displayName = lines[1].getString();
                pos = hitResult.getBlockPos();
            }
            return ActionResult.PASS;
        });

        ClientSendMessageEvents.COMMAND.register((command -> {
            String[] parts = command.split(" ");

            List<Shop> possibleShops = null;

            if (parts.length >= 2 && parts[0].equalsIgnoreCase("pw")) {
                possibleShops = Shop.getByCommand("/pw " + parts[1]);
            }
            if (parts.length == 1 && parts[0].startsWith("shop") && !parts[0].equals("shop")) {
                possibleShops = Shop.getByCommand("/" + parts[0]);
            }

            if (possibleShops != null) {
                selectedShop = null;
                if (!possibleShops.isEmpty()) {
                    Shop shop = possibleShops.getFirst();
                    selectedShop = shop.getName();

                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player == null) return;

                    Messages.sendPlayerActionBar(player, Messages.SHOP_SELECTED, shop.getName(), shop.getCommand());
                }
            }
        }));

        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            writeState();
        });

        AtomicInteger currentTick = new AtomicInteger(1);
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            int tick = currentTick.getAndIncrement();

            if (tick % 2400 == 0) {
                writeState();
            }

        });

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (type == TooltipType.BASIC || type == TooltipType.ADVANCED) {
                String itemId = getItemId(stack);
                List<Offer> offers = Offer.getByItem(itemId);

                if (offers.isEmpty()) return;

                DecimalFormat df = new DecimalFormat("#.##");
                df.setRoundingMode(RoundingMode.CEILING);

                offers.sort(Comparator.comparing(Offer::getSellPrice));
                Offer bestSellOffer = offers.reversed().getFirst();
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
                Offer bestBuyOffer = offers.getFirst();
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

    private static String getItemId(ItemStack item) {
        String itemId = "minecraft:" + item.getItem().getTranslationKey().split("\\.")[2];

        if (itemId.equals("minecraft:filled_map")) {
            MapIdComponent mapIdComponent = item.get(DataComponentTypes.MAP_ID);

            if (mapIdComponent != null) {
                itemId += "#" + mapIdComponent.id();
            }
        }

        for (Component<?> component : item.getComponents()) {
            if (component.type() == DataComponentTypes.CUSTOM_DATA && component.value() instanceof NbtComponent nbt) {
                Pattern itemPattern = Pattern.compile("oxywire:item_id\":\"([a-zA-Z\\d]+)\"");
                Matcher itemMatcher = itemPattern.matcher(nbt.copyNbt().toString());
                if (itemMatcher.find()) {
                    itemId = "oxywire:" + itemMatcher.group(1).toLowerCase();

                    Pattern successPattern = Pattern.compile("oxymechanics:success-rate\":(\\d+)");
                    Matcher successMatcher = successPattern.matcher(nbt.copyNbt().toString());

                    if (successMatcher.find()) {
                        itemId += "#" + successMatcher.group(1);
                    }
                }
            }
        }
        return itemId;
    }

    public static void onShopOpen(int syncId) {
        if ((MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen containerScreen)) {
            if(containerScreen.getTitle().getString().length() == 2 && containerScreen.getTitle().getString().charAt(0) == 57344 && containerScreen.getTitle().getString().charAt(1) == 57856) {
                currentSyncId = syncId;
            }
        }
    }

    public static void onShopData(int syncId, List<ItemStack> data) {
        if (syncId != currentSyncId) return;
        currentSyncId = Integer.MIN_VALUE;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        if (selectedShop == null) {
            Messages.sendPlayerMessage(player, Messages.NO_SHOP_SELECTED);
            return;
        }

        Offer offer = Offer.getByCoordinate(pos.getX(), pos.getY(), pos.getZ());

        if (offer != null) {
            if (!selectedShop.equals(offer.getShop())) {
                Shop otherShop = Shop.getByName(offer.getShop());
                if (otherShop != null) {
                    Messages.sendPlayerMessage(player, Messages.SHOP_OWNED_BY, otherShop.getName(), otherShop.getCommand());
                    return;
                }
            }
        } else {
            offer = new Offer();
        }

        float buyPrice;
        float sellPrice;

        try {
            String buyString = ((PlainTextContent.Literal) data.get(11).getName().getSiblings().getFirst().getSiblings().getFirst().getContent()).string().replaceAll("[-$,]", "");
            String sellString = ((PlainTextContent.Literal) data.get(15).getName().getSiblings().getFirst().getSiblings().getFirst().getContent()).string().replaceAll("[+$,]", "");

            if (buyString.length() > 10) {
                buyPrice = Float.MAX_VALUE;
            } else {
                buyPrice = Float.parseFloat(buyString);
            }
            if (sellString.length() > 10) {
                sellPrice = Float.MAX_VALUE;
            } else {
                sellPrice = Float.parseFloat(sellString);
            }
        } catch (NumberFormatException e) {
            CastiaUtils.LOGGER.error("Chestsop price could not be parsed: ");
            CastiaUtils.LOGGER.error(e);
            return;
        }

        ItemStack item = data.get(13);

        String itemId = getItemId(item);

        offer.setBuyPrice(buyPrice);
        offer.setSellPrice(sellPrice);
        offer.setShop(selectedShop);
        offer.setItem(itemId);
        offer.setOwner(seller);
        offer.setDisplay(displayName);
        offer.setX(pos.getX());
        offer.setY(pos.getY());
        offer.setZ(pos.getZ());

        player.sendMessage(Text.literal(selectedShop + " " + itemId + " (" + buyPrice + ", " + sellPrice + ")"), true);
    }
}
