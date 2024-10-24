package de.craftery.castiautils.api;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.Messages;
import de.craftery.castiautils.chestshop.ShopConfig;
import de.craftery.castiautils.config.CastiaConfig;
import de.craftery.castiautils.config.DataSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class RefetchService {
    private static int lastFetchTicksAgo = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CastiaConfig config = CastiaUtils.getConfig();
            if (config.periodicallyRefetchData == RefetchPeriod.OFF) return;
            int ticksForFetch = CastiaUtils.getConfig().periodicallyRefetchData.getSeconds()*20;

            lastFetchTicksAgo++;
            if (lastFetchTicksAgo >= ticksForFetch) {
                lastFetchTicksAgo = 0;

                new Thread(() -> {
                    ShopConfig.writeState();

                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    try {
                        ShopConfig.load();
                        CastiaUtils.LOGGER.info("reloaded data");

                        if (player != null && config.devMode) {
                            if (config.dataSource == DataSource.LOCAL_ONLY) {
                                Messages.sendPlayerMessage(player, "reloadedLocal");
                            } else if (config.dataSource == DataSource.SERVER_ONLY) {
                                Messages.sendPlayerMessage(player, "reloadedServer");
                            } else {
                                Messages.sendPlayerMessage(player, "reloadedMerge");
                            }
                        }
                    } catch (CastiaUtilsException e) {
                        CastiaUtils.LOGGER.error(e.getMessage());
                        if (player != null && config.devMode) {
                            Messages.sendPlayerMessage(player, "reloadFailed", e.getMessage());
                        }
                    }

                    AdditionalDataTooltip.invalidateAll();
                }).start();
            }
        });
    }
}
