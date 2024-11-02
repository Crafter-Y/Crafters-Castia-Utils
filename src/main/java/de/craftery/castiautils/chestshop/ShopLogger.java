package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.Messages;
import de.craftery.castiautils.api.AdditionalDataTooltip;
import de.craftery.castiautils.api.RequestService;
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
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
            if (!CastiaUtils.getConfig().chestshopDataCollection) return ActionResult.PASS;

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
            if (!CastiaUtils.getConfig().chestshopDataCollection) return;

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

                    Messages.sendPlayerActionBar(player, "shopSelected", shop.getName(), shop.getCommand());
                }
            }
        }));
    }

    public static String getItemId(ItemStack item) {
        StringBuilder itemId = new StringBuilder();
        itemId.append("minecraft:").append(item.getItem().getTranslationKey().split("\\.")[2]);

        if (itemId.toString().equals("minecraft:filled_map")) {
            MapIdComponent mapIdComponent = item.get(DataComponentTypes.MAP_ID);

            if (mapIdComponent != null) {
                itemId.append("#").append(mapIdComponent.id());
            }
        }

        if (itemId.toString().equals("minecraft:spawner")) {
            for (Component<?> component : item.getComponents()) {
                if (component.type() == DataComponentTypes.CUSTOM_DATA && component.value() instanceof NbtComponent nbt) {
                    Pattern itemPattern = Pattern.compile("oxymechanics:spawner-type\":\"([A-Z_]+)\"");
                    Matcher itemMatcher = itemPattern.matcher(nbt.copyNbt().toString());
                    if (itemMatcher.find()) {
                        itemId.append("#").append(itemMatcher.group(1).toLowerCase());
                    }
                }
            }
        }

        if (itemId.toString().equals("minecraft:enchanted_book")) {
            for (Component<?> component : item.getComponents()) {
                if (component.type() == DataComponentTypes.STORED_ENCHANTMENTS && component.value() instanceof ItemEnchantmentsComponent enchantmentsComponent) {
                    itemId.append("#");
                    for (RegistryEntry<Enchantment> enchantment : enchantmentsComponent.getEnchantments()) {
                        itemId.append(enchantment.getIdAsString().replace("minecraft:", ""));
                        int enchantmentLevel = enchantmentsComponent.getLevel(enchantment);
                        if (enchantmentLevel > 1) {
                            itemId.append(enchantmentLevel);
                        }
                    }
                }
            }
        }

        if (itemId.toString().equals("minecraft:potion") || itemId.toString().equals("minecraft:splash_potion")) {
            for (Component<?> component : item.getComponents()) {
                if (component.type() == DataComponentTypes.POTION_CONTENTS && component.value() instanceof PotionContentsComponent potionComponent) {
                    Optional<RegistryEntry<Potion>> potion = potionComponent.potion();
                    if (potion.isPresent()) {
                        itemId.append("#");
                        itemId.append(potion.get().getIdAsString().replace("minecraft:", ""));
                    }
                }
            }
        }

        if (itemId.toString().equals("minecraft:firework_rocket")) {
            for (Component<?> component : item.getComponents()) {
                if (component.type() == DataComponentTypes.FIREWORKS && component.value() instanceof FireworksComponent fireworksComponent) {
                    itemId.append("#").append(fireworksComponent.flightDuration());
                }
            }
        }

        if (itemId.toString().endsWith("shulker_box")) {
            outer:
            for (Component<?> component : item.getComponents()) {
                if (component.type() == DataComponentTypes.CONTAINER && component.value() instanceof ContainerComponent containerComponent) {
                    List<ItemStack> contents = containerComponent.stream().toList();
                    itemId.append("#");

                    if (contents.isEmpty()) {
                        itemId.append("empty");
                        continue;
                    }
                    if (contents.size() != 27) {
                        itemId.append("mixed");
                        continue;
                    }
                    String currentItemId = getItemId(contents.getFirst());
                    for (ItemStack stack : contents) {
                        if (stack.getCount() != stack.getItem().getMaxCount()) {
                            itemId.append("mixed");
                            continue outer;
                        }
                        if (!getItemId(stack).equals(currentItemId)) {
                            itemId.append("mixed");
                            continue outer;
                        }
                    }
                    itemId.append(currentItemId.replace("minecraft:", ""));
                }
            }
        }

        for (Component<?> component : item.getComponents()) {
            if (component.type() == DataComponentTypes.CUSTOM_DATA && component.value() instanceof NbtComponent nbt) {
                Pattern itemPattern = Pattern.compile("oxywire:item_id\":\"([a-zA-Z\\d_]+)\"");
                Matcher itemMatcher = itemPattern.matcher(nbt.copyNbt().toString());
                if (itemMatcher.find()) {
                    itemId = new StringBuilder("oxywire:").append(itemMatcher.group(1).toLowerCase());

                    Pattern successPattern = Pattern.compile("oxymechanics:success-rate\":(\\d+)");
                    Matcher successMatcher = successPattern.matcher(nbt.copyNbt().toString());

                    if (successMatcher.find()) {
                        itemId.append("#").append(successMatcher.group(1));
                    }
                }
            }
        }
        return itemId.toString();
    }

    public static void onContainerOpen(int syncId, Text title) {
        if (!CastiaUtils.getConfig().chestshopDataCollection) return;

        if (title.getString().length() != 2) return;
        if (title.getString().charAt(0) != 57344 || title.getString().charAt(1) != 57856) return;

        if ((MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen containerScreen)) {
            if(containerScreen.getTitle().getString().length() == 2 && containerScreen.getTitle().getString().charAt(0) == 57344 && containerScreen.getTitle().getString().charAt(1) == 57856) {
                currentSyncId = syncId;
            }
        }
    }

    public static void onInventoryData(int syncId, List<ItemStack> data) {
        if (!CastiaUtils.getConfig().chestshopDataCollection) return;

        if (syncId != currentSyncId) return;
        currentSyncId = Integer.MIN_VALUE;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        if (selectedShop == null) {
            Messages.sendPlayerMessage(player, "noShopSelected");
            return;
        }

        Offer offer = Offer.getByCoordinate(pos.getX(), pos.getY(), pos.getZ());

        if (offer != null) {
            if (!selectedShop.equals(offer.getShop())) {
                Shop otherShop = Shop.getByName(offer.getShop());
                if (otherShop != null) {
                    Messages.sendPlayerMessage(player, "shopOwnedBy", otherShop.getName(), otherShop.getCommand());
                    return;
                }
            }
        } else {
            offer = new Offer();
        }

        float buyPrice;
        float sellPrice;

        try {
            String buyString;
            try {
                buyString = ((PlainTextContent.Literal) data.get(11).getName().getSiblings().getFirst().getSiblings().getFirst().getContent()).string().replaceAll("[-$,]", "");
            } catch (NoSuchElementException ignored) {
                buyString = "10000000000000000000000000";
            }

            String sellString;
            try {
                sellString = ((PlainTextContent.Literal) data.get(15).getName().getSiblings().getFirst().getSiblings().getFirst().getContent()).string().replaceAll("[+$,]", "");
            } catch (NoSuchElementException ignored) {
                sellString = "0";
            }


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

        final Offer offerToSend = SerializationUtils.clone(offer);
        new Thread(() -> {
            if (CastiaUtils.getConfig().apiEnabled) {
                try {
                    RequestService.post("offer", offerToSend.getUniqueIdentifier(), offerToSend);
                    player.sendMessage(Text.literal(selectedShop + " " + itemId + " (" + buyPrice + ", " + sellPrice + ") (synced)"), true);
                    AdditionalDataTooltip.invalidateCache(offerToSend.getItem());
                } catch (CastiaUtilsException e) {
                    CastiaUtils.LOGGER.error("Sync failed because of: " + e.getMessage());
                    player.sendMessage(Text.literal(selectedShop + " " + itemId + " (" + buyPrice + ", " + sellPrice + ") (sync failed)"), true);
                }
            } else {
                player.sendMessage(Text.literal(selectedShop + " " + itemId + " (" + buyPrice + ", " + sellPrice + ")"), true);
            }
        }).start();
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
        if (!CastiaUtils.getConfig().chestshopDataCollection) return;

        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setEmpty(true);
            shop.setFull(false);
            triggerApiUpdate(shop);
        }
    }

    public static void onShopFull() {
        if (!CastiaUtils.getConfig().chestshopDataCollection) return;

        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setFull(true);
            triggerApiUpdate(shop);
        }
    }

    public static void onBoughtMessage() {
        if (!CastiaUtils.getConfig().chestshopDataCollection) return;

        Offer shop = getOfferAtPlayer();
        if (shop != null) {
            shop.setEmpty(false);
            shop.setFull(false);
            triggerApiUpdate(shop);
        }
    }

    public static void onSoldMessage() {
        onBoughtMessage();
    }

    public static void onNotEnoughFunds() {
        onShopFull();
    }

    private static void triggerApiUpdate(Offer offer) {
        new Thread(() -> {
            if (CastiaUtils.getConfig().apiEnabled) {
                try {
                    RequestService.post("offer", offer.getUniqueIdentifier(), offer);
                } catch (CastiaUtilsException e) {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player == null) return;
                    Messages.sendPlayerMessage(player, "syncFailed", e.getMessage());
                }

                AdditionalDataTooltip.invalidateCache(offer.getItem());
            }
        }).start();
    }
}
