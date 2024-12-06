package de.craftery.castiautils.chestshop;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.config.CastiaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;

public enum ContainerType {
    AUCTION_HOUSE,
    TOWN_VAULT,
    PRIVATE_VAULT,
    CHESTSHOP,
    CHEST,
    SHULKER_BOX,
    INVENTORY,
    OTHER;

    public static ContainerType getCurrentScreenType() {
        CastiaConfig conf = CastiaUtils.getConfig();

        Screen screen = MinecraftClient.getInstance().currentScreen;

        if (screen instanceof GenericContainerScreen genScreen) {
            String title = genScreen.getTitle().getString();

            if (title.length() == 2) {
                if (title.charAt(0) != 57344) {
                    if (conf.devMode) {
                        CastiaUtils.LOGGER.error("Unmatched starting title code " + ((int) title.charAt(0)));
                    }

                    return ContainerType.OTHER;
                }

                return switch (title.charAt(1)) {
                    case 57961 -> ContainerType.AUCTION_HOUSE;
                    case 57856 -> ContainerType.CHESTSHOP;
                    default -> {
                        if (conf.devMode) {
                            CastiaUtils.LOGGER.error("Unmatched title code " + ((int) title.charAt(1)));
                        }

                        yield ContainerType.OTHER;
                    }
                };
            } else if (title.equals("Town Vault")) {
                return ContainerType.TOWN_VAULT;
            } else if (title.equals("Vault #")) {
                return ContainerType.PRIVATE_VAULT;
            }
            return ContainerType.CHEST;
        }
        if (screen instanceof ShulkerBoxScreen) return ContainerType.SHULKER_BOX;
        if (screen instanceof InventoryScreen) return ContainerType.INVENTORY;

        return ContainerType.OTHER;
    }
}
