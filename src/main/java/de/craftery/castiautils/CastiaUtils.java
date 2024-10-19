package de.craftery.castiautils;

import de.craftery.castiautils.adblock.Adblocker;
import de.craftery.castiautils.chestshop.ItemShopTooltip;
import de.craftery.castiautils.chestshop.ShopCommand;
import de.craftery.castiautils.chestshop.ShopConfig;
import de.craftery.castiautils.chestshop.ShopLogger;
import de.craftery.castiautils.config.CastiaConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CastiaUtils implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("castiautils");

    @Override
    public void onInitialize() {
        LOGGER.info("Starting up Crafters Castia Utils (castiautils)");

        AutoConfig.register(CastiaConfig.class, JanksonConfigSerializer::new);

        ShopConfig.load();
        ShopConfig.register();
        Adblocker.register();
        ItemShopTooltip.register();
        ShopLogger.register();
        ShopCommand.register();
    }

    public static CastiaConfig getConfig() {
        return AutoConfig.getConfigHolder(CastiaConfig.class).getConfig();
    }
}
