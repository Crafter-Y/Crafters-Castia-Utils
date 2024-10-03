package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.Messages;
import lombok.Setter;
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
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopLogger {
    private static int currentSyncId = Integer.MIN_VALUE;

    @Setter
    private static String selectedShop = null;

    private static String seller = null;
    private static String displayName = null;
    private static BlockPos pos = null;

    public static void register() {
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
    }

    public static String getItemId(ItemStack item) {
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

    private static @Nullable Offer getOfferAtPlayer() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player == null) return null;
        HitResult hit = player.raycast(20, 0, false);

        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockHitResult blockHit = (BlockHitResult) hit;

        int x = blockHit.getBlockPos().getX();
        int y = blockHit.getBlockPos().getY();
        int z = blockHit.getBlockPos().getZ();

        return Offer.getByCoordinate(x,y,z);
    }

    public static void onShopEmpty() {
        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setEmpty(true);
        }
    }

    public static void onShopFull() {
        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setFull(true);
        }
    }

    public static void onBoughtMessage() {
        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setEmpty(false);
        }
    }

    public static void onSoldMessage() {
        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setFull(false);
        }
    }

    public static void onNotEnoughFunds() {
        onShopFull();
    }
}
