package de.craftery.castiautils.chestshop;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.craftery.castiautils.Messages;
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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

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
                                            tp(context, offerId);
                                            return 1;
                                        }).suggests((context, builder) -> {
                                            for (String item : Offer.getAll().stream().map(Offer::getItem).distinct().toList()) {
                                                builder.suggest(item.toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                )
                        )
                        .then(literal("deletechest")
                                .executes(context -> {
                                    deleteOffer(context);
                                    return 1;
                                })
                        )
                )
        );
    }

    public static void createShop(CommandContext<FabricClientCommandSource> context, String name, String command) {
        Shop shop = Shop.getByName(name);

        if (shop == null) {
            shop = new Shop();
        }

        shop.setCommand(command);
        shop.setName(name);

        ShopLogger.setSelectedShop(name);

        Messages.sendCommandFeedback(context, Messages.SHOP_CREATED, name, command);
    }

    public static void useShop(CommandContext<FabricClientCommandSource> context, String name) {
        Shop shop = Shop.getByName(name);

        if (shop == null) {
            Messages.sendCommandFeedback(context, Messages.SHOP_NOT_EXISTING);
            return;
        }

        ShopLogger.setSelectedShop(shop.getName());

        Messages.sendCommandFeedback(context, Messages.SHOP_SELECTED, shop.getName(), shop.getCommand());
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

        List<Offer> shops = Offer.getByItem(itemName).stream().filter(shop -> shop.getBuyPrice() != null).sorted((a, b) -> a.getBuyPrice() > b.getBuyPrice() ? 1 : -1).toList();

        if (shops.isEmpty()) {
            Messages.sendCommandFeedback(context, Messages.SHOP_NOT_FOUND, itemName);
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

        List<Offer> shops = Offer.getByItem(itemName).stream().filter(shop -> shop.getSellPrice() != 0).sorted((a, b) -> a.getSellPrice() < b.getSellPrice() ? 1 : -1).toList();

        if (shops.isEmpty()) {
            Messages.sendCommandFeedback(context, Messages.SHOP_NOT_FOUND, itemName);
            return;
        }

        listTradePlaces(context, itemName, shops, false, page);
    }

    private static void listTradePlaces(CommandContext<FabricClientCommandSource> context, String itemName, List<Offer> offers, boolean buy, int page) {
        page--; // because the first page is 1

        context.getSource().sendFeedback(Text.empty());
        if (buy) {
            Messages.sendCommandFeedback(context, Messages.BUY_HEADER, itemName);
        } else {
            Messages.sendCommandFeedback(context, Messages.SELL_HEADER, itemName);
        }

        for (int i = 0; i < offers.size(); i++) {
            if (i < page*8 || i >= (page+1)*8) continue;

            Offer offer = offers.get(i);

            Shop shop = Shop.getByName(offer.getShop());

            assert shop != null;

            MutableText base = Text.empty();

            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);

            if (buy) {
                base.append(Text.literal("$" + df.format(offer.getBuyPrice())).formatted(Formatting.GOLD));
            } else {
                base.append(Text.literal("$" + df.format(offer.getSellPrice())).formatted(Formatting.GOLD));
            }

            base.append(Text.literal(" " + offer.getOwner()).formatted(Formatting.GRAY));

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
                Messages.sendCommandFeedback(context, Messages.NO_SHOP_FOUND);
                return;
            }

            shop.delete();
            Messages.sendCommandFeedback(context, Messages.SHOP_DELETED);
        }
    }

    public static void tp(CommandContext<FabricClientCommandSource> context, String offerId) {
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
}
