package de.craftery.castiautils.chestshop;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.Messages;
import de.craftery.castiautils.api.AdditionalDataTooltip;
import de.craftery.castiautils.api.RequestService;
import de.craftery.castiautils.config.CastiaConfig;
import de.craftery.castiautils.config.DataSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import red.jackf.whereisit.api.SearchResult;
import red.jackf.whereisit.client.render.CurrentGradientHolder;
import red.jackf.whereisit.client.render.Rendering;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ShopCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("shop")
                        .then(literal("create")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("command", StringArgumentType.greedyString())
                                                .executes((context) -> {
                                                    String name = StringArgumentType.getString(context, "name");
                                                    String command = StringArgumentType.getString(context, "command");
                                                    createShop(context, name, command);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(literal("use")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            useShop(context, name);
                                            return 1;
                                        }).suggests((context, builder) -> {
                                            Shop.getAll().stream().map(Shop::getName).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                )
                        )
                        .then(literal("buy")
                                .then(argument("item", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String itemName = StringArgumentType.getString(context, "item");
                                            buyItem(context, itemName);
                                            return 1;
                                        }).suggests((context, builder) -> {
                                            for (String item : Offer.getAll().stream().map(Offer::getItem).distinct().toList()) {
                                                builder.suggest(item.toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                )
                        )
                        .then(literal("sell")
                                .then(argument("item", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String itemName = StringArgumentType.getString(context, "item");
                                            sellItem(context, itemName);
                                            return 1;
                                        }).suggests((context, builder) -> {
                                            for (String item : Offer.getAll().stream().map(Offer::getItem).distinct().toList()) {
                                                builder.suggest(item.toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                )
                        )
                        .then(literal("tp")
                                .then(argument("offerId", StringArgumentType.word())
                                        .executes(context -> {
                                            String offerId = StringArgumentType.getString(context, "offerId");
                                            tp(offerId);
                                            return 1;
                                        }).suggests((context, builder) -> {
                                            for (String item : Offer.getAll().stream().map(Offer::getItem).distinct().toList()) {
                                                builder.suggest(item.toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                )
                        )
                        .then(literal("deleteoffer")
                                .executes(context -> {
                                    deleteOffer(context);
                                    return 1;
                                })
                        )
                        .then(literal("push")
                                .executes(context -> {
                                    pushShops(context);
                                    return 1;
                                })
                        )
                        .then(literal("reload")
                                .executes(context -> {
                                    reloadShops(context);
                                    return 1;
                                })
                        )
                        .then(literal("reset")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            resetShop(context, name);
                                            return 1;
                                        }).suggests((context, builder) -> {
                                            Shop.getAll().stream().map(Shop::getName).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                )
                        )
                )
        );
    }

    public static void createShop(CommandContext<FabricClientCommandSource> context, String name, String command) {
        Shop shop = Shop.getByName(name);
        if (shop != null) {
            Messages.sendCommandFeedback(context, "shopExisting");
            return;
        }

        shop = new Shop();
        shop.setCommand(command);
        shop.setName(name);

        ShopLogger.setSelectedShop(name);

        if (CastiaUtils.getConfig().contributeShops) {
            try {
                RequestService.put("shop", shop);
                Messages.sendCommandFeedback(context, "successfulContribution");
            } catch (CastiaUtilsException e) {
                Messages.sendCommandFeedback(context, "failedContribution", e.getMessage());
            }
        }

        Messages.sendCommandFeedback(context, "shopCreated", name, command);
    }

    public static void useShop(CommandContext<FabricClientCommandSource> context, String name) {
        Shop shop = Shop.getByName(name);

        if (shop == null) {
            Messages.sendCommandFeedback(context, "shopNotExisting");
            return;
        }

        ShopLogger.setSelectedShop(shop.getName());

        Messages.sendCommandFeedback(context, "shopSelected", shop.getName(), shop.getCommand());
    }

    public static void buyItem(CommandContext<FabricClientCommandSource> context, String itemName) {
        int page = 1;
        if (itemName.contains(" ")) {
            String[] parts = itemName.split(" ");
            itemName = parts[0];
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        List<Offer> shops = Offer.getByItem(itemName).stream().filter(shop -> shop.getBuyPrice() != null).sorted((a, b) -> a.getBuyPrice() > b.getBuyPrice() ? 1 : -1).toList(); // TODO: Comparator.comparing()

        if (shops.isEmpty()) {
            Messages.sendCommandFeedback(context, "shopNotFound", itemName);
            return;
        }

        listTradePlaces(context, itemName, shops,true, page);
    }

    public static void sellItem(CommandContext<FabricClientCommandSource> context, String itemName) {
        int page = 1;
        if (itemName.contains(" ")) {
            String[] parts = itemName.split(" ");
            itemName = parts[0];
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        List<Offer> shops = Offer.getByItem(itemName).stream().filter(shop -> shop.getSellPrice() != 0).sorted((a, b) -> a.getSellPrice() < b.getSellPrice() ? 1 : -1).toList(); // TODO: Comparator.comparing()

        if (shops.isEmpty()) {
            Messages.sendCommandFeedback(context, "shopNotFound", itemName);
            return;
        }

        listTradePlaces(context, itemName, shops, false, page);
    }

    private static void listTradePlaces(CommandContext<FabricClientCommandSource> context, String itemName, List<Offer> offers, boolean buy, int page) {
        page--; // because the first page is 1

        context.getSource().sendFeedback(Text.empty());
        if (buy) {
            Messages.sendCommandFeedback(context, "buyHeader", itemName);
        } else {
            Messages.sendCommandFeedback(context, "sellHeader", itemName);
        }

        for (int i = 0; i < offers.size(); i++) {
            if (i < page*8 || i >= (page+1)*8) continue;

            Offer offer = offers.get(i);

            Shop shop = Shop.getByName(offer.getShop());

            assert shop != null;

            MutableText base = Text.empty();

            DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
            df.setRoundingMode(RoundingMode.CEILING);

            if (buy) {
                base.append(Text.literal("$" + df.format(offer.getBuyPrice())).formatted(Formatting.GOLD));
            } else {
                base.append(Text.literal("$" + df.format(offer.getSellPrice())).formatted(Formatting.GOLD));
            }

            base.append(Text.literal(" " + offer.getOwner()).formatted(Formatting.GRAY));

            if (offer.isEmpty()) {
                base.append(Text.literal(" (").formatted(Formatting.GRAY));
                base.append(Text.literal("empty").formatted(Formatting.RED));
                base.append(Text.literal(")").formatted(Formatting.GRAY));
            }

            if (offer.isFull()) {
                base.append(Text.literal(" (").formatted(Formatting.GRAY));
                base.append(Text.literal("full").formatted(Formatting.RED));
                base.append(Text.literal(")").formatted(Formatting.GRAY));
            }

            base.append(Text.literal(" "));
            Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop tp " + offer.getId()));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to tp")));
            MutableText tp = Text.literal("[TP]").setStyle(style).formatted(Formatting.GREEN);
            base.append(tp);

            context.getSource().sendFeedback(base);
        }
        int maxPage = (int) Math.ceil((double) offers.size() / 8);
        MutableText pages = Text.empty();
        String verb = buy ? "buy" : "sell";
        if (page != 0) {
            pages.append(Text.literal("<<< ").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop " +verb + " " + itemName + " " + page)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Show page " + page)))).formatted(Formatting.GOLD));
        }
        pages.append(Text.literal("Page ").formatted(Formatting.DARK_GRAY));
        pages.append(Text.literal((page + 1) + "").formatted(Formatting.GOLD));
        pages.append(Text.literal("/").formatted(Formatting.DARK_GRAY));
        pages.append(Text.literal(maxPage + " ").formatted(Formatting.GOLD));
        if (page + 1 != maxPage) {
            pages.append(Text.literal(">>>").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop " + verb + " " + itemName + " " + (page + 2))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Show page " + (page+2))))).formatted(Formatting.GOLD));
        }

        pages.append(Text.literal(" "));

        String invVerb = buy ?  "sell" : "buy";
        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop " + invVerb + " " + itemName));

        if (buy) {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("castiautils.switchToSell")));
            MutableText text = Text.translatable("castiautils.switchToSell").setStyle(style).formatted(Formatting.LIGHT_PURPLE);
            pages.append(text);
        } else {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("castiautils.switchToBuy")));
            MutableText text = Text.translatable("castiautils.switchToBuy").setStyle(style).formatted(Formatting.LIGHT_PURPLE);
            pages.append(text);
        }

        context.getSource().sendFeedback(pages);
    }

    public static void deleteOffer(CommandContext<FabricClientCommandSource> context) {
        ClientPlayerEntity player = context.getSource().getPlayer();

        HitResult hit = player.raycast(20, 0, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;

            int x = blockHit.getBlockPos().getX();
            int y = blockHit.getBlockPos().getY();
            int z = blockHit.getBlockPos().getZ();

            Offer shop = Offer.getByCoordinate(x,y,z);

            if (shop == null) {
                Messages.sendCommandFeedback(context, "noShopFound");
                return;
            }

            shop.delete();

            if (CastiaUtils.getConfig().contributeOffers) {
                try {
                    RequestService.delete("offer", shop.getUniqueIdentifier());
                } catch (CastiaUtilsException e) {
                    Messages.sendCommandFeedback(context, "deleteApiRequestFailed", e.getMessage());
                }
            }

            Messages.sendCommandFeedback(context, "shopDeleted");
        }
    }

    public static void tp(String offerId) {
        Offer cs = Offer.getById(offerId);
        if (cs == null) return;

        Shop shop = Shop.getByName(cs.getShop());
        if (shop == null) return;

        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return;

        ShopLogger.setSelectedShop(shop.getName());

        handler.sendCommand(shop.getCommand().replace("/", ""));

        // unfortunately this is very hacky. but it works :)
        // integration with Where Is it?
        Rendering.resetSearchTime();
        SearchResult search = SearchResult.builder(new BlockPos(cs.getX(), cs.getY(), cs.getZ())).build();
        Collection<SearchResult> list = new ArrayDeque<>();
        list.add(search);
        Rendering.addResults(list);

        CurrentGradientHolder.refreshColourScheme();
    }

    public static void pushShops(CommandContext<FabricClientCommandSource> context) {
        CastiaConfig config = CastiaUtils.getConfig();

        if (config.apiUrl.isEmpty()) {
            Messages.sendCommandFeedback(context, "noApiUrl");
            return;
        }
        if (config.token.isEmpty()) {
            Messages.sendCommandFeedback(context, "noToken");
            return;
        }
        if (!config.contributeShops || !config.contributeOffers) {
            Messages.sendCommandFeedback(context, "dontContribute");
            return;
        }
        try {
            RequestService.put("shop", Shop.getAll().toArray());
            Messages.sendCommandFeedback(context, "successfulShopsContribution");
        } catch (CastiaUtilsException e) {
            Messages.sendCommandFeedback(context, "failedContribution", e.getMessage());
        }

        try {
            RequestService.put("offer", Offer.getAll().toArray());
            Messages.sendCommandFeedback(context, "successfulOfferContribution");
        } catch (CastiaUtilsException e) {
            Messages.sendCommandFeedback(context, "failedContribution", e.getMessage());
        }
    }

    public static void reloadShops(CommandContext<FabricClientCommandSource> context) {
        CastiaConfig config = CastiaUtils.getConfig();

        new Thread(() -> {
            ShopConfig.writeState();
            try {
                ShopConfig.load();

                if (config.dataSource == DataSource.LOCAL_ONLY) {
                    Messages.sendCommandFeedback(context, "reloadedLocal");
                } else if (config.dataSource == DataSource.SERVER_ONLY) {
                    Messages.sendCommandFeedback(context, "reloadedServer");
                } else {
                    Messages.sendCommandFeedback(context, "reloadedMerge");
                }
            } catch (CastiaUtilsException e) {
                Messages.sendCommandFeedback(context, "reloadFailed", e.getMessage());
            }

            AdditionalDataTooltip.invalidateAll();
        }).start();
    }

    public static void resetShop(CommandContext<FabricClientCommandSource> context, String shopName) {
        Shop shop = Shop.getByName(shopName);
        if (shop == null) {
            Messages.sendCommandFeedback(context, "shopNotFound", shopName);
            return;
        }

        int deletedCount = 0;
        for (Offer offer : List.copyOf(Offer.getAll())) {
            if (offer.getShop().equals(shop.getName())) {
                offer.delete();
                deletedCount++;

                if (CastiaUtils.getConfig().contributeOffers) {
                    try {
                        RequestService.delete("offer", offer.getUniqueIdentifier());
                    } catch (CastiaUtilsException e) {
                        Messages.sendCommandFeedback(context, "deleteApiRequestFailed", e.getMessage());
                    }
                }
            }
        }

        if (deletedCount > 0) {
            Messages.sendCommandFeedback(context, "offersDeleted", deletedCount + "");
        } else {
            Messages.sendCommandFeedback(context, "noOffersDeleted");
        }
    }
}
